#!/usr/bin/env python3
"""Compare every JSP/Jakarta source file, including descriptors and test fixtures.

Only the named platform differences below are translated. TLD XML formatting is
insignificant, but all declarations, attributes and description words are checked.
Existing short-name metadata is preserved: a caller's chosen JSP prefix does not
have to match short-name, so changing it is unnecessary for source parity.
"""

import difflib
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
JAVAX_XML = "http://java.sun.com/xml/ns/javaee"
JAKARTA_XML = "https://jakarta.ee/xml/ns/jakartaee"
TLD_URIS = {
    "main/resources/META-INF/java-encoder.tld": (
        "https://www.owasp.org/index.php/OWASP_Java_Encoder_Project", "owasp.encoder.jakarta"),
    "main/resources/META-INF/java-encoder-advanced.tld": (
        "https://www.owasp.org/index.php/OWASP_Java_Encoder_Project#advanced", "owasp.encoder.jakarta.advanced"),
}
CONSUMERS = "test/modules/owasp.encoder.jsp.consumer/"


def target_path(path):
    if path.startswith(CONSUMERS):
        return path.replace(CONSUMERS, "test/modules/owasp.encoder.jakarta.consumer/", 1).replace(
            "/JspConsumer.java", "/JakartaConsumer.java")
    return path


def descriptor(text, path, translate):
    root = ET.fromstring(text)
    if translate:
        for element in root.iter():
            element.tag = element.tag.replace("{" + JAVAX_XML + "}", "{" + JAKARTA_XML + "}")
        # The two modules target different JSP specification versions/schemas.
        if root.get("version") == "2.1":
            root.set("version", "3.0")
        schema = "{http://www.w3.org/2001/XMLSchema-instance}schemaLocation"
        if root.get(schema) == JAVAX_XML + " " + JAVAX_XML + "/web-jsptaglibrary_2_1.xsd":
            root.set(schema, JAKARTA_XML + " " + JAKARTA_XML + "/web-jsptaglibrary_3_0.xsd")
        uri = root.find("{" + JAKARTA_XML + "}uri")
        before, after = TLD_URIS[path]
        if uri is not None and uri.text == before:
            uri.text = after
        # This metadata difference has shipped since Jakarta support was added.
        if path == "main/resources/META-INF/java-encoder.tld":
            short_name = root.find("{" + JAKARTA_XML + "}short-name")
            if short_name is not None and short_name.text == "java-encoder":
                short_name.text = "e"

    def canonical(element):
        return (element.tag, sorted(element.attrib.items()), " ".join((element.text or "").split()),
                tuple((canonical(child), " ".join((child.tail or "").split())) for child in element))

    return repr(canonical(root))


def translated(text, path):
    if path.endswith(".java"):
        # XML/JAXP still uses javax.xml on Jakarta; do not rewrite all javax APIs.
        text = re.sub(r"(?m)^import javax\.(servlet|el)\.", r"import jakarta.\1.", text)
    if path == "main/java9/module-info.java":
        text = text.replace("module owasp.encoder.jsp {", "module owasp.encoder.jakarta {")
        text = text.replace("requires transitive javax.servlet.jsp.api;", "requires transitive jakarta.servlet.jsp;")
    elif path.startswith(CONSUMERS):
        text = text.replace("owasp.encoder.jsp", "owasp.encoder.jakarta")
        text = text.replace("JspConsumer", "JakartaConsumer")
        text = text.replace("exercises the JSP tag API", "exercises the Jakarta JSP tag API")
    elif path == "site/markdown/index.md":
        text = text.replace("OWASP JSP", "OWASP Jakarta JSP")
        text = text.replace("<artifactId>encoder-jsp</artifactId>", "<artifactId>encoder-jakarta-jsp</artifactId>")
        text = text.replace('uri="https://www.owasp.org/index.php/OWASP_Java_Encoder_Project"',
                            'uri="owasp.encoder.jakarta"')
    return text


def main():
    jsp = ROOT / "jsp/src"
    jakarta = ROOT / "jakarta/src"
    source_paths = sorted(p.relative_to(jsp).as_posix() for p in jsp.rglob("*") if p.is_file())
    actual_paths = {p.relative_to(jakarta).as_posix() for p in jakarta.rglob("*") if p.is_file()}
    expected_paths = {target_path(path) for path in source_paths}
    errors = []
    for path in sorted(expected_paths - actual_paths):
        errors.append("Missing Jakarta file: " + path)
    for path in sorted(actual_paths - expected_paths):
        errors.append("Unexpected Jakarta file: " + path)
    for path in source_paths:
        target = target_path(path)
        if target not in actual_paths:
            continue
        expected = (jsp / path).read_text(encoding="utf-8")
        actual = (jakarta / target).read_text(encoding="utf-8")
        if path in TLD_URIS:
            expected = descriptor(expected, path, True)
            actual = descriptor(actual, path, False)
        else:
            expected = translated(expected, path)
        if expected != actual:
            errors.append("Drift: jsp/src/" + path + " -> jakarta/src/" + target)
            if path not in TLD_URIS:
                errors.extend(difflib.unified_diff(expected.splitlines(), actual.splitlines(),
                              fromfile="expected Jakarta", tofile="actual Jakarta", lineterm=""))
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print("JSP/Jakarta parity: {} source files checked".format(len(source_paths)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
