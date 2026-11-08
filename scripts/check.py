#!/usr/bin/env python3
"""Check discovery on an original APK, resource rules and artifact contents."""
from pathlib import Path
import os
import re
import subprocess
import sys
import zipfile

root = Path(__file__).resolve().parents[1]
apk = Path(sys.argv[1]).resolve()
sdk = Path(os.environ.get('ANDROID_SDK_ROOT', str(Path.home() / 'Android/Sdk')))
android = sdk / 'platforms/android-35/android.jar'
out = root / 'build/tests'
out.mkdir(parents=True, exist_ok=True)
src = root / 'app/src/main/java/io/github/vibernoslop'
subprocess.run(['javac', '-encoding', 'UTF-8', '-cp', str(android), '-d', str(out),
                *(str(src / name) for name in ('DexFinder.java', 'UiRules.java', 'Options.java')),
                *(str(p) for p in (root / 'tests').glob('*.java'))], check=True)
classpath = str(out) + os.pathsep + str(android)
for test in ('DexFinderCheck', 'UiRulesCheck'):
    subprocess.run(['java', '-XX:-UsePerfData', '-Xmx256m', '-cp', classpath, 'io.github.vibernoslop.' + test, str(apk)], check=True)
aapt = sdk / 'build-tools/36.0.0/aapt'
resources = subprocess.check_output([str(aapt), 'dump', 'resources', str(apk)], text=True)
rules = (src / 'UiRules.java').read_text()
missing = []
for name in re.findall(r'case "([^"]+)"', rules):
    if not re.search(r':(?:id|layout)/' + re.escape(name) + r':', resources):
        missing.append(name)
if missing:
    raise SystemExit('Resource rules not found in supplied APK: ' + ', '.join(missing))
print('All explicit UI resource rules exist in target APK')
module = root / 'dist/viber-noslop-0.2.3.apk'
with zipfile.ZipFile(module) as z:
    assert z.read('assets/xposed_init').decode().strip() == 'io.github.vibernoslop.XposedModule'
    assert 'classes.dex' in z.namelist()
    assert not any(p.endswith('.apk') or p.endswith('.png') for p in z.namelist())
print('APK entry point and packaging checks passed')
