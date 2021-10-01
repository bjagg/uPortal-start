<%@ page contentType="text/html" isELIgnored="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="portlet" uri="http://java.sun.com/portlet_2_0" %>

<c:set var="n"><portlet:namespace/></c:set>

<div id="${n}container">
<!-- ${user.login.id} -->
  <c:if test = "${not empty error}">
  <div class="alert alert-danger">${error}</div>
  </c:if>
  <div class="message">
    <p>CLEARED4 Cuesta is the system the College will be using for vaccination/testing verification. Please click the button below to access your account and complete your vaccination or testing status in order to return to Cuesta College campus.</p>
  </div>
  <div class="redirect">
    <portlet:actionURL var="requestRedirectUrl"/>
    <form method="post" action="${requestRedirectUrl}" role="form">
    <button type="submit" class="btn btn-primary">Visit CLEARED4</button>
    </form>
  </div>
</div>

