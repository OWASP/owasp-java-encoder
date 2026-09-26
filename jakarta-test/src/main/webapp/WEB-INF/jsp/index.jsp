<%@page session="false" contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="jakarta.tags.core"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>OWASP Java Encoder Jakarta JSP Test</title>
    </head>
    <body>
        <h1>Hello World!</h1>
        You are likely looking for the test page located <a href="<c:url value="/item/viewItems"/>">here</a>.
    </body>
</html>
