#!/usr/bin/env python3
"""Prepare on JDK 17; run the resulting isolated consumers on a chosen runtime."""
import argparse
import hashlib
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
# Versions the framework exports for host packages, copied from the Export-Package
# headers of the API JARs in compatibility/dependencies (ESAPI has no OSGi metadata).
HOST_VERSIONS = {'javax.servlet.jsp': '2.2.1', 'javax.servlet.jsp.tagext': '2.2.1', 'javax.servlet.jsp.el': '2.2.1',
                 'javax.el': '2.2.5', 'jakarta.servlet.jsp': '3.0.0.SNAPSHOT', 'jakarta.servlet.jsp.tagext': '3.0.0.SNAPSHOT',
                 'jakarta.servlet.jsp.el': '3.0.0.SNAPSHOT', 'jakarta.el': '4.0.0'}
# Published Import-Package version ranges (#137); None means deliberately unversioned.
# The tags call Encode.forJson (1.5); the ESAPI adapter's floor is the oldest supported core.
IMPORT_RANGES = {
    'core': {},
    'jsp': {'org.owasp.encoder': '[1.5,2)', 'javax.servlet.jsp': '[2.0,3)', 'javax.servlet.jsp.tagext': '[2.0,3)'},
    'jakarta': {'org.owasp.encoder': '[1.5,2)', 'jakarta.servlet.jsp': '[3.0,4)', 'jakarta.servlet.jsp.tagext': '[3.0,4)'},
    'esapi': {'org.owasp.encoder': '[1.4.1,2)', 'org.owasp.esapi': None, 'org.owasp.esapi.codecs': None,
              'org.owasp.esapi.errors': None, 'org.owasp.esapi.reference': None},
}


def run(*args, **kwargs):
    print('+', ' '.join(map(str, args)), flush=True)
    subprocess.run(list(map(str, args)), check=True, timeout=240, **kwargs)


def path(items):
    return os.pathsep.join(map(str, items))


def digest(file):
    return hashlib.sha256(file.read_bytes()).hexdigest()


