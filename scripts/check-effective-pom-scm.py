#!/usr/bin/env python3
"""Verify published modules' actual effective SCM metadata against the parent."""

import argparse
import os
from pathlib import Path
import subprocess
import tempfile
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]
NAMESPACE = 'http://maven.apache.org/POM/4.0.0'
FIELDS = ('connection', 'developerConnection', 'url')
MODULES = {'': 'encoder-parent', 'core': 'encoder', 'jsp': 'encoder-jsp',
           'jakarta': 'encoder-jakarta-jsp'}
MODULE_SUFFIXES = tuple('/' + name for name in
                        ('core', 'jsp', 'jakarta', 'esapi', 'encoder',
                         'encoder-jsp', 'encoder-jakarta-jsp', 'encoder-esapi'))


def qualified(name):
    return '{' + NAMESPACE + '}' + name


def read_scm(path, artifact):
    """Read SCM fields from a Maven effective POM, rejecting absent metadata."""
    project = ET.parse(path).getroot()
    if project.tag != qualified('project'):
        raise ValueError(str(path) + ': expected a Maven effective POM')
    actual_artifact = project.findtext(qualified('artifactId'))
    if actual_artifact != artifact:
        raise ValueError(str(path) + ': expected artifactId ' + artifact +
                         ', found ' + repr(actual_artifact))
    scm = project.find(qualified('scm'))
    if scm is None:
        raise ValueError(artifact + ': missing effective SCM metadata')
    values = {}
    for field in FIELDS:
        element = scm.find(qualified(field))
        value = element.text.strip() if element is not None and element.text else ''
        if not value:
            raise ValueError(artifact + ': missing effective scm/' + field)
        values[field] = value
    return values


def check_effective_poms(paths):
    """Compare the four parsed effective POMs, including inherited SCM paths."""
    if set(paths) != set(MODULES):
        raise ValueError('Expected effective POMs for parent, core, jsp and jakarta')
    parent = read_scm(paths[''], MODULES[''])
    for field, value in parent.items():
        if value.rstrip('/').endswith(MODULE_SUFFIXES):
            raise ValueError('encoder-parent: scm/' + field +
                             ' contains a module-appended path: ' + value)
    for module, artifact in MODULES.items():
        if not module:
            continue
        child = read_scm(paths[module], artifact)
        for field in FIELDS:
            if child[field] != parent[field]:
                suffix = (' (module-appended path)' if child[field].startswith(
                    parent[field].rstrip('/') + '/') else '')
                raise ValueError(artifact + ': effective scm/' + field +
                                 ' differs from encoder-parent' + suffix + ': ' +
                                 repr(child[field]) + ' != ' + repr(parent[field]))


def generate_and_check(root, maven, repository=None, settings=None, options=()):
    """Run the committed wrapper outside the Maven lifecycle for each published POM."""
    with tempfile.TemporaryDirectory(prefix='encoder-effective-scm-') as directory:
        paths = {}
        for module, artifact in MODULES.items():
            output = Path(directory) / (artifact + '.xml')
            pom = root / module / 'pom.xml'
            command = [str(maven), '-B', '-ntp', '-N', '-f', str(pom)]
            if repository is not None:
                command.append('-Dmaven.repo.local=' + str(repository))
            if settings is not None:
                command.extend(('-s', str(settings)))
            command.extend(options)
            command.extend(('help:effective-pom', '-Doutput=' + str(output)))
            subprocess.run(command, cwd=root, check=True)
            if not output.is_file():
                raise ValueError(artifact + ': Maven did not write an effective POM')
            paths[module] = output
        check_effective_poms(paths)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=ROOT)
    parser.add_argument('--maven', type=Path,
                        help='Maven executable (default: the committed wrapper)')
    parser.add_argument('--repository', type=Path,
                        help='Maven local repository (-Dmaven.repo.local)')
    parser.add_argument('--settings', type=Path, help='Maven settings.xml')
    parser.add_argument('--maven-option', action='append', default=[],
                        help='Additional Maven argument; repeat, e.g. --maven-option=-o')
    args = parser.parse_args()
    root = args.root.resolve()
    maven = args.maven or root / ('mvnw.cmd' if os.name == 'nt' else 'mvnw')
    repository = args.repository.resolve() if args.repository else None
    settings = args.settings.resolve() if args.settings else None
    if settings is not None and not settings.is_file():
        parser.error('Maven settings file does not exist: ' + str(settings))
    try:
        generate_and_check(root, maven, repository, settings, args.maven_option)
    except (ValueError, ET.ParseError, subprocess.CalledProcessError) as error:
        parser.exit(1, str(error) + '\n')
    print('Effective SCM metadata matches encoder-parent for all three published modules')


if __name__ == '__main__':
    main()
