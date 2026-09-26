"""Verify plugin graph parsing keeps edges, scope, classifiers and fails closed."""
from test_ci_policy import load
import unittest

graph = load('build-dependency-snapshot')


class BuildGraph(unittest.TestCase):
    def test_edges_and_direct_precedence(self):
        nodes = graph.parse_report('''
The following plugins have been resolved:
   org.example:compiler:jar:1.0
      org.example:shared:jar:2.0
      org.example:shared:jar:tests:2.0
   org.example:shared:jar:2.0
      org.example:compiler:jar:1.0
''')
        compiler = graph.purl('org.example:compiler:jar:1.0')
        shared = graph.purl('org.example:shared:jar:2.0')
        tests = graph.purl('org.example:shared:jar:tests:2.0')
        self.assertEqual([shared, tests], nodes[compiler]['dependencies'])
        self.assertEqual('direct', nodes[shared]['relationship'])
        self.assertEqual('indirect', nodes[tests]['relationship'])
        self.assertTrue(all(n['scope'] == 'development' for n in nodes.values()))

    def test_invalid_or_empty_reports_rejected(self):
        for report in ('', 'The following plugins have been resolved:',
                       'The following plugins have been resolved:\n   none',
                       'The following plugins have been resolved:\n      a:b:jar:1',
                       'The following plugins have been resolved:\n   a:b'):
            with self.subTest(report=report), self.assertRaises(ValueError):
                graph.parse_report(report)


if __name__ == '__main__':
    unittest.main()
