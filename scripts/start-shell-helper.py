#!/usr/bin/env python3
"""Start the Debug shell helper after in-app consent. Uses an already authorized ADB device."""
import argparse
import re
import shlex
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', required=True)
parser.add_argument('--package', default='lifeos.md.tapilot.e2everify')
args = parser.parse_args()
if not re.fullmatch(r'lifeos\.md\.tapilot(?:\.[a-zA-Z0-9_]+)*', args.package):
    parser.error('Unexpected package name')
adb = ['adb', '-s', args.serial]
def read(*command):
    return subprocess.check_output(adb + list(command), text=True, stderr=subprocess.PIPE, timeout=15).strip()
try:
    if read('shell', 'id', '-u') != '2000':
        raise RuntimeError('Use a normal shell connection, not adb root')
    token = read('shell', 'run-as', args.package, 'cat', 'files/shell-consent')
    if not re.fullmatch(r'[a-f0-9]{64}', token):
        raise RuntimeError('Invalid consent request')
    uid = read('shell', 'run-as', args.package, 'id', '-u')
    if not uid.isdigit(): raise RuntimeError('Invalid app identity')
    paths = read('shell', 'pm', 'path', args.package).splitlines()
    apk = next(p.removeprefix('package:') for p in paths if p.endswith('/base.apk'))
    command = f'CLASSPATH={shlex.quote(apk)} exec app_process /system/bin lifeos.md.tapilot.shell.DisplayHelperMain {shlex.quote(args.package)} {uid}'
    # Keep consent off command lines and logs. Revoke in the app to terminate the helper explicitly.
    child = subprocess.Popen(adb + ['shell', '-T', command], stdin=subprocess.PIPE)
    child.stdin.write((token + '\n').encode()); child.stdin.close()
    raise SystemExit(child.wait())
except (subprocess.SubprocessError, RuntimeError, StopIteration):
    raise SystemExit('Helper not started. Authorize it in the Debug experiment first, then check the selected ADB device.')
