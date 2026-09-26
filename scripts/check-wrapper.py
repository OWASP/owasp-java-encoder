#!/usr/bin/env python3
"""Exercise both platform bootstraps and the distribution checksum failure path."""
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
WRAPPER = 'mvnw.cmd' if os.name == 'nt' else 'mvnw'


def bootstrap(directory, bad_hash):
    project = directory / 'project'
    project.mkdir()
    shutil.copy2(ROOT / WRAPPER, project / WRAPPER)
    shutil.copytree(ROOT / '.mvn', project / '.mvn')
    properties = project / '.mvn/wrapper/maven-wrapper.properties'
    if bad_hash:
        properties.write_text(re.sub(r'distributionSha256Sum=.*',
                                    'distributionSha256Sum=' + '0' * 64,
                                    properties.read_text()))
    env = dict(os.environ, MAVEN_USER_HOME=str(directory / 'maven-home'))
    # The test must follow the reviewed URL and cannot reuse a wrapper override/cache.
    for name in ('MVNW_REPOURL', 'MVNW_USERNAME', 'MVNW_PASSWORD'):
        env.pop(name, None)
    result = subprocess.run([str(project / WRAPPER), '-version'], cwd=project,
                            env=env, text=True, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, timeout=240)
    if bad_hash:
        if result.returncode == 0 or 'SHA-256' not in result.stdout:
            raise ValueError('Checksum rejection did not work: ' + result.stdout)
    else:
        if result.returncode != 0 or 'Apache Maven 3.9.16' not in result.stdout:
            raise ValueError('Wrapper bootstrap failed: ' + result.stdout)
    print(('Rejected incorrect checksum' if bad_hash else 'Verified bootstrap'), WRAPPER)


if __name__ == '__main__':
    for bad_hash in (False, True):
        with tempfile.TemporaryDirectory(prefix='encoder-wrapper-') as root:
            bootstrap(Path(root), bad_hash)
