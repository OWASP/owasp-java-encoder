#!/usr/bin/env python3
"""Verify and assemble an existing signed release payload; never build/sign/upload."""
import argparse
import hashlib
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET
import zipfile

NS = {'p': 'http://maven.apache.org/POM/4.0.0'}
MODULES = {'': 'encoder-parent', 'core': 'encoder', 'jsp': 'encoder-jsp',
           'jakarta': 'encoder-jakarta-jsp', 'esapi': 'encoder-esapi'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--gnupg-home', type=Path, required=True)
    parser.add_argument('--fingerprint', required=True)
    args = parser.parse_args()
    expected = args.fingerprint.upper()
    assert re.fullmatch('[0-9A-F]{40}', expected), 'Use a full expected fingerprint'
    root = args.source.resolve()
    pom = ET.parse(root / 'pom.xml')
    version = pom.findtext('p:version', namespaces=NS)
    assert re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.-]+)?', version)
    assert not version.endswith('-SNAPSHOT'), 'Refuse snapshot bundle'
    assert pom.findtext('p:scm/p:tag', namespaces=NS) == 'v' + version
    entries = {}
    payloads = 0
    for module, artifact in MODULES.items():
        module_pom = ET.parse(root / module / 'pom.xml')
        assert module_pom.findtext('p:artifactId', namespaces=NS) == artifact
        if module:
            assert module_pom.findtext('p:parent/p:version', namespaces=NS) == version
        suffixes = ['.pom'] if not module else ['.pom', '.jar', '-sources.jar', '-javadoc.jar']
        for suffix in suffixes:
            filename = artifact + '-' + version + suffix
            path = root / module / 'target' / filename
            data = path.read_bytes()
            if suffix == '.pom':
                assert data == (root / module / 'pom.xml').read_bytes(), 'Stale signed POM: ' + filename
            signature = Path(str(path) + '.asc')
            result = subprocess.run(['gpg', '--homedir', str(args.gnupg_home.resolve()),
                                     '--batch', '--status-fd', '1', '--verify', str(signature), str(path)],
                                    text=True, capture_output=True, check=True)
            valid = [line.split() for line in result.stdout.splitlines()
                     if line.startswith('[GNUPG:] VALIDSIG ')]
            assert any(row[2] == expected or row[-1] == expected for row in valid), filename
            name = 'org/owasp/encoder/' + artifact + '/' + version + '/' + filename
            entries[name] = data
            entries[name + '.asc'] = signature.read_bytes()
            for algorithm in ('md5', 'sha1', 'sha256', 'sha512'):
                entries[name + '.' + algorithm] = hashlib.new(algorithm, data).hexdigest().encode('ascii')
            payloads += 1
    assert payloads == 17 and len(entries) == 102
    # Exclusive creation protects retained immutable bundles from accidental replacement.
    with zipfile.ZipFile(args.output, 'x', compression=zipfile.ZIP_DEFLATED) as archive:
        for name, data in sorted(entries.items()):
            archive.writestr(name, data)
    # Re-read the exact written ZIP and verify every generated checksum.
    with zipfile.ZipFile(args.output) as archive:
        assert set(archive.namelist()) == set(entries)
        for name, data in entries.items():
            assert archive.read(name) == data
    print('Verified 17 signatures; assembled 5 POMs, 12 JARs, signatures and 68 checksums:', args.output)
    print('Bundle SHA-256:', hashlib.sha256(args.output.read_bytes()).hexdigest())


if __name__ == '__main__':
    main()
