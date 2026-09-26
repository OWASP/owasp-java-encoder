#!/usr/bin/env python3
"""Prepare on JDK 17; run the resulting isolated consumers on a chosen runtime."""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import struct
import subprocess
import tempfile
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'compatibility' / 'src'
ARTIFACTS = {
    'core': ('encoder', 'owasp.encoder', 'org.owasp.encoder', 'org.owasp.encoder', 'CoreConsumer'),
    'jsp': ('encoder-jsp', 'owasp.encoder.jsp', 'org.owasp.encoder.jsp', 'org.owasp.encoder.tag', 'TagConsumer'),
    'jakarta': ('encoder-jakarta-jsp', 'owasp.encoder.jakarta', 'org.owasp.encoder.jakarta', 'org.owasp.encoder.tag', 'TagConsumer'),
    'esapi': ('encoder-esapi', 'owasp.encoder.esapi', 'org.owasp.encoder.esapi', 'org.owasp.encoder.esapi', 'EsapiConsumer'),
}
API_MODULES = {'core': [], 'jsp': ['javax.servlet.jsp.api', 'javax.el.api', 'javax.servlet.api'],
               'jakarta': ['jakarta.servlet.jsp', 'jakarta.el', 'jakarta.servlet'], 'esapi': ['esapi']}
HOST_PACKAGES = {'core': [], 'jsp': ['javax.servlet.jsp', 'javax.servlet.jsp.tagext', 'javax.servlet.jsp.el', 'javax.el'],
                 'jakarta': ['jakarta.servlet.jsp', 'jakarta.servlet.jsp.tagext', 'jakarta.servlet.jsp.el', 'jakarta.el'],
                 'esapi': ['org.owasp.esapi', 'org.owasp.esapi.codecs', 'org.owasp.esapi.errors', 'org.owasp.esapi.reference']}


def run(*args, **kwargs):
    print('+', ' '.join(map(str, args)), flush=True)
    subprocess.run(list(map(str, args)), check=True, timeout=240, **kwargs)


def path(items):
    return os.pathsep.join(map(str, items))


def manifest(jar):
    with zipfile.ZipFile(jar) as archive:
        text = archive.read('META-INF/MANIFEST.MF').decode().replace('\r\n', '\n').replace('\n ', '')
    return dict(line.split(': ', 1) for line in text.splitlines() if ': ' in line)


def metadata(kind, jar, core):
    artifact, explicit, automatic, package, _ = ARTIFACTS[kind]
    attrs = manifest(jar)
    assert attrs['Multi-Release'] == 'true', jar
    assert attrs['Automatic-Module-Name'] == automatic, jar
    symbolic = 'org.owasp.encoder.jakarta-jsp' if kind == 'jakarta' else automatic
    assert attrs['Bundle-SymbolicName'] == symbolic, jar
    assert attrs['Bundle-ManifestVersion'] == '2', jar
    exports = attrs['Export-Package'].split(',')
    assert [entry.split(';')[0] for entry in exports] == [package], exports
    imports = attrs.get('Import-Package', '').split(',') if attrs.get('Import-Package') else []
    expected_imports = set(HOST_PACKAGES[kind]) - {'javax.servlet.jsp.el', 'javax.el', 'jakarta.servlet.jsp.el', 'jakarta.el'}
    if kind != 'core': expected_imports.add('org.owasp.encoder')
    assert set(x.split(';')[0] for x in imports) == expected_imports, imports
    assert not any(x.startswith('java.') for x in imports), imports
    with zipfile.ZipFile(jar) as archive, zipfile.ZipFile(core) as core_archive:
        names = archive.namelist()
        assert 'META-INF/versions/9/module-info.class' in names
        assert 'module-info.class' not in names
        for name in names:
            if name.endswith('.class'):
                data = archive.read(name)
                major = struct.unpack('>H', data[6:8])[0]
                if name == 'META-INF/versions/9/module-info.class':
                    assert major == 53, (jar, name, major)
                else:
                    assert not name.startswith('META-INF/versions/'), name
                    assert major <= 52, (jar, name, major)
                    assert name.startswith(package.replace('.', '/') + '/'), name
                    assert not re.search(r'(?:Test|IT|Consumer)\$?[^/]*\.class$', name), name
            assert not name.endswith('.jar'), name
        tlds = [name for name in names if name.endswith('.tld')]
        assert bool(tlds) == (kind in ('jsp', 'jakarta')), (kind, tlds)
        for tld in tlds:
            tree = ET.fromstring(archive.read(tld))
            assert tree.tag.endswith('taglib')
            references = [node.text for node in tree.iter() if node.tag.endswith(('tag-class', 'function-class'))]
            assert references, tld
            for name in references:
                resource = name.replace('.', '/') + '.class'
                assert resource in names or resource in core_archive.namelist(), (tld, name)
        pom = ET.fromstring(archive.read('META-INF/maven/org.owasp.encoder/' + artifact + '/pom.xml'))
        ns = {'p': 'http://maven.apache.org/POM/4.0.0'}
        for dep in pom.findall('p:dependencies/p:dependency', ns):
            group = dep.findtext('p:groupId', namespaces=ns)
            if group in ('junit', 'org.junit.jupiter', 'org.mockito', 'org.apache.felix'):
                assert dep.findtext('p:scope', namespaces=ns) == 'test', group
    print('Metadata passed:', jar.name)


