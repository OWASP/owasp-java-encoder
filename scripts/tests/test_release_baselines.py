"""Release guards derived from immutable semantic-version tags."""

from pathlib import Path
import re
import subprocess
import tempfile
import unittest
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
NS = {'p': 'http://maven.apache.org/POM/4.0.0'}
RELEASE_TAG = re.compile(r'^v(\d+)\.(\d+)\.(\d+)$')
PROJECT_VERSION = re.compile(r'^(\d+)\.(\d+)\.(\d+)(?:[-+].*)?$')


def immutable_release_versions(repository=ROOT):
    """Include signed release sources integrated by squash, not just ancestors."""
    versions = []
    tags = subprocess.check_output(
        ['git', 'tag', '--list', 'v*'], cwd=repository,
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

    def test_future_release_is_not_a_baseline(self):
        releases = [((1, 5, 0), '1.5.0'), ((2, 0, 0), '2.0.0')]
        self.assertEqual('1.5.0', preceding_release(releases, '1.5.1-SNAPSHOT'))


class ReleaseTagDiscovery(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.repository = Path(temporary.name)
        self.git('init', '--quiet')
        self.git('commit', '--quiet', '--allow-empty', '-m', 'Initial fixture')

    def git(self, *arguments):
        return subprocess.check_output([
            'git', '-c', 'user.name=Release test',
            '-c', 'user.email=release-test@example.invalid',
            '-c', 'commit.gpgSign=false', '-c', 'tag.gpgSign=false',
            '-c', 'core.hooksPath=/dev/null', *arguments,
        ], cwd=self.repository, text=True, stderr=subprocess.STDOUT).strip()

    def test_squash_merged_release_tag_is_discovered(self):
        base = self.git('rev-parse', 'HEAD')
        self.git('tag', '-a', 'v1.4.1', '-m', 'Previous release')
        self.git('checkout', '--quiet', '--detach')
        self.git('commit', '--quiet', '--allow-empty', '-m', 'Tested release')
        self.git('tag', '-a', 'v1.5.0', '-m', 'Released source')
        self.git('checkout', '--quiet', '--detach', base)
        self.git('commit', '--quiet', '--allow-empty', '-m', 'Squash merge')
        self.assertNotIn('v1.5.0', self.git('tag', '--merged', 'HEAD').splitlines())
        versions = immutable_release_versions(self.repository)
        self.assertEqual('1.5.0', preceding_release(versions, '1.5.1-SNAPSHOT'))
        self.assertEqual('1.4.1', preceding_release(versions, '1.5.0'))

    def test_nonrelease_tags_are_ignored(self):
        for tag in ('v1.5.0', 'v2.0.0-rc1', 'v99.0', 'notes', 'v1.5.0-extra'):
            self.git('tag', tag)
        self.assertEqual([((1, 5, 0), '1.5.0')],
                         immutable_release_versions(self.repository))

    def test_missing_release_tags_fail_closed(self):
        with self.assertRaisesRegex(ValueError, 'No immutable'):
            immutable_release_versions(self.repository)


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
