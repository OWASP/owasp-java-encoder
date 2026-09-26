#!/usr/bin/env python3
"""Compare twelve JARs and five installed POMs from the same committed sources."""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import subprocess
import tarfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
NS = {'p': 'http://maven.apache.org/POM/4.0.0'}
MODULES = {'': 'encoder-parent', 'core': 'encoder', 'jsp': 'encoder-jsp',
           'jakarta': 'encoder-jakarta-jsp', 'esapi': 'encoder-esapi'}


def require_identical(first, second):
    differences = sorted(name for name in set(first) | set(second)
                         if first.get(name) != second.get(name))
    if differences:
        raise ValueError('Artifact bytes differ: ' + ', '.join(differences))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--commit', required=True)
    parser.add_argument('--directory', type=Path, required=True)
    args = parser.parse_args()
    commit = subprocess.check_output(['git', 'rev-parse', '--verify', args.commit + '^{commit}'],
                                     cwd=ROOT, text=True).strip()
    java = Path(os.environ['JAVA_HOME']) / 'bin/java'
    props = subprocess.run([str(java), '-XshowSettings:properties', '-version'],
                           text=True, capture_output=True, check=True).stderr
    if 'java.vendor = Eclipse Adoptium' not in props or 'java.runtime.version = 17.0.20.1+1\n' not in props:
        raise ValueError('Unexpected reference JDK: ' + props)
    directory = args.directory.resolve()
    directory.mkdir(parents=True, exist_ok=False)
    archive = subprocess.check_output(['git', 'archive', commit], cwd=ROOT)
    results = []
    for name in ('first', 'second'):
        work = directory / name
        work.mkdir()
        with tarfile.open(fileobj=io.BytesIO(archive)) as source:
            source.extractall(work, filter='data')
        repository = directory / (name + '-m2')
        env = dict(os.environ, TZ='UTC', LC_ALL='C', LANG='C',
                   MAVEN_OPTS='-Duser.language=en -Duser.country=US -Duser.timezone=UTC -Dfile.encoding=UTF-8',
                   MAVEN_USER_HOME=str(directory / (name + '-wrapper')))
        for key in ('MAVEN_ARGS', 'MVNW_REPOURL', 'JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS'):
            env.pop(key, None)
        command = [str(work / ('mvnw.cmd' if os.name == 'nt' else 'mvnw')),
                   '-B', '-ntp', '-Dmaven.repo.local=' + str(repository), '-DskipTests', 'clean', 'install']
        with (directory / (name + '.log')).open('w') as log:
            subprocess.run(command, cwd=work, env=env, stdout=log, stderr=subprocess.STDOUT, check=True)
        version = ET.parse(work / 'pom.xml').findtext('p:version', namespaces=NS)
        hashes = {}
        for module, artifact in MODULES.items():
            base = repository / 'org/owasp/encoder' / artifact / version
            suffixes = ['.pom'] if not module else ['.pom', '.jar', '-sources.jar', '-javadoc.jar']
            for suffix in suffixes:
                filename = artifact + '-' + version + suffix
                hashes[filename] = hashlib.sha256((base / filename).read_bytes()).hexdigest()
        if len(hashes) != 17:
            raise ValueError('Expected seventeen release payload files')
        results.append(hashes)
    differences = [name for name in results[0] if results[0][name] != results[1][name]]
    report = {'commit': commit, 'java': props, 'maven': '3.9.16',
              'hashes': results, 'differences': differences}
    (directory / 'comparison.json').write_text(json.dumps(report, indent=2) + '\n')
    require_identical(results[0], results[1])
    print('Identical: twelve binary/source/Javadoc JARs and five POMs from', commit)


if __name__ == '__main__':
    main()
