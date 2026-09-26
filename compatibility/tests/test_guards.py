"""Negative tests against copies of real packaged artifacts (run after prepare)."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('consumers', ROOT / 'compatibility/consumers.py')
consumers = importlib.util.module_from_spec(spec)
spec.loader.exec_module(consumers)


class ArtifactGuards(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.jars = {}
        for kind, (artifact, *_) in consumers.ARTIFACTS.items():
            matches = list((ROOT / kind / 'target').glob(artifact + '-*.jar'))
            cls.jars[kind] = next(p for p in matches if not p.name.endswith(('-sources.jar', '-javadoc.jar')))

    def rejected(self, kind, transform):
        original = self.jars[kind]
        with tempfile.TemporaryDirectory(prefix='encoder-artifact-guard-') as directory:
            target = Path(directory) / original.name
            with zipfile.ZipFile(original) as source:
                entries = {name: source.read(name) for name in source.namelist()}
            transform(entries)
            with zipfile.ZipFile(target, 'w') as archive:
                for name, data in entries.items():
                    archive.writestr(name, data)
            with self.assertRaises(AssertionError):
                consumers.metadata(kind, target, self.jars['core'])

    def test_multi_release_flag(self):
        def mutate(entries):
            name = 'META-INF/MANIFEST.MF'
            entries[name] = entries[name].replace(b'Multi-Release: true', b'Multi-Release: false')
        self.rejected('core', mutate)

    def test_osgi_execution_requirement(self):
        def mutate(entries):
            name = 'META-INF/MANIFEST.MF'
            entries[name] = entries[name].replace(b'Manifest-Version: 1.0', b'Manifest-Version: 1.0\r\nRequire-Capability: osgi.ee')
        self.rejected('core', mutate)

    def test_missing_descriptor(self):
        self.rejected('core', lambda entries: entries.pop('META-INF/versions/9/module-info.class'))

    def test_java9_base_class(self):
        def mutate(entries):
            name = 'org/owasp/encoder/Encode.class'
            entries[name] = entries[name][:6] + b'\x00\x35' + entries[name][8:]
        self.rejected('core', mutate)

    def test_bundled_dependency_class(self):
        def mutate(entries):
            entries['org/junit/Test.class'] = entries['org/owasp/encoder/Encode.class']
        self.rejected('core', mutate)

    def test_missing_tld_tag(self):
        self.rejected('jsp', lambda entries: entries.pop('org/owasp/encoder/tag/ForHtmlTag.class'))

    def test_changed_tld_identity(self):
        def mutate(entries):
            name = 'META-INF/java-encoder.tld'
            entries[name] = entries[name].replace(b'owasp.encoder.jakarta', b'wrong.taglib.uri')
        self.rejected('jakarta', mutate)

    def test_runtime_test_dependency(self):
        def mutate(entries):
            name = 'META-INF/maven/org.owasp.encoder/encoder/pom.xml'
            pom = ET.fromstring(entries[name])
            ns = '{http://maven.apache.org/POM/4.0.0}'
            dependency = ET.SubElement(pom.find(ns + 'dependencies'), ns + 'dependency')
            for key, value in [('groupId', 'junit'), ('artifactId', 'junit'), ('version', '4.13.2')]:
                ET.SubElement(dependency, ns + key).text = value
            entries[name] = ET.tostring(pom)
        self.rejected('core', mutate)


if __name__ == '__main__':
    unittest.main()
