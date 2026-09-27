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

    def test_module_delta_omits_shared_closures(self):
        shared = graph.parse_report('''
The following plugins have been resolved:
   org.example:compiler:jar:1.0
      org.example:shared:jar:2.0
   org.example:tests:jar:1.0
      org.example:runner:jar:2.0
''')
        child = graph.parse_report('''
The following plugins have been resolved:
   org.example:compiler:jar:1.0
      org.example:shared:jar:2.0
   org.example:tests:jar:1.0
      org.example:runner:jar:2.0
      org.example:engine:jar:3.0
''')
        delta = graph.module_delta(shared, child)
        compiler = graph.purl('org.example:compiler:jar:1.0')
        tests = graph.purl('org.example:tests:jar:1.0')
        engine = graph.purl('org.example:engine:jar:3.0')
        self.assertNotIn(compiler, delta)
        self.assertEqual('direct', delta[tests]['relationship'])
        self.assertEqual('indirect', delta[engine]['relationship'])


if __name__ == '__main__':
    unittest.main()
