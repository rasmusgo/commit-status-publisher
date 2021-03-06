<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="util" uri="/WEB-INF/functions/util" %>
<jsp:useBean id="keys" class="jetbrains.buildServer.commitPublisher.Constants"/>
<c:if test="${testConnectionSupported}">
  <script>
    $j(document).ready(function() {
      PublisherFeature.showTestConnection();
    });
  </script>
</c:if>
<table style="width: 100%">
  <tr>
    <th><label for="${keys.bitbucketCloudUsername}">Bitbucket Username: <l:star/></label></th>
    <td>
      <props:textProperty name="${keys.bitbucketCloudUsername}" style="width:18em;"/>
      <span class="error" id="error_${keys.bitbucketCloudUsername}"></span>
    </td>
  </tr>

  <tr>
    <th><label for="${keys.bitbucketCloudPassword}">Bitbucket Password: <l:star/></label></th>
    <td>
      <props:passwordProperty name="${keys.bitbucketCloudPassword}" style="width:18em;"/>
      <span class="error" id="error_${keys.bitbucketCloudPassword}"></span>
    </td>
  </tr>
</table>

