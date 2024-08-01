<%@page contentType="text/html;charset=UTF-8" language="java"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<%@taglib prefix="e" uri="owasp.encoder.jakarta"%>
<html>
    <head>
        <title>View Items</title>
        <link href="<c:url value="/css/common.css"/>" rel="stylesheet" type="text/css">
    </head>
    <body>
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Description</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach items="${items}" var="item">
                    <tr>
                        <td id="a${item.id}">${item.id}</td>
                        <td id="b${item.id}"><e:forHtml  value="${item.name}"/></td>
                        <td id="c${item.id}">${e:forHtml(item.description)}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </body>
</html>