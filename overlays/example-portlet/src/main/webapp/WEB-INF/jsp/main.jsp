<%--

    Licensed to Apereo under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Apereo licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License.  You may obtain a
    copy of the License at the following location:

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

--%>
<!-- 
<jsp:directive.include file="/WEB-INF/jsp/include.jsp"/>
-->
<!--<h2>Hello ${ fn:escapeXml(displayName) }!</h2>-->
<!-- <h2><c:out value="${greetingMessage}" /> ${ fn:escapeXml(displayName) }!</h2>
<p>Your email address is ${ fn:escapeXml(emailAddress) }</p>
-->

<jsp:directive.include file="/WEB-INF/jsp/include.jsp"/>

<script src="/ResourceServingWebapp/rs/jquery/1.12.4/jquery-1.12.4.min.js" type="text/javascript"></script>
<c:set var="n"><portlet:namespace/></c:set>

<div id="${n}container">
    <h2>Click the link below to be greeted by name</h2>
    <div class="greetings"></div>
    <p><a class="greetLink" href="javascript:;">Greet Me!</a></p>
    <portlet:renderURL var="editUrl" portletMode="edit"/>
    <p><a href="${editUrl}">Edit Your Preferences</a></p>
</div>

<script type="text/javascript">
    var ${n} = {};
    ${n}.jQuery = jQuery.noConflict(true);

    ${n}.jQuery(function() {
    	var $ = ${n}.jQuery,
                   greeting = '<p><c:out value="${greetingMessage}" /> ${fn:escapeXml(displayName)}!</p>';

    	$('#${n}container .greetLink').click(function() {
                   $('#${n}container .greetings').append(greeting);
               });
    });
</script>
