"""Negative tests for the CI release/version and aggregate-result boundaries."""
import importlib.util
from pathlib import Path
import shutil
import tempfile
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]


def load(name):
    spec = importlib.util.spec_from_file_location(name, ROOT / 'scripts' / (name + '.py'))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


version = load('check-ci-version')
gate = load('check-ci-gate')


class VersionPolicy(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.poms = ['pom.xml', 'core/pom.xml', 'jsp/pom.xml', 'jakarta/pom.xml',
                     'jakarta-test/pom.xml']
        for name in self.poms:
            target = self.root / name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / name, target)
        self.current = version.ET.parse(self.root / 'pom.xml').findtext('p:version', namespaces=version.NS)
        # Normalize even when these tests run on the intentional release commit.
        self.replace(self.current, '9.8.7-SNAPSHOT')
        self.replace('<tag>v9.8.7-SNAPSHOT</tag>', '<tag>HEAD</tag>')

    def replace(self, old, new, names=None):
        for name in names or self.poms:
            path = self.root / name
            path.write_text(path.read_text().replace(old, new))

    def test_snapshot(self):
        self.assertEqual('snapshot', version.check(self.root, snapshot_only=True))

    def test_retired_esapi_module_is_absent(self):
        root = version.ET.parse(ROOT / 'pom.xml')
        modules = [node.text for node in root.findall('p:modules/p:module', version.NS)]
        self.assertEqual(['core', 'jsp', 'jakarta'], modules)
        self.assertFalse((ROOT / 'esapi' / 'pom.xml').exists())

    def test_accidental_release_rejected(self):
        self.replace('9.8.7-SNAPSHOT', '9.8.7')
        with self.assertRaises(ValueError):
            version.check(self.root)

    def test_reviewed_release_and_next_snapshot(self):
        self.replace('9.8.7-SNAPSHOT', '9.8.7')
        self.replace('<tag>HEAD</tag>', '<tag>v9.8.7</tag>')
        self.assertEqual('release', version.check(self.root))
        self.assertEqual('release', version.check(self.root, ref='refs/tags/v9.8.7'))
        for kwargs in ({'snapshot_only': True}, {'ref': 'refs/tags/v9.8.6'}):
            with self.assertRaises(ValueError):
                version.check(self.root, **kwargs)
        self.replace('9.8.7', '9.8.8-SNAPSHOT')
        self.replace('<tag>v9.8.8-SNAPSHOT</tag>', '<tag>HEAD</tag>')
        self.assertEqual('snapshot', version.check(self.root))

    def test_mismatched_module_or_app(self):
        for name in self.poms[1:]:
            with self.subTest(name=name):
                self.replace('9.8.7-SNAPSHOT', '9.8.6-SNAPSHOT', [name])
                with self.assertRaises(ValueError):
                    version.check(self.root)
                self.replace('9.8.6-SNAPSHOT', '9.8.7-SNAPSHOT', [name])

    def test_snapshot_cannot_claim_release_tag(self):
        with self.assertRaises(ValueError):
            version.check(self.root, ref='refs/tags/v9.8.7')
        self.replace('<tag>HEAD</tag>', '<tag>v9.8.7</tag>')
        with self.assertRaises(ValueError):
            version.check(self.root)


class GatePolicy(unittest.TestCase):
    def test_success(self):
        gate.check({'build': {'result': 'success'}, 'matrix': {'result': 'success'}}, ['build', 'matrix'])

    def test_failure_cancel_skip_and_unknown_fail_closed(self):
        for result in ('failure', 'cancelled', 'skipped', 'neutral', '', None):
            with self.subTest(result=result), self.assertRaises(ValueError):
                gate.check({'build': {'result': 'success'}, 'matrix': {'result': result}}, ['build', 'matrix'])

    def test_missing_or_extra_job(self):
        for needs in ({}, {'build': {'result': 'success'}, 'other': {'result': 'success'}}):
            with self.assertRaises(ValueError):
                gate.check(needs, ['build'])


