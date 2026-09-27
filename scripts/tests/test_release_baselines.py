"""Release guards derived from immutable semantic-version tags."""

from pathlib import Path
import re
import subprocess
import unittest
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
NS = {'p': 'http://maven.apache.org/POM/4.0.0'}
RELEASE_TAG = re.compile(r'^v(\d+)\.(\d+)\.(\d+)$')
PROJECT_VERSION = re.compile(r'^(\d+)\.(\d+)\.(\d+)(?:[-+].*)?$')


def immutable_release_versions():
    versions = []
    tags = subprocess.check_output(
        ['git', 'tag', '--merged', 'HEAD', '--list', 'v*'], cwd=ROOT,
        text=True).splitlines()
    for tag in tags:
        match = RELEASE_TAG.match(tag)
        if match:
            versions.append((tuple(int(part) for part in match.groups()), tag[1:]))
    if not versions:
        raise ValueError('No immutable semantic-version release tags found')
    return versions


def version_tuple(version):
    match = PROJECT_VERSION.match(version)
    if not match:
        raise ValueError('Project version is not semantic: ' + version)
    return tuple(int(part) for part in match.groups())


def preceding_release(versions, project_version):
    """Return the newest immutable release strictly older than the build."""
    current = version_tuple(project_version)
    preceding = [release for release in versions if release[0] < current]
    if not preceding:
        raise ValueError('No immutable release precedes ' + project_version)
    return max(preceding)[1]


class BaselineSelection(unittest.TestCase):
    def test_release_tag_at_head_is_not_its_own_baseline(self):
        releases = [((1, 4, 1), '1.4.1'), ((1, 5, 0), '1.5.0')]
        self.assertEqual('1.4.1', preceding_release(releases, '1.5.0'))

    def test_next_snapshot_uses_the_completed_release(self):
        releases = [((1, 4, 1), '1.4.1'), ((1, 5, 0), '1.5.0')]
        self.assertEqual('1.5.0', preceding_release(releases, '1.5.1-SNAPSHOT'))


class PublicApiBaseline(unittest.TestCase):
    def test_japicmp_uses_preceding_immutable_release(self):
        pom = ET.parse(ROOT / 'pom.xml').getroot()
        project_version = pom.findtext('p:version', namespaces=NS)
        expected = preceding_release(immutable_release_versions(), project_version)
        baseline = pom.findtext('p:properties/p:public.api.baseline.version',
                                namespaces=NS)
        self.assertEqual(expected, baseline)

        plugins = pom.findall('p:build/p:pluginManagement/p:plugins/p:plugin', NS)
        execution = next(plugin for plugin in plugins
                         if plugin.findtext('p:artifactId', namespaces=NS)
                         == 'exec-maven-plugin')
        dependencies = execution.findall('p:dependencies/p:dependency', NS)
        self.assertTrue(any(
            dependency.findtext('p:artifactId', namespaces=NS) == 'japicmp'
            for dependency in dependencies))
        arguments = [node.text for node in execution.findall(
            'p:executions/p:execution/p:configuration/p:arguments/p:argument', NS)]
        self.assertTrue(any('${public.api.baseline.version}' in argument
                            for argument in arguments))
        self.assertIn('--error-on-binary-incompatibility', arguments)
        self.assertIn('--error-on-source-incompatibility', arguments)

        for module in ('core', 'jsp', 'jakarta'):
            child = ET.parse(ROOT / module / 'pom.xml').getroot()
            plugins = child.findall('p:build/p:plugins/p:plugin', NS)
            configured = next(plugin for plugin in plugins
                              if plugin.findtext('p:artifactId', namespaces=NS)
                              == 'exec-maven-plugin')
            baseline_dependency = configured.find(
                'p:dependencies/p:dependency/p:version', NS)
            self.assertEqual('${public.api.baseline.version}', baseline_dependency.text)


if __name__ == '__main__':
    unittest.main()
