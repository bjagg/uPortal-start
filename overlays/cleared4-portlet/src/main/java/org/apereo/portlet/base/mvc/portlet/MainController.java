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

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.PooledConnection;
import javax.portlet.ActionResponse;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private static final String SQL = "select url from cleared_urls where userid = ?";
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private DataSource ds;

    @RenderMapping
    public ModelAndView showMainView(final RenderRequest request, final RenderResponse response) {

        @SuppressWarnings("unchecked")
        final Map<String, String> userInfo = (Map<String, String>) request.getAttribute(PortletRequest.USER_INFO);

        final ModelAndView mav = new ModelAndView("main");
        mav.addObject("user.login.id", userInfo.get("user.login.id"));
        return mav;

    }

    @ActionMapping
    public void doAction(final PortletRequest request, final ActionResponse response) throws IOException, SQLException {
        @SuppressWarnings("unchecked")
        final Map<String, String> userInfo = (Map<String, String>) request.getAttribute(PortletRequest.USER_INFO);
        final String userId = userInfo.get("user.login.id");
        log.debug("user.login.id = {}", userId);
        final String url = getCleared4DeepUrl(userId);
        log.debug("deep url = {}", url);
        if (url != null) {
            response.sendRedirect(url);
        }
    }

    private String getCleared4DeepUrl(final String userId) {
        PooledConnection conn;
        try {
            conn = ds.getPooledConnection();
        } catch (SQLException se) {
            log.error("Could not obtain datasource for integration database", se);
            return null;
        }
        JdbcTemplate template = new JdbcTemplate(ds);
        Object[] args = new Object[] {userId};
        List<Map<String, Object>> results = template.query(SQL, args, new ColumnMapRowMapper());
        log.debug("found {} for query {}", results, SQL);
        if (!results.isEmpty()) {
            return results.get(0).values().toArray(new String[0])[0];
        } else {
            log.warn("No url for {}", userId);
            return null;
        }
    }

}