def prepare(args):
    out = args.directory.resolve()
    out.mkdir(parents=True, exist_ok=True)
    jars = {}
    for kind, (artifact, *_) in ARTIFACTS.items():
        candidates = [p for p in (ROOT / kind / 'target').glob(artifact + '-*.jar')
                      if not p.name.endswith(('-sources.jar', '-javadoc.jar'))]
        assert len(candidates) == 1, candidates
        target = out / 'artifacts' / candidates[0].name
        target.parent.mkdir(exist_ok=True)
        shutil.copy2(candidates[0], target)
        jars[kind] = target
    for kind, jar in jars.items(): metadata(kind, jar, jars['core'])
    for kind in ('jsp', 'jakarta', 'esapi', 'osgi-r6', 'osgi-r8'):
        run(args.maven, '-B', '-ntp', '-f', ROOT / 'compatibility/dependencies' / (kind + '.xml'),
            '-Dmaven.repo.local=' + str(args.repository.resolve()),
            'org.apache.maven.plugins:maven-dependency-plugin:3.9.0:copy-dependencies',
            '-DincludeScope=runtime', '-DoutputDirectory=' + str(out / 'dependencies' / kind))
    for kind, (artifact, explicit, automatic, package, main) in ARTIFACTS.items():
        deps = sorted((out / 'dependencies' / kind).glob('*.jar'))
        artifacts = [jars['core']] + ([jars[kind]] if kind != 'core' else [])
        src = out / 'sources' / kind
        src.mkdir(parents=True, exist_ok=True)
        for name in ('Checks.java', main + '.java'):
            text = (SOURCE / name).read_text()
            if kind == 'jakarta': text = text.replace('javax.', 'jakarta.')
            (src / name).write_text(text)
        sources = sorted(src.glob('*.java'))
        classes = out / 'classes' / kind
        run('javac', '--release', '8', '-d', classes, '-cp', path(artifacts + deps), *sources)
        for mode, encoder_module in [('explicit', explicit), ('automatic', automatic)]:
            module_src = src / mode / 'module-info.java'
            module_src.parent.mkdir(exist_ok=True)
            requires = [encoder_module] + API_MODULES[kind]
            module_src.write_text('module consumer.fixture {\n' + ''.join('    requires ' + m + ';\n' for m in requires) + '}\n')
            module_deps = deps if kind != 'esapi' else [p for p in deps if p.name.startswith('esapi-')]
            run('javac', *(['-J-Djdk.util.jar.enableMultiRelease=false'] if mode == 'automatic' else []),
                '--release', '9', '-d', out / mode / kind,
                '--module-path', path(artifacts + module_deps), module_src, *sources)
        # Probe bundle includes consumer classes only; encoder classes are separate installed JARs.
        probe = out / 'probes' / (kind + '.jar')
        probe.parent.mkdir(exist_ok=True)
        imports = [package] + HOST_PACKAGES[kind]
        with zipfile.ZipFile(probe, 'w') as archive:
            archive.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\r\nBundle-ManifestVersion: 2\r\n'
                             + 'Bundle-SymbolicName: consumer.fixture\r\nImport-Package: ' + ','.join(imports) + '\r\n\r\n')
            for file in classes.rglob('*.class'): archive.write(file, file.relative_to(classes))
    framework = next((out / 'dependencies' / 'osgi-r6').glob('*.jar'))
    run('javac', '--release', '8', '-cp', framework, '-d', out / 'osgi', SOURCE / 'OsgiConsumer.java')
    (out / 'artifacts.json').write_text(json.dumps({k: v.name for k, v in jars.items()}))


def consume(args):
    out = args.directory.resolve()
    jars = {k: out / 'artifacts' / v for k, v in json.loads((out / 'artifacts.json').read_text()).items()}
    java = str(Path(args.java_home) / 'bin/java') if args.java_home else 'java'
    run(java, '-version')
    for kind, (_, explicit, automatic, package, main) in ARTIFACTS.items():
        deps = sorted((out / 'dependencies' / kind).glob('*.jar'))
        artifacts = [jars['core']] + ([jars[kind]] if kind != 'core' else [])
        run(java, '-cp', path([out / 'classes' / kind] + artifacts + deps), 'consumer.' + main)
        if args.runtime != 8:
            for mode, name in [('explicit', explicit), ('automatic', automatic)]:
                module_deps = deps if kind != 'esapi' else [p for p in deps if p.name.startswith('esapi-')]
                classpath = [p for p in deps if p not in module_deps]
                run(java, '-Djdk.util.jar.enableMultiRelease=' + str(mode == 'explicit').lower(),
                    '-Dconsumer.module=' + name, '--module-path', path([out / mode / kind] + artifacts + module_deps),
                    '--class-path', path(classpath), '--module', 'consumer.fixture/consumer.' + main)
        for framework in ('osgi-r6', 'osgi-r8'):
            framework_jars = sorted((out / 'dependencies' / framework).glob('*.jar'))
            with tempfile.TemporaryDirectory(prefix='encoder-osgi-') as storage:
                run(java, '-cp', path([out / 'osgi'] + framework_jars + deps), 'consumer.OsgiConsumer',
                    storage, ','.join(HOST_PACKAGES[kind]), 'consumer.' + main,
                    *artifacts, out / 'probes' / (kind + '.jar'))
        print('PASS Java', args.runtime, kind, flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['prepare', 'run'])
    parser.add_argument('--directory', type=Path, default=ROOT / 'target/compatibility')
    parser.add_argument('--maven', default='mvn')
    parser.add_argument('--repository', type=Path, default=Path.home() / '.m2/repository')
    parser.add_argument('--runtime', type=int, choices=[8, 11, 17, 21, 25], default=17)
    parser.add_argument('--java-home', default=os.environ.get('JAVA_HOME'))
    options = parser.parse_args()
    (prepare if options.action == 'prepare' else consume)(options)
