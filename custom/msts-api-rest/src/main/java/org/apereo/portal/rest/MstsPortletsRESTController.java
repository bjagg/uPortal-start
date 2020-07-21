/**
 * Licensed to Apereo under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership. Apereo
 * licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at the
 * following location:
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apereo.portal.rest;

import org.apache.commons.lang.StringUtils;
import org.apache.pluto.container.om.portlet.impl.PreferenceType;
import org.apereo.portal.EntityIdentifier;
import org.apereo.portal.IUserIdentityStore;
import org.apereo.portal.io.xml.portlet.ExternalPortletDefinition;
import org.apereo.portal.io.xml.portlet.PortletDefinitionImporterExporter;
import org.apereo.portal.io.xml.portlet.PortletPortalDataType;
import org.apereo.portal.portlet.dao.IPortletDefinitionDao;
import org.apereo.portal.portlet.dao.jpa.PortletPreferenceImpl;
import org.apereo.portal.portlet.om.IPortletDefinition;
import org.apereo.portal.portlet.om.IPortletPreference;
import org.apereo.portal.portlet.om.PortletCategory;
import org.apereo.portal.portlet.registry.IPortletCategoryRegistry;
import org.apereo.portal.portlet.registry.IPortletDefinitionRegistry;
import org.apereo.portal.portlet.registry.IPortletWindowRegistry;
import org.apereo.portal.portlet.rendering.IPortletExecutionManager;
import org.apereo.portal.security.IAuthorizationPrincipal;
import org.apereo.portal.security.IPerson;
import org.apereo.portal.security.IPersonManager;
import org.apereo.portal.services.AuthorizationServiceFacade;
import org.apereo.portal.url.PortalHttpServletFactoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Custom JSON portlet API endpoints.
 *
 * @since 5.8
 */
@Controller
public class MstsPortletsRESTController {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private IPortletDefinitionRegistry portletDefinitionRegistry;
    @Autowired
    private IPortletCategoryRegistry portletCategoryRegistry;
    @Autowired
    private IPersonManager personManager;
    @Autowired
    private PortalHttpServletFactoryService servletFactoryService;
    @Autowired
    private IPortletWindowRegistry portletWindowRegistry;
    @Autowired
    private IPortletExecutionManager portletExecutionManager;
    @Autowired
    private PortletPortalDataType typeRegistry;
    @Autowired
    private IPortletCategoryRegistry categoryRegistry;
    @Autowired
    private IPortletDefinitionDao definitionDao;
    @Autowired
    private IUserIdentityStore identityStore;
    private PortletDefinitionImporterExporter exporter;

    @PostConstruct
    private void postConstruct() {
        exporter = new PortletDefinitionImporterExporter();
        exporter.setPortletDefinitionRegistry(definitionDao);
        exporter.setUserIdentityStore(identityStore);
        exporter.setPortletCategoryRegistry(categoryRegistry);
        exporter.setPortletPortalDataType(typeRegistry);
    }

    /**
     * Provides information about all portlets in the portlet registry. NOTE: The response is
     * governed by the <code>IPermission.PORTLET_MANAGER_xyz</code> series of permissions. The
     * actual level of permission required is based on the current lifecycle state of the portlet.
     */
    @RequestMapping(value = "/v5-8/portlets.json", method = RequestMethod.GET)
    public ModelAndView getManageablePortlets(
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        // get a list of all channels
        List<IPortletDefinition> allPortlets = portletDefinitionRegistry.getAllPortletDefinitions();
        IAuthorizationPrincipal ap = getAuthorizationPrincipal(request);

        /*
         * Can customize PortletTuple class and use it below to change what values are sent through the JSON response.
         */
        List<ExternalPortletDefinition> portlets = new ArrayList<>();
        for (IPortletDefinition pdef : allPortlets) {
            if (ap.canManage(pdef.getPortletDefinitionId().getStringId())) {
                final ExternalPortletDefinition epDef = getExternalPortletDef(pdef);
                portlets.add(epDef);
            }
        }
        return new ModelAndView("json", "portlets", portlets);
    }

