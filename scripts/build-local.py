#!/usr/bin/env python3
"""Offline build with Android SDK tools and JDK; no Maven/Gradle downloads."""
from pathlib import Path
import os
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SDK = Path(os.environ.get('ANDROID_SDK_ROOT', str(Path.home() / 'Android/Sdk')))
TOOLS = SDK / 'build-tools' / os.environ.get('VIBER_BUILD_TOOLS', '36.0.0')
ANDROID = SDK / 'platforms' / 'android-35' / 'android.jar'
BUILD = ROOT / 'build' / 'local'
OUT = ROOT / 'dist'
KEY = ROOT / 'build' / 'noslop-debug.jks'

def run(*args):
    subprocess.run([str(a) for a in args], check=True, cwd=ROOT)

for directory in (BUILD / 'classes', BUILD / 'dex', BUILD / 'generated', OUT):
    directory.mkdir(parents=True, exist_ok=True)
# Prevent stale removed classes from entering subsequent builds.
shutil.rmtree(BUILD / 'classes')
(BUILD / 'classes').mkdir()
shutil.rmtree(BUILD / 'dex')
(BUILD / 'dex').mkdir()
main = ROOT / 'app/src/main'
run(TOOLS / 'aapt', 'package', '-f', '-M', main / 'AndroidManifest.xml',
    '-S', main / 'res', '-A', main / 'assets', '-I', ANDROID,
    '-J', BUILD / 'generated', '-F', BUILD / 'resources.apk', '--version-code', '5', '--version-name', '0.2.3')
sources = sorted((main / 'java').rglob('*.java'))
run('javac', '-encoding', 'UTF-8', '-source', '11', '-target', '11',
    '-classpath', f'{ANDROID}{os.pathsep}{ROOT / "app/libs/api-82.jar"}',
    '-d', BUILD / 'classes', *sources)
run('jar', 'cf', BUILD / 'classes.jar', '-C', BUILD / 'classes', '.')
run(TOOLS / 'd8', '--lib', ANDROID, '--classpath', ROOT / 'app/libs/api-82.jar',
    '--min-api', '32', '--output', BUILD / 'dex', BUILD / 'classes.jar')
shutil.copyfile(BUILD / 'resources.apk', BUILD / 'unsigned.apk')
with zipfile.ZipFile(BUILD / 'unsigned.apk', 'a', zipfile.ZIP_DEFLATED) as apk:
    for dex in sorted((BUILD / 'dex').glob('*.dex')):
        apk.write(dex, dex.name)
run(TOOLS / 'zipalign', '-f', '4', BUILD / 'unsigned.apk', BUILD / 'aligned.apk')
if not KEY.exists():
    run('keytool', '-genkeypair', '-keystore', KEY, '-storepass', 'android',
        '-keypass', 'android', '-alias', 'viber-noslop', '-keyalg', 'RSA', '-keysize', '3072',
        '-validity', '10000', '-dname', 'CN=Viber NoSlop local development')
run(TOOLS / 'apksigner', 'sign', '--ks', KEY, '--ks-pass', 'pass:android',
    '--key-pass', 'pass:android', '--out', OUT / 'viber-noslop-0.2.3.apk', BUILD / 'aligned.apk')
run(TOOLS / 'apksigner', 'verify', '--verbose', OUT / 'viber-noslop-0.2.3.apk')
print(OUT / 'viber-noslop-0.2.3.apk')
