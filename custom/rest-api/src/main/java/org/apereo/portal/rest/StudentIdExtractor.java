package org.apereo.portal.rest;

import lombok.extern.slf4j.Slf4j;
import org.apereo.portal.security.IPerson;
import org.apereo.portal.security.IPersonManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * Component that retrieves a users studentID from the {code HttpServletRequeset}.
 */
@Slf4j
@Component
public class StudentIdExtractor {

    public static final String STUDENT_ID = "studentId";

    @Autowired
    private IPersonManager personManager;

    /**
     * Retrieves studentId attribute value (if present) for remote user in the {code HttpServletRequest}.
     *
     * @param request parameter used to determine user via {code HttpServletRequest.getRemoteUser()}
     * @return {code String} of studentId for user if present, or null
     */
    String getStudentId(final HttpServletRequest request) {
        log.debug("Getting {} for user {}", STUDENT_ID, request.getRemoteUser());
        IPerson person = personManager.getPerson(request);
        log.debug("attributes for {}: {}", request.getRemoteUser(), person.getAttributeMap().toString());
        final String id = (String) person.getAttribute(STUDENT_ID);
        log.debug("{} for {} is {}", STUDENT_ID, request.getRemoteUser(), id);
        return id;
    }
}
