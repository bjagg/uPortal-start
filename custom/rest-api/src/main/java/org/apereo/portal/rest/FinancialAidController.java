package org.apereo.portal.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class FinancialAidController {

    private static final String SQL = "select * from StudentFAAward where StudentID = ?";

    @Value("${financial.aid.paymentUrl:https://studentaid.edu/payment}")
    private String paymentUrl;

    @Value("${financial.aid.applyUrl:https://studentaid.edu/sa/fafsa}")
    private String applyUrl;

    @Value("${financial.aid.viewDetailsUrl:https://studentaid.edu/details}")
    private String viewDetailsUrl;

    @Autowired
    StudentIdExtractor studentIdExtractor;

    @Autowired
    SimpleSqlIntegrationDao integrationDao;

    @RequestMapping(value="/finAid", method= RequestMethod.GET)
    public ModelAndView getCourses(HttpServletRequest request) {
        String studentId = studentIdExtractor.getStudentId(request);
        Map<String, Object> data = new HashMap<>();
        data.put("payments", new HashMap<>());
        data.put("financialAid", new HashMap<>());
        if (studentId != null) {
            List<Map<String, Object>> recs = integrationDao.getRecords(SQL, studentId);
            if (recs != null) {
                log.debug("{} has {} section records", recs.size());
                List<Map<String, Object>> paymentsRecs = filterRecords(recs, isPayment());
                log.debug("{} has {} payment records", studentId, paymentsRecs.size());
                List<Map<String, Object>> finaidRecs = filterRecords(recs, isFinAid());
                log.debug("{} has {} finaid records", studentId, finaidRecs.size());
                data = buildData(paymentsRecs, finaidRecs);
            }
        }
        return new ModelAndView("jsonView", data);
    }

    private Map<String, Object> buildData(List<Map<String, Object>> paymentsRecs, List<Map<String, Object>> finaidRecs) {
        Map<String, Object> payments = createPaymentsSection(paymentsRecs);
        Map<String, Object> finAid = createFinaidSection(finaidRecs);
        Map<String, Object> data = new HashMap<>();
        data.put("payments", payments);
        data.put("financialAid", finAid);
        return data;
    }

    private Map<String, Object> createPaymentsSection(List<Map<String, Object>> paymentsRecs) {
        Map<String, Object> payments = new HashMap<>();
        if (paymentsRecs.isEmpty()) {
            return payments;
        }
        payments.put("paymentUrl", paymentUrl);
        payments.put("payments", paymentsRecs);
        payments.put("warning", makeWarningMap(
                "You are in danger of being dropped from your classes unless payment is made by the date and time listed above.",
                paymentUrl));
        return payments;
    }

    private Map<String, Object> createFinaidSection(List<Map<String, Object>> finaidRecs) {
        Map<String, Object> finAid = new HashMap<>();
        finAid.put("viewDetailsUrl", viewDetailsUrl);
        if (finaidRecs.isEmpty()) {
            finAid.put("applyUrl", applyUrl);
            return finAid;
        }
        finAid.put("accounts", finaidRecs);
        finAid.put("warning", makeWarningMap("FAFSA Applications for the fall semester are due on November 30th.", applyUrl));
        return finAid;
    }

    public static List<Map<String, Object>> filterRecords(List<Map<String, Object>> recs, Predicate<Map<String, Object>> predicate) {
        return recs.stream()
                .filter(predicate)
                .map(refineRecord())
                .collect(Collectors.toList());
    }

    // Was going to use this on the records before calling filterRecords, but the time to create Optional objects is small
    /*
    public static Function<Map<String, Object>, Map<String, Object>> addOptionals() {
        return m -> {
            Map<String, Object> newrec = new HashMap<>(m);
            Optional<String> action = Optional.ofNullable((String) m.get("SA_ACTION"));
            newrec.put("SA_ACTION", action);
            Optional<Number> amount = Optional.ofNullable((Number) m.get("AMOUNT"));
            newrec.put("AMOUNT", amount);
            Optional<Number> txAmount = Optional.ofNullable((Number) m.get("TRANSMITTEDAMOUNT"));
            newrec.put("TRANSMITTEDAMOUNT", txAmount);
            return newrec;
        };
    }
     */

    public static Predicate<Map<String, Object>> isPayment() {
       return m -> {
           String year = (String) m.get("YEAR");
           Optional<String> action = Optional.ofNullable((String) m.get("SA_ACTION"));
           Optional<Number> amount = Optional.ofNullable((Number) m.get("AMOUNT"));
           Optional<Number> txAmount = Optional.ofNullable((Number) m.get("TRANSMITTEDAMOUNT"));
           return year == null
                   && action.map(a -> a.equalsIgnoreCase("B")).orElse(false)
                   && (amount.map(a -> a.doubleValue() > 0.0).orElse(false)
                   || txAmount.map(a -> a.doubleValue() > 0.0).orElse(false));
       };
    }

    public static Predicate<Map<String, Object>> isFinAid() {
        return m -> {
            String year = (String) m.get("YEAR");
            Optional<String> action = Optional.ofNullable((String) m.get("SA_ACTION"));
            Optional<Number> amount = Optional.ofNullable((Number) m.get("AMOUNT"));
            Optional<Number> txAmount = Optional.ofNullable((Number) m.get("TRANSMITTEDAMOUNT"));
            return year != null
                    && action.map(a -> a.equalsIgnoreCase("A")).orElse(false)
                    && (amount.map(a -> a.doubleValue() > 0.0).orElse(false)
                    || txAmount.map(a -> a.doubleValue() > 0.0).orElse(false));
        };
    }

    public static Function<Map<String, Object>, Map<String, Object>> refineRecord() {
        return m -> {
            Map<String, Object> newrec = new HashMap<>();
            newrec.put("name", Optional.ofNullable((String) m.get("DESCRIPTION")).orElse("NO DESC"));
            Optional<Number> amount = Optional.ofNullable((Number) m.get("AMOUNT"));
            Optional<Number> txAmount = Optional.ofNullable((Number) m.get("TRANSMITTEDAMOUNT"));
            newrec.put("amount", txAmount.filter(t -> t.doubleValue() > 0.0).orElse(amount.orElse(0.0)));
            return newrec;
        };
    }

    public static Map<String, String> makeWarningMap(String warning, String url) {
        Map<String, String> map = new HashMap<>();
        map.put("message", warning);
        map.put("url", url);
        map.put("image", "/uPortal/media/images/information-icon.svg");
        return map;
    }
}
