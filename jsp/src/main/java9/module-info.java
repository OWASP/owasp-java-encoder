module owasp.encoder.jsp {
    requires owasp.encoder;
    requires transitive javax.servlet.jsp.api;

    exports org.owasp.encoder.tag;
}
