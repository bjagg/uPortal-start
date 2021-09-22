package org.apereo.portal.rest;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Slf4j
@Controller
public class CourseScheduleController {

    private static final String SQL = "select * from StudentSections where StudentID = ?";
    @Autowired
    StudentIdExtractor studentIdExtractor;
    @Autowired
    SimpleSqlIntegrationDao integrationDao;
    @Value("${course.schedule.helpUrl:http://helpme.edu}")
    private String helpUrl;

    static String getStringOrElse(Map<String, Object> rec, String field, String orElse) {
        return Optional.ofNullable((String) rec.get(field)).orElse(orElse);
    }

    @RequestMapping(value = "/courses", method = RequestMethod.GET)
    public ModelAndView getCourses(HttpServletRequest request) {
        String studentId = studentIdExtractor.getStudentId(request);
        Map<String, Object> data = new HashMap<>();
        data.put("waitlisted", 0);
        data.put("helpUrl", helpUrl);
        if (studentId != null) {
            List<Map<String, Object>> recs = integrationDao.getRecords(SQL, studentId);
            if (recs != null) {
                List<Section> sections = recs.stream().map(Section::new).collect(Collectors.toList());
                log.debug("{} has {} section records", studentId, sections.size());
                long waitlists = countWaitLists(sections);
                log.debug("{} has {} waitlists", studentId, waitlists);
                data.put("waitlisted", waitlists);
                List<Course> courses = structureCourses(sections);
                log.debug("{} has {} courses", studentId, courses.size());
                data.put("courses", courses);
            }
        }
        return new ModelAndView("jsonView", data);
    }

    private long countWaitLists(List<Section> sections) {
        return sections.stream()
                .filter(c -> c.waitingList)
                .count();
    }

    List<Course> structureCourses(List<Section> sections) {
        Map<Course, List<Section>> groups = sections.stream()
                .collect(groupingBy(Course::new));
        Comparator<Course> compareCourses = Comparator
                .comparing(Course::getHeading)
                .thenComparing(Course::getTitle);
        Set<Course> courses = groups.keySet();
        return courses.stream()
                .peek(c -> {
                    c.setSections(groups.get(c));
                })
                .sorted(compareCourses)
                .collect(Collectors.toList());
    }

    Map<String, Map<Course, List<Section>>> structureCoursesByCollege(List<Section> sections) {
        return sections.stream()
                .collect(groupingBy(Section::getCollege,
                        groupingBy(Course::new)));
    }

    @lombok.Value
    public static class Section {
        boolean waitingList;
        String college;
        String title;
        Number credit;
        String dates;
        String type;
        String day;
        String time;
        String room;
        String instructor;

        public Section(Map<String, Object> rec) {
            final String waitYN = (String) rec.get("WAITLIST");
            this.waitingList = "Y".equalsIgnoreCase(waitYN);
            this.college = (String) rec.get("COLLEGE");
            final String section = getStringOrElse(rec, "SECTIONNAME", "No Section");
            final String title = getStringOrElse(rec, "SHORTTITLE", "No Title");
            this.title = section + " " + title;
            this.credit = (Number) rec.get("CREDIT");
            dates = get2Fields(rec, "STARTDATE", "ENDDATE", " to ", "No dates specified");
            time = get2Fields(rec, "STARTTIME", "ENDTIME", "-", "ARR");
            this.type = getStringOrElse(rec, "CSM_INSTR_METHOD", "UNK");
            this.day = getStringOrElse(rec, "WEEKDAYS", "--");
            this.room = getRoom(rec);
            this.instructor = getStringOrElse(rec, "INSTRUCTOR", "--");
            log.debug("{}", this.toString());
        }

        private String get2Fields(Map<String, Object> rec, String field1, String field2, String separator, String missing) {
            String fields;
            final String first = rec.get(field1) == null ? "" : (String) rec.get(field1);
            final String second = rec.get(field2) == null ? "" : (String) rec.get(field2);
            if (!first.isEmpty() && !second.isEmpty()) {
                fields = first + separator + second;
            } else if (!first.isEmpty()) {
                fields = first;
            } else if (!second.isEmpty()) {
                fields = second;
            } else {
                fields = missing;
            }
            return fields;
        }

        private String getRoom(Map<String, Object> rec) {
            final String bldg = getStringOrElse(rec, "BUILDING", "");
            final String room = (bldg + " " + getStringOrElse(rec, "ROOM", "")).trim();
            return room.isEmpty() ? "--" : room;
        }
    }

    @EqualsAndHashCode
    public static class Course {
        @Getter
        final String heading;
        @Getter
        final String title;
        @Getter
        final Number credit;
        @Getter
        final String dates;
        @Getter
        @Setter
        private List<Section> sections;

        public Course(Section s) {
            heading = s.college;
            title = s.title;
            credit = s.credit;
            dates = s.dates;
            sections = Collections.emptyList();
        }

        public boolean getWaitlist() {
            return sections.stream().anyMatch(Section::isWaitingList);
        }
    }
}
