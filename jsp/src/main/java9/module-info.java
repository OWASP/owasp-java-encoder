module owasp.encoder.jsp {
    requires transitive javax.servlet.jsp.api;
    requires owasp.encoder;

    exports org.owasp.encoder.tag;
}
