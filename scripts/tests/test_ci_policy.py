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
    def test_dependabot_scans_reactor_once(self):
        dependabot = (ROOT / '.github/dependabot.yml').read_text()
        maven = dependabot.split('- package-ecosystem: maven', 1)[1]
        maven = maven.split('- package-ecosystem:', 1)[0]
        directories = [line.split('- ', 1)[1].strip()
                       for line in maven.splitlines()
                       if line.lstrip().startswith('- /')]

        self.assertEqual(len(directories), len(set(directories)))
        self.assertIn('/', directories)
        self.assertIn('/compatibility/dependencies', directories)

        root = ET.parse(ROOT / 'pom.xml').getroot()
        reactor_modules = {
            '/' + node.text.strip()
            for node in root.findall('.//p:modules/p:module', version.NS)
        }
        self.assertTrue(reactor_modules)
        self.assertEqual(set(), reactor_modules.intersection(directories))

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

    def test_api_updates_keep_explicit_compatibility_baselines(self):
        def properties(project):
            return project.find('p:properties', version.NS)

        def dependencies(parent):
            return {
                (dependency.findtext('p:groupId', namespaces=version.NS),
                 dependency.findtext('p:artifactId', namespaces=version.NS)):
                dependency
                for dependency in parent.findall('p:dependencies/p:dependency',
                                                   version.NS)
            }

        def dependency_version(dependency):
            return dependency.findtext('p:version', namespaces=version.NS)

        def dependency_scope(dependency):
            return dependency.findtext('p:scope', namespaces=version.NS)

        def exec_dependencies(project):
            plugins = project.findall('p:build/p:plugins/p:plugin', version.NS)
            plugin = next(item for item in plugins
                          if item.findtext('p:artifactId', namespaces=version.NS)
                          == 'exec-maven-plugin')
            return dependencies(plugin)

        jsp = ET.parse(ROOT / 'jsp/pom.xml').getroot()
        jsp_properties = properties(jsp)
        self.assertEqual('2.2.1', jsp_properties.findtext(
            'p:jsp.api.baseline.version', namespaces=version.NS))
        self.assertEqual('2.3.3', jsp_properties.findtext(
            'p:jsp.api.version', namespaces=version.NS))
        jsp_dependencies = dependencies(jsp)
        jsp_api = jsp_dependencies[('javax.servlet.jsp', 'javax.servlet.jsp-api')]
        self.assertEqual('${jsp.api.version}', dependency_version(jsp_api))
        self.assertEqual('provided', dependency_scope(jsp_api))
        self.assertEqual('${jsp.api.baseline.version}', dependency_version(
            exec_dependencies(jsp)[('javax.servlet.jsp', 'javax.servlet.jsp-api')]))
        self.assertIn('${jsp.api.baseline.version}', jsp_properties.findtext(
            'p:japicmp.old.classpath', namespaces=version.NS))
        self.assertIn('${jsp.api.version}', jsp_properties.findtext(
            'p:japicmp.new.classpath', namespaces=version.NS))
        self.assertNotIn('${jsp.api.version}', jsp_properties.findtext(
            'p:japicmp.old.classpath', namespaces=version.NS))
        self.assertNotIn('${jsp.api.baseline.version}', jsp_properties.findtext(
            'p:japicmp.new.classpath', namespaces=version.NS))

        jakarta = ET.parse(ROOT / 'jakarta/pom.xml').getroot()
        jakarta_properties = properties(jakarta)
        expected = {
            'jakarta.el.api.baseline.version': '4.0.0',
            'jakarta.el.api.version': '6.0.1',
            'jakarta.servlet.api.baseline.version': '6.0.0',
            'jakarta.servlet.api.version': '6.1.0',
        }
        for name, value in expected.items():
            self.assertEqual(value, jakarta_properties.findtext(
                'p:' + name, namespaces=version.NS))

        jakarta_dependencies = dependencies(jakarta)
        jakarta_exec_dependencies = exec_dependencies(jakarta)
        for group, artifact, current, baseline in (
                ('jakarta.el', 'jakarta.el-api',
                 '${jakarta.el.api.version}',
                 '${jakarta.el.api.baseline.version}'),
                ('jakarta.servlet', 'jakarta.servlet-api',
                 '${jakarta.servlet.api.version}',
                 '${jakarta.servlet.api.baseline.version}')):
            coordinate = (group, artifact)
            self.assertEqual(current, dependency_version(
                jakarta_dependencies[coordinate]))
            self.assertEqual('test', dependency_scope(
                jakarta_dependencies[coordinate]))
            self.assertEqual(baseline, dependency_version(
                jakarta_exec_dependencies[coordinate]))

        old_classpath = jakarta_properties.findtext(
            'p:japicmp.old.classpath', namespaces=version.NS)
        new_classpath = jakarta_properties.findtext(
            'p:japicmp.new.classpath', namespaces=version.NS)
        self.assertIn('${jakarta.el.api.baseline.version}', old_classpath)
        self.assertIn('${jakarta.servlet.api.baseline.version}', old_classpath)
        self.assertIn('${jakarta.el.api.version}', new_classpath)
        self.assertIn('${jakarta.servlet.api.version}', new_classpath)
        self.assertNotIn('${jakarta.el.api.version}', old_classpath)
        self.assertNotIn('${jakarta.servlet.api.version}', old_classpath)
        self.assertNotIn('${jakarta.el.api.baseline.version}', new_classpath)
        self.assertNotIn('${jakarta.servlet.api.baseline.version}', new_classpath)

        jsp_fixture = dependencies(ET.parse(
            ROOT / 'compatibility/dependencies/jsp.xml').getroot())
        self.assertEqual('2.2.1', dependency_version(
            jsp_fixture[('javax.servlet.jsp', 'javax.servlet.jsp-api')]))

        jakarta_fixture = dependencies(ET.parse(
            ROOT / 'compatibility/dependencies/jakarta.xml').getroot())
        self.assertEqual('4.0.0', dependency_version(
            jakarta_fixture[('jakarta.el', 'jakarta.el-api')]))
        self.assertEqual('5.0.0', dependency_version(
            jakarta_fixture[('jakarta.servlet', 'jakarta.servlet-api')]))


if __name__ == '__main__':
    unittest.main()
