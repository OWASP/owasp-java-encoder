"""Security acceptance checks must remain effective with Python optimization."""
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]


class ReproducibilityChecks(unittest.TestCase):
    def test_different_or_missing_payload_rejected_when_optimized(self):
        for second in ({'encoder.jar': 'different'}, {}):
            code = ('import runpy; checks=runpy.run_path(' + repr(str(ROOT / 'scripts/check-reproducible.py'))
                    + '); checks["require_identical"]({"encoder.jar":"expected"}, ' + repr(second) + ')')
            result = subprocess.run([sys.executable, '-O', '-c', code], capture_output=True, text=True)
            self.assertNotEqual(0, result.returncode)
            self.assertIn('Artifact bytes differ: encoder.jar', result.stderr)


@unittest.skipUnless(shutil.which('gpg'), 'GnuPG required for real detached-signature fixture')
class ReleaseAcceptance(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory(prefix='enc-')
        cls.addClassCleanup(cls.temp.cleanup)
        cls.root = Path(cls.temp.name)
        cls.home = cls.root / 'g'
        cls.home.mkdir(mode=0o700)
        generated = subprocess.run(['gpg', '--homedir', str(cls.home), '--batch', '--pinentry-mode', 'loopback', '--passphrase', '',
                        '--quick-generate-key', 'Disposable TEST ONLY <nobody@example.invalid>',
                        'rsa2048', 'sign', '1d'], capture_output=True, text=True)
        if generated.returncode:
            raise RuntimeError(generated.stderr)
        keys = subprocess.check_output(['gpg', '--homedir', str(cls.home), '--with-colons', '--list-keys'], text=True)
        cls.fingerprint = next(row.split(':')[9] for row in keys.splitlines() if row.startswith('fpr:'))

    @classmethod
    def tearDownClass(cls):
        if shutil.which('gpgconf'):
            subprocess.run(['gpgconf', '--homedir', str(cls.home), '--kill', 'gpg-agent'], capture_output=True)
        cls.temp.cleanup()

    def setUp(self):
        self.project = self.root / self._testMethodName
        (self.project / 'target').mkdir(parents=True)
        pom = ('<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>'
               '<artifactId>encoder-parent</artifactId><version>9.9.9-validation</version>'
               '<scm><tag>v9.9.9-validation</tag></scm></project>')
        (self.project / 'pom.xml').write_text(pom)
        self.signed_pom = self.project / 'target/encoder-parent-9.9.9-validation.pom'
        self.signed_pom.write_text(pom)
        subprocess.run(['gpg', '--homedir', str(self.home), '--batch', '--armor', '--detach-sign',
                        str(self.signed_pom)], check=True, capture_output=True)

    def reject(self, fingerprint, message):
        output = self.project / 'must-not-exist.zip'
        result = subprocess.run([sys.executable, '-O', str(ROOT / 'scripts/package-release.py'),
                                 '--source', str(self.project), '--output', str(output),
                                 '--gnupg-home', str(self.home), '--fingerprint', fingerprint],
                                text=True, capture_output=True)
        self.assertNotEqual(0, result.returncode)
        self.assertIn(message, result.stderr)
        self.assertFalse(output.exists())

    def test_valid_signature_from_unexpected_key_rejected(self):
        self.reject('0' * 40, 'Unexpected signing fingerprint')

    def test_stale_signed_pom_rejected(self):
        with (self.project / 'pom.xml').open('a') as pom:
            pom.write('\n')
        self.reject(self.fingerprint, 'Stale signed POM')

    def test_snapshot_rejected(self):
        pom = self.project / 'pom.xml'
        pom.write_text(pom.read_text().replace('9.9.9-validation', '9.9.9-SNAPSHOT'))
        self.reject(self.fingerprint, 'Refuse snapshot bundle')


if __name__ == '__main__':
    unittest.main()
