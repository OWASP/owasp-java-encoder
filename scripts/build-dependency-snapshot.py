#!/usr/bin/env python3
"""Convert filtered Maven resolve-plugins 3.11.0 reports to a GitHub snapshot.

Runtime/test graphs are submitted by the Maven submission action. This separate
development-scope graph retains each invoked build plugin's resolved dependency
edges. Shared inherited tooling is submitted once on the root POM; child POMs
contain only module-specific plugin deltas. No network or credentials are needed
to generate the reviewable JSON file.
"""
import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
from urllib.parse import quote, urlencode


def purl(coordinate):
    parts = coordinate.split(':')
    if len(parts) not in (4, 5) or not all(parts):
        raise ValueError('Unexpected Maven artifact coordinate: ' + coordinate)
    group, artifact, packaging = parts[:3]
    qualifiers = {'type': packaging}
    if len(parts) == 5:
        qualifiers['classifier'] = parts[3]
    return ('pkg:maven/' + quote(group, safe='') + '/' + quote(artifact, safe='')
            + '@' + quote(parts[-1], safe='') + '?' + urlencode(sorted(qualifiers.items())))


def parse_report(text):
    if 'The following plugins have been resolved:' not in text:
        raise ValueError('Missing Maven resolve-plugins report header')
    resolved = {}
    plugin = None
    for line in text.splitlines():
        if not line.strip() or line == 'The following plugins have been resolved:':
            continue
        if not line.startswith('   ') or line.strip() == 'none':
            raise ValueError('Unexpected or empty plugin report: ' + line)
        indirect = line.startswith('      ')
        key = purl(line.strip())
        node = resolved.setdefault(key, {'package_url': key, 'relationship': 'indirect',
                                         'scope': 'development', 'dependencies': []})
        if not indirect:
            node['relationship'] = 'direct'
            plugin = key
        elif plugin is None:
            raise ValueError('Dependency precedes its plugin')
        elif key != plugin and key not in resolved[plugin]['dependencies']:
            resolved[plugin]['dependencies'].append(key)
    if not resolved:
        raise ValueError('Empty build dependency graph')
    return resolved


def module_delta(shared, resolved):
    """Return direct plugin closures that differ from the shared root graph."""
    delta = {}
    for key, node in resolved.items():
        if node['relationship'] != 'direct':
            continue
        common = shared.get(key)
        if (common is not None
                and set(common['dependencies']) == set(node['dependencies'])):
            continue
        delta[key] = node
        for dependency in node['dependencies']:
            delta[dependency] = resolved[dependency]
    return delta


def snapshot(root, correlator):
    reports = sorted(root.glob('**/target/build-dependencies.txt'))
    root_report = root / 'target' / 'build-dependencies.txt'
    if root_report not in reports:
        raise ValueError('No resolved root build dependency report')
    shared = parse_report(root_report.read_text())
    manifests = {}
    for report in reports:
        pom = (report.parent.parent / 'pom.xml').relative_to(root).as_posix()
        if not (root / pom).is_file():
            raise ValueError('No POM for report: ' + str(report))
        resolved = shared if report == root_report else module_delta(
            shared, parse_report(report.read_text()))
        if not resolved:
            continue
        manifests[pom + ' (build)'] = {
            'name': pom + ' (build)', 'file': {'source_location': pom},
            'resolved': resolved}
    return {'version': 0, 'sha': os.environ['GITHUB_SHA'], 'ref': os.environ['GITHUB_REF'],
            'job': {'correlator': correlator, 'id': os.environ['GITHUB_RUN_ID']},
            'detector': {'name': 'encoder-maven-build-graph', 'version': '1.0.0',
                         'url': 'https://github.com/OWASP/owasp-java-encoder'},
            'scanned': datetime.now(timezone.utc).isoformat(), 'manifests': manifests}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--correlator', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    result = snapshot(args.root.resolve(), args.correlator)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    print('Prepared build manifests:', ', '.join(result['manifests']))