    /**
     * Provides information about a single portlet in the registry. NOTE: Access to this API enpoint
     * requires only <code>IPermission.PORTAL_SUBSCRIBE</code> permission.
     */
    @RequestMapping(value = "/v5-8/portlet/{fname}.json", method = RequestMethod.GET)
    public ModelAndView getPortlet(
            HttpServletRequest request, HttpServletResponse response, @PathVariable String fname)
            throws Exception {
        IAuthorizationPrincipal ap = getAuthorizationPrincipal(request);
        IPortletDefinition portletDef =
                portletDefinitionRegistry.getPortletDefinitionByFname(fname);
        if (portletDef != null && ap.canRender(portletDef.getPortletDefinitionId().getStringId())) {
            // can use PortletTuple class to limit fields sent
            final ExternalPortletDefinition epDef = getExternalPortletDef(portletDef);
            return new ModelAndView("json", "portlet", epDef);
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return new ModelAndView("json");
        }
    }

    /**
     * Adds or updates a specific portlet preference for a portlet.
     */
    @PostMapping("/v5-8/portlet/{fname}/preference")
    public ModelAndView updatePortletPreference(
            HttpServletRequest request, HttpServletResponse response, @PathVariable String fname,
            @RequestBody PreferenceType preference) {
        logger.debug("POSTed preference for {} is: {}", fname, preference);

        IAuthorizationPrincipal ap = getAuthorizationPrincipal(request);

        IPortletDefinition portletDef = portletDefinitionRegistry.getPortletDefinitionByFname(fname);
        if (portletDef == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return new ModelAndView("json");
        }

        // Should this be ap.canManage() ?
        if (!ap.canRender(portletDef.getPortletDefinitionId().getStringId())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return new ModelAndView("json");
        }

        final List<IPortletPreference> preferences = portletDef.getPortletPreferences();
        Optional<IPortletPreference> existingPreference = preferences.stream()
                .filter(p -> p.getName().equals(preference.getName()))
                .findAny();

        if (existingPreference.isPresent()) {
            logger.debug("Updating existing preference {} to {}", preference.getName(), fname);
            IPortletPreference pref = existingPreference.get();
            pref.setValues(preference.getValues().toArray(new String[0]));
            preference.setReadOnly(pref.isReadOnly());
            // object is still in the preferences list
        } else {
            logger.debug("Adding preference {} to {}", preference.getName(), fname);
            preferences.add(new PortletPreferenceImpl(preference));
        }

        portletDefinitionRegistry.savePortletDefinition(portletDef);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);
        return new ModelAndView("json", "preference", preference);

    }

    private IAuthorizationPrincipal getAuthorizationPrincipal(HttpServletRequest req) {
        IPerson user = personManager.getPerson(req);
        EntityIdentifier ei = user.getEntityIdentifier();
        IAuthorizationPrincipal rslt =
                AuthorizationServiceFacade.instance().newPrincipal(ei.getKey(), ei.getType());
        return rslt;
    }

    private Set<String> getPortletCategories(IPortletDefinition pdef) {
        Set<PortletCategory> categories = portletCategoryRegistry.getParentCategories(pdef);
        Set<String> rslt = new HashSet<String>();
        for (PortletCategory category : categories) {
            rslt.add(StringUtils.capitalize(category.getName().toLowerCase()));
        }
        return rslt;
    }

    private ExternalPortletDefinition getExternalPortletDef(IPortletDefinition pdef) {
        return Optional.ofNullable(pdef)
                .map(IPortletDefinition::getFName)
                .map(n -> exporter.exportData(n))
                .orElse(null);
    }

    /*
     * Nested Types
     */

    @SuppressWarnings("unused")
    private /* non-static */ final class PortletTuple implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String id;
        private final String name;
        private final String fname;
        private final String description;
        private final String type;
        private final String lifecycleState;
        private final Set<String> categories;

        public PortletTuple(IPortletDefinition pdef) {
            this.id = pdef.getPortletDefinitionId().getStringId();
            this.name = pdef.getName();
            this.fname = pdef.getFName();
            this.description = pdef.getDescription();
            this.type = pdef.getType().getName();
            this.lifecycleState = pdef.getLifecycleState().toString();
            this.categories = getPortletCategories(pdef);
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getFname() {
            return fname;
        }

        public String getDescription() {
            return description;
        }

        public String getType() {
            return type;
        }

        public String getLifecycleState() {
            return lifecycleState;
        }

        public Set<String> getCategories() {
            return categories;
        }
    }
}
