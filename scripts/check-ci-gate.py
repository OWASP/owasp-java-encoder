#!/usr/bin/env python3
"""Fail closed unless every explicitly required job succeeded."""
import json
import os
import sys


def check(needs, required):
    if set(needs) != set(required):
        raise ValueError('Unexpected or missing required jobs: ' + repr(needs))
    failed = {name: job.get('result') for name, job in needs.items()
              if job.get('result') != 'success'}
    if failed:
        raise ValueError('Required jobs did not succeed: ' + repr(failed))


if __name__ == '__main__':
    check(json.loads(os.environ['NEEDS']), sys.argv[1:])
    print('All required jobs succeeded')
