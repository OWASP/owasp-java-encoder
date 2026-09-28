"""Fixtures for the effective POM SCM inheritance guard."""

import importlib.util
from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location(
    'check_effective_pom_scm', ROOT / 'scripts/check-effective-pom-scm.py')
guard = importlib.util.module_from_spec(spec)
spec.loader.exec_module(guard)

BASE_SCM = {
    'connection': 'scm:git:https://github.com/OWASP/owasp-java-encoder.git',
    'developerConnection': 'scm:git:git@github.com:OWASP/owasp-java-encoder.git',
    'url': 'https://github.com/OWASP/owasp-java-encoder',
}


class EffectiveScmFixtures(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.paths = {}
        for module, artifact in guard.MODULES.items():
            self.write_fixture(module, artifact, BASE_SCM)

    def write_fixture(self, module, artifact, scm):
        project = ET.Element(guard.qualified('project'))
        ET.SubElement(project, guard.qualified('artifactId')).text = artifact
        metadata = ET.SubElement(project, guard.qualified('scm'))
        for field, value in scm.items():
            ET.SubElement(metadata, guard.qualified(field)).text = value
        path = Path(self.temp.name) / (artifact + '.xml')
        ET.ElementTree(project).write(path, encoding='utf-8', xml_declaration=True)
        self.paths[module] = path

    def test_matching_effective_metadata(self):
        guard.check_effective_poms(self.paths)

    def test_each_changed_field_is_rejected(self):
        for field in guard.FIELDS:
            with self.subTest(field=field):
                altered = dict(BASE_SCM, **{field: BASE_SCM[field] + '-wrong'})
                self.write_fixture('core', 'encoder', altered)
                with self.assertRaisesRegex(ValueError, 'scm/' + field):
                    guard.check_effective_poms(self.paths)
                self.write_fixture('core', 'encoder', BASE_SCM)

    def test_each_module_appended_field_is_rejected(self):
        for module, artifact in list(guard.MODULES.items())[1:]:
            for field in guard.FIELDS:
                with self.subTest(module=module, field=field):
                    altered = dict(BASE_SCM, **{field: BASE_SCM[field] + '/' + module})
                    self.write_fixture(module, artifact, altered)
                    with self.assertRaisesRegex(ValueError, 'module-appended path'):
                        guard.check_effective_poms(self.paths)
                    self.write_fixture(module, artifact, BASE_SCM)

    def test_missing_field_and_wrong_artifact_are_rejected(self):
        for field in guard.FIELDS:
            with self.subTest(field=field):
                self.write_fixture('jsp', 'encoder-jsp',
                                   {key: value for key, value in BASE_SCM.items()
                                    if key != field})
                with self.assertRaisesRegex(ValueError, 'missing effective scm/' + field):
                    guard.check_effective_poms(self.paths)
                self.write_fixture('jsp', 'encoder-jsp', BASE_SCM)
        self.write_fixture('jsp', 'unrelated-artifact', BASE_SCM)
        with self.assertRaisesRegex(ValueError, 'expected artifactId encoder-jsp'):
            guard.check_effective_poms(self.paths)

    def test_parent_module_path_is_rejected(self):
        altered = dict(BASE_SCM, url=BASE_SCM['url'] + '/core')
        self.write_fixture('', 'encoder-parent', altered)
        with self.assertRaisesRegex(ValueError, 'module-appended path'):
            guard.check_effective_poms(self.paths)


if __name__ == '__main__':
    unittest.main()