def clauses(value):
    # OSGi version ranges may contain a comma inside a quoted attribute.
    return re.split(r',(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)', value) if value else []


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
    assert 'Require-Capability' not in attrs, attrs
    exports = clauses(attrs['Export-Package'])
    assert [entry.split(';')[0] for entry in exports] == [package], exports
    assert ';version="' + '.'.join(attrs['Bundle-Version'].split('.')[:3]) + '"' in exports[0], exports
    imports = clauses(attrs.get('Import-Package', ''))
    actual_ranges = {}
    for entry in imports:
        name, *parameters = entry.split(';')
        versions = [p.split('=', 1)[1].strip('"') for p in parameters if p.startswith('version=')]
        assert name not in actual_ranges, ('duplicate import', entry)
        actual_ranges[name] = versions[0] if versions else None
    assert actual_ranges == IMPORT_RANGES[kind], (kind, imports)
    assert not any(x.startswith('java.') for x in imports), imports
    with zipfile.ZipFile(jar) as archive, zipfile.ZipFile(core) as core_archive:
        names = archive.namelist()
        assert len(names) == len(set(names)), (jar, 'duplicate ZIP entries')
        assert 'META-INF/versions/9/module-info.class' in names
        assert 'module-info.class' not in names
        for name in names:
            if name.startswith('META-INF/versions/') and not name.endswith('/'):
                assert name == 'META-INF/versions/9/module-info.class', name
            if name.endswith('.class'):
                data = archive.read(name)
                major = struct.unpack('>H', data[6:8])[0]
                if name == 'META-INF/versions/9/module-info.class':
                    assert major == 53, (jar, name, major)
                else:
                    assert not name.startswith('META-INF/versions/'), name
                    assert major == 52, (jar, name, major)
                    assert name.startswith(package.replace('.', '/') + '/'), name
                    assert not re.search(r'(?:Test|IT|Consumer)\$?[^/]*\.class$', name), name
            assert not name.endswith('.jar'), name
            assert not name.startswith(('org/junit/', 'org/mockito/', 'org/apache/felix/', 'consumer/')), name
        tlds = [name for name in names if name.endswith('.tld')]
        expected_tlds = {'META-INF/java-encoder.tld', 'META-INF/java-encoder-advanced.tld'} if kind in ('jsp', 'jakarta') else set()
        assert set(tlds) == expected_tlds, (kind, tlds)
        for tld in tlds:
            tree = ET.fromstring(archive.read(tld))
            assert tree.tag.endswith('taglib')
            namespace = {'t': tree.tag.split('}')[0][1:]}
            base_uri = 'owasp.encoder.jakarta' if kind == 'jakarta' else 'https://www.owasp.org/index.php/OWASP_Java_Encoder_Project'
            expected_uri = base_uri + ('#advanced' if kind == 'jsp' else '.advanced') if '-advanced' in tld else base_uri
            assert tree.findtext('t:uri', namespaces=namespace) == expected_uri, tld
            references = [node.text for node in tree.iter() if node.tag.endswith(('tag-class', 'function-class'))]
            assert references, tld
            for name in references:
                resource = name.replace('.', '/') + '.class'
                assert resource in names or resource in core_archive.namelist(), (tld, name)
        pom = ET.fromstring(archive.read('META-INF/maven/org.owasp.encoder/' + artifact + '/pom.xml'))
        ns = {'p': 'http://maven.apache.org/POM/4.0.0'}
        version = pom.findtext('p:parent/p:version', namespaces=ns)
        assert pom.findtext('p:artifactId', namespaces=ns) == artifact, artifact
        assert jar.name == artifact + '-' + version + '.jar', jar
        assert attrs['Bundle-Version'] == version.replace('-SNAPSHOT', '.SNAPSHOT'), attrs
        runtime_dependencies = set()
        provided_dependencies = set()
        for dep in pom.findall('p:dependencies/p:dependency', ns):
            group = dep.findtext('p:groupId', namespaces=ns)
            scope = dep.findtext('p:scope', default='compile', namespaces=ns)
            coordinate = (group, dep.findtext('p:artifactId', namespaces=ns))
            if scope in ('compile', 'runtime'):
                runtime_dependencies.add(coordinate)
                assert dep.findtext('p:optional', default='false', namespaces=ns) == 'false', coordinate
            if scope == 'provided': provided_dependencies.add(coordinate)
        expected_dependencies = set() if kind == 'core' else {('org.owasp.encoder', 'encoder')}
        if kind == 'esapi': expected_dependencies.add(('org.owasp.esapi', 'esapi'))
        assert runtime_dependencies == expected_dependencies, (kind, runtime_dependencies)
        expected_provided = {'jsp': {('javax.servlet.jsp', 'javax.servlet.jsp-api')},
                             'jakarta': {('jakarta.servlet.jsp', 'jakarta.servlet.jsp-api')}}.get(kind, set())
        assert provided_dependencies == expected_provided, (kind, provided_dependencies)
    print('Metadata passed:', jar.name)


def prepare(args):
    out = args.directory.resolve()
    if out.exists() and any(out.iterdir()):
        raise ValueError('Preparation requires an empty directory; run mvn clean verify or choose a new --directory: ' + str(out))
    out.mkdir(parents=True, exist_ok=True)
    shutil.copytree(ROOT / 'compatibility/config', out / 'config', dirs_exist_ok=True)
    ns = {'p': 'http://maven.apache.org/POM/4.0.0'}
    parent = ET.parse(ROOT / 'pom.xml')
    for dep in parent.findall('p:dependencies/p:dependency', ns):
        assert dep.findtext('p:scope', namespaces=ns) == 'test', ET.tostring(dep)
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
    run('javac', '--release', '9', '-d', out / 'metadata', SOURCE / 'ModuleMetadata.java')
    run('java', '-cp', out / 'metadata', 'consumer.ModuleMetadata', *jars.values())
    # javac's module discovery does not honor the runtime multi-release property.
    # Compile against clearly separated descriptor-free copies, but ALWAYS execute
    # against the original packaged JARs with multi-release support disabled.
    compile_only = out / 'compile-only-automatic'
    compile_only.mkdir(exist_ok=True)
    for jar in jars.values():
        with zipfile.ZipFile(jar) as source, zipfile.ZipFile(compile_only / jar.name, 'w') as target:
            for entry in source.infolist():
                if not entry.filename.endswith('module-info.class'):
                    target.writestr(entry, source.read(entry))
    for kind in ('jsp', 'jakarta', 'esapi', 'osgi-r6', 'osgi-r8', 'legacy-core'):
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
            compile_artifacts = [compile_only / p.name for p in artifacts] if mode == 'automatic' else artifacts
            run('javac', '--release', '9', '-d', out / mode / kind,
                '--module-path', path(compile_artifacts + module_deps), module_src, *sources)
        # Probe bundle includes consumer classes only; encoder classes are separate installed JARs.
        probe = out / 'probes' / (kind + '.jar')
        probe.parent.mkdir(exist_ok=True)
        imports = [package] + HOST_PACKAGES[kind]
        with zipfile.ZipFile(probe, 'w') as archive:
            header = 'Import-Package: ' + ','.join(imports)
            folded_imports = '\r\n '.join(header[i:i + 70] for i in range(0, len(header), 70))
            archive.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\r\nBundle-ManifestVersion: 2\r\n'
                             + 'Bundle-SymbolicName: consumer.fixture\r\n' + folded_imports + '\r\n\r\n')
            for file in classes.rglob('*.class'): archive.write(file, file.relative_to(classes))
    framework = next((out / 'dependencies' / 'osgi-r6').glob('*.jar'))
    run('javac', '--release', '8', '-cp', framework, '-d', out / 'osgi', SOURCE / 'OsgiConsumer.java')
    (out / 'artifacts.json').write_text(json.dumps({k: {'file': v.name, 'sha256': digest(v)} for k, v in jars.items()}))