class DependencySubmissionPolicy(unittest.TestCase):
    def test_only_executed_plugins_are_submitted(self):
        workflow = (ROOT / '.github/workflows/dependency-submission.yaml').read_text()
        self.assertIn('-DincludeArtifactIds=', workflow)
        for required in ('exec-maven-plugin', 'maven-help-plugin',
                         'spring-boot-maven-plugin', 'central-publishing-maven-plugin'):
            self.assertIn(required, workflow)
        for inactive in ('maven-release-plugin', 'maven-site-plugin',
                         'flyway-maven-plugin', 'native-maven-plugin'):
            self.assertNotIn(inactive, workflow)

        app_line = next(line.strip() for line in workflow.splitlines()
                        if line.strip().startswith('app_plugins:'))
        for required in ('maven-clean-plugin', 'maven-checkstyle-plugin',
                         'maven-failsafe-plugin', 'maven-war-plugin',
                         'spring-boot-maven-plugin'):
            self.assertIn(required, app_line)
        for inactive in ('maven-source-plugin', 'maven-javadoc-plugin',
                         'maven-help-plugin', 'maven-install-plugin',
                         'build-helper-maven-plugin', 'maven-antrun-plugin'):
            self.assertNotIn(inactive, app_line)
        app_command = '-f jakarta-test/pom.xml'
        self.assertIn(app_command, workflow)
        self.assertLess(workflow.index('Resolve shared library build plugins'),
                        workflow.index(app_command))

    def test_consumer_fixture_downloader_uses_submitted_patched_realm(self):
        workflow = (ROOT / '.github/workflows/dependency-submission.yaml').read_text()
        fixture_command = '-f compatibility/dependencies/pom.xml'
        self.assertIn(fixture_command, workflow)
        self.assertLess(workflow.index(fixture_command),
                        workflow.index('Submit build graph'))

        parent = ET.parse(ROOT / 'compatibility/dependencies/pom.xml').getroot()
        plugins = parent.findall('p:build/p:pluginManagement/p:plugins/p:plugin',
                                 version.NS)
        downloader = next(plugin for plugin in plugins
                          if plugin.findtext('p:artifactId', namespaces=version.NS)
                          == 'maven-dependency-plugin')
        self.assertEqual('3.11.0', downloader.findtext('p:version', namespaces=version.NS))
        dependencies = downloader.findall('p:dependencies/p:dependency', version.NS)
        beanutils = next(dependency for dependency in dependencies
                         if dependency.findtext('p:artifactId', namespaces=version.NS)
                         == 'commons-beanutils')
        self.assertEqual('1.11.0', beanutils.findtext('p:version', namespaces=version.NS))

        root = ET.parse(ROOT / 'pom.xml').getroot()
        root_plugins = root.findall('p:build/p:pluginManagement/p:plugins/p:plugin',
                                    version.NS)
        root_downloader = next(plugin for plugin in root_plugins
                               if plugin.findtext('p:artifactId', namespaces=version.NS)
                               == 'maven-dependency-plugin')

        def dependency_coordinates(plugin):
            return sorted(tuple(dependency.findtext('p:' + field,
                                                     namespaces=version.NS)
                                for field in ('groupId', 'artifactId', 'version'))
                          for dependency in plugin.findall(
                              'p:dependencies/p:dependency', version.NS))

        self.assertEqual(root_downloader.findtext('p:version', namespaces=version.NS),
                         downloader.findtext('p:version', namespaces=version.NS))
        self.assertEqual(dependency_coordinates(root_downloader),
                         dependency_coordinates(downloader))

        expected_parent = ('org.owasp.encoder.tests', 'consumer-dependency-parent',
                           '1', 'pom.xml')
        for name in ('jsp.xml', 'jakarta.xml', 'osgi-r6.xml', 'osgi-r8.xml',
                     'legacy-core.xml'):
            child = ET.parse(ROOT / 'compatibility/dependencies' / name).getroot()
            actual_parent = tuple(child.findtext('p:parent/p:' + field,
                                                 namespaces=version.NS)
                                  for field in ('groupId', 'artifactId', 'version',
                                                'relativePath'))
            self.assertEqual(expected_parent, actual_parent, name)
            downloader_overrides = [
                plugin for plugin in child.findall('.//p:plugin', version.NS)
                if plugin.findtext('p:artifactId', namespaces=version.NS)
                == 'maven-dependency-plugin'
            ]
            self.assertEqual([], downloader_overrides, name)

        dependabot = (ROOT / '.github/dependabot.yml').read_text()
        self.assertIn('- /compatibility/dependencies', dependabot)


if __name__ == '__main__':
    unittest.main()
