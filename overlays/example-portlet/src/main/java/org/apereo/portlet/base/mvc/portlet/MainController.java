/**
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apereo.portlet.base.mvc.portlet;

import java.util.Map;

import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.bind.annotation.ActionMapping;
import org.springframework.web.portlet.bind.annotation.RenderMapping;

/**
 * Main portlet view.
 */
@Controller
@RequestMapping("VIEW")
public class MainController {

    private static final String GREETING_MESSAGE_PREFERENCE = "MainController.greetingMessage";
    private static final String GREETING_MESSAGE_DEFAULT = "Good day";

    private final Log log = LogFactory.getLog(getClass());

    @RenderMapping
    public ModelAndView showMainView(final RenderRequest request, final RenderResponse response) {
        if (log.isDebugEnabled()) {
            log.debug("Processing main view");
        }
        final ModelAndView mav = new ModelAndView("main");
        PortletPreferences prefs = request.getPreferences();
        final String greetingMessage = prefs.getValue(GREETING_MESSAGE_PREFERENCE, GREETING_MESSAGE_DEFAULT);

        //Get the USER_INFO from portlet.xml,
        //which gets it from personDirectoryContext.xml
        @SuppressWarnings("unchecked")
        final Map<String, String> userInfo = (Map<String, String>) request.getAttribute(PortletRequest.USER_INFO);

        mav.addObject("username", request.getRemoteUser());
        mav.addObject("displayName", userInfo.get("displayName"));
        mav.addObject("emailAddress", userInfo.get("mail"));

        if (log.isDebugEnabled()) {
            log.debug("Rendering main view");
        }

        mav.addObject("greetingMessage", greetingMessage);
        return mav;

    }

    @ActionMapping
    public void doAction() {
        // no-op action mapping to prevent accidental calls to this URL from
        // crashing the portlet
    }

}
