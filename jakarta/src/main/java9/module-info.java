module owasp.encoder.jakarta {
    requires transitive jakarta.servlet.jsp;
    requires owasp.encoder;

    exports org.owasp.encoder.tag;
}
