#!/usr/bin/env python3
"""Check development versions and the explicit, reviewed release-commit form.

This checks build inputs; it never authorizes publishing or verifies signatures.
"""
import argparse
from pathlib import Path
import re
import xml.etree.ElementTree as ET

NS = {'p': 'http://maven.apache.org/POM/4.0.0'}


def check(root, snapshot_only=False, ref=''):
    pom = ET.parse(root / 'pom.xml')
    version = pom.findtext('p:version', namespaces=NS)
    tag = pom.findtext('p:scm/p:tag', namespaces=NS)
    if not version or not re.fullmatch(r'\d+\.\d+\.\d+(?:-SNAPSHOT)?', version):
        raise ValueError('Root version must be an explicit x.y.z[-SNAPSHOT]')
    snapshot = version.endswith('-SNAPSHOT')
    if snapshot:
        if tag != 'HEAD':
            raise ValueError('Development versions require SCM tag HEAD')
    elif snapshot_only or tag != 'v' + version:
        raise ValueError('Development requires -SNAPSHOT; an intentional release commit must set SCM tag v<VERSION>')
    if ref.startswith('refs/tags/') and (snapshot or ref != 'refs/tags/v' + version):
        raise ValueError('Release tag and POM version must match')
    for module in pom.findall('p:modules/p:module', NS):
        child = ET.parse(root / module.text / 'pom.xml')
        if child.findtext('p:parent/p:version', namespaces=NS) != version:
            raise ValueError(module.text + ': parent version differs from root')
        own_version = child.findtext('p:version', namespaces=NS)
        if own_version is not None and own_version != version:
            raise ValueError(module.text + ': library version differs from root')
    app = ET.parse(root / 'jakarta-test/pom.xml')
    if app.findtext('p:properties/p:encoder.version', namespaces=NS) != version:
        raise ValueError('jakarta-test must test the current reactor encoder.version')
    return 'snapshot' if snapshot else 'release'


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--snapshot-only', action='store_true')
    parser.add_argument('--ref', default='')
    args = parser.parse_args()
    try:
        print('Version policy passed:', check(args.root, args.snapshot_only, args.ref))
    except (ValueError, ET.ParseError) as error:
        parser.exit(1, str(error) + '\n')
