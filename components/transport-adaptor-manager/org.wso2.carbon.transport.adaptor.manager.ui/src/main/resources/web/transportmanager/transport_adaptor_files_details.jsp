<%@ page import="org.wso2.carbon.transport.adaptor.manager.stub.TransportManagerAdminServiceStub" %>
<%@ page import="org.wso2.carbon.transport.adaptor.manager.stub.types.TransportAdaptorFileDto" %>
<%@ page import="org.wso2.carbon.transport.adaptor.manager.ui.UIUtils" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>


<fmt:bundle basename="org.wso2.carbon.transport.adaptor.manager.ui.i18n.Resources">

    <carbon:breadcrumb
            label="transportmanager.details"
            resourceBundle="org.wso2.carbon.transport.adaptor.manager.ui.i18n.Resources"
            topPage="false"
            request="<%=request%>"/>

    <script type="text/javascript" src="../admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../admin/js/cookies.js"></script>
    <script type="text/javascript" src="../admin/js/main.js"></script>

    <script type="text/javascript">
        function doDelete(filePath) {
            var theform = document.getElementById('deleteForm');
            theform.filePath.value = filePath;
            theform.submit();
        }
    </script>
    <%
        String filePath = request.getParameter("filePath");
        if (filePath != null) {
            TransportManagerAdminServiceStub stub = UIUtils.getTransportManagerAdminService(config, session, request);
            stub.removeTransportAdaptorFile(filePath);
    %>
    <script type="text/javascript">CARBON.showInfoDialog('Transport File successfully deleted.');</script>
    <%
        }
    %>


    <div id="middle">
        <div id="workArea">
            <h3>Available Transport Adaptors</h3>
            <table class="styledLeft">

                <%
                    TransportManagerAdminServiceStub stub = UIUtils.getTransportManagerAdminService(config, session, request);
                    TransportAdaptorFileDto[] transportDetailsArray = stub.getUnDeployedFiles();
                    if (transportDetailsArray != null) {
                        for (TransportAdaptorFileDto transportAdaptorFile : transportDetailsArray) {

                %>
                <thead>
                <tr>
                    <th>File Path</th>
                    <th>Transport Adaptor Name</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td>
                        <a href="transport_details.jsp?filePath=<%=transportAdaptorFile.getFilePath()%>&transportName=<%=transportAdaptorFile.getTransportAdaptorName()%>"><%=transportAdaptorFile.getFilePath()%>
                        </a>

                    </td>
                    <td><%=transportAdaptorFile.getTransportAdaptorName()%>
                    </td>
                    <td>
                        <a style="background-image: url(../admin/images/delete.gif);"
                           class="icon-link"
                           onclick="doDelete('<%=transportAdaptorFile.getFilePath()%>')"><font
                                color="#4682b4">Delete</font></a>
                    </td>

                </tr>
                <%
                        }
                    }
                %>
                </tbody>
            </table>

            <div>
                <form id="deleteForm" name="input" action="" method="get"><input type="HIDDEN"
                                                                                 name="filePath"
                                                                                 value=""/></form>
            </div>
        </div>


        <script type="text/javascript">
            alternateTableRows('expiredsubscriptions', 'tableEvenRow', 'tableOddRow');
            alternateTableRows('validsubscriptions', 'tableEvenRow', 'tableOddRow');
        </script>
</fmt:bundle>