def consume(args):
    out = args.directory.resolve()
    inventory = json.loads((out / 'artifacts.json').read_text())
    jars = {k: out / 'artifacts' / v['file'] for k, v in inventory.items()}
    for kind, jar in jars.items():
        assert digest(jar) == inventory[kind]['sha256'], ('Artifact changed', jar)
    java = str(Path(args.java_home) / 'bin/java') if args.java_home else 'java'
    run(java, '-version')
    runtime = subprocess.check_output([java, '-XshowSettings:properties', '-version'], stderr=subprocess.STDOUT, text=True)
    expected = '1.8' if args.runtime == 8 else str(args.runtime)
    assert re.search(r'java.specification.version = ' + re.escape(expected) + r'\s', runtime), runtime
    for kind, (_, explicit, automatic, package, main) in ARTIFACTS.items():
        deps = sorted((out / 'dependencies' / kind).glob('*.jar'))
        artifacts = [jars['core']] + ([jars[kind]] if kind != 'core' else [])
        config = ['-Dorg.owasp.esapi.resources=' + str(out / 'config')] if kind == 'esapi' else []
        run(java, *config, '-cp', path([out / 'classes' / kind] + artifacts + deps), 'consumer.' + main)
        if args.runtime != 8:
            for mode, name in [('explicit', explicit), ('automatic', automatic)]:
                module_deps = deps if kind != 'esapi' else [p for p in deps if p.name.startswith('esapi-')]
                classpath = [p for p in deps if p not in module_deps]
                run(java, *config, '-Djdk.util.jar.enableMultiRelease=' + str(mode == 'explicit').lower(),
                    '-Dconsumer.module=' + name, '--module-path', path([out / mode / kind] + artifacts + module_deps),
                    '--class-path', path(classpath), '--module', 'consumer.fixture/consumer.' + main)
        host = ','.join(p + (';version="' + HOST_VERSIONS[p] + '"' if p in HOST_VERSIONS else '')
                        for p in HOST_PACKAGES[kind])
        legacy_core = sorted((out / 'dependencies' / 'legacy-core').glob('encoder-*.jar'))
        for framework in ('osgi-r6', 'osgi-r8'):
            framework_jars = sorted((out / 'dependencies' / framework).glob('*.jar'))
            with tempfile.TemporaryDirectory(prefix='encoder-osgi-') as storage:
                run(java, *config, '-cp', path([out / 'osgi'] + framework_jars + deps), 'consumer.OsgiConsumer',
                    storage, host, 'consumer.' + main,
                    *artifacts, out / 'probes' / (kind + '.jar'))
            if kind != 'core':
                # A released core older than the adapter's import range must not wire.
                assert len(legacy_core) == 1, legacy_core
                with tempfile.TemporaryDirectory(prefix='encoder-osgi-') as storage:
                    run(java, *config, '-cp', path([out / 'osgi'] + framework_jars + deps), 'consumer.OsgiConsumer',
                        storage, host, '--expect-unresolved', legacy_core[0], jars[kind])
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
