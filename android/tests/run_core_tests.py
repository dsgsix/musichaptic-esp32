"""JDK-only tests. Optional: python tests/run_core_tests.py path/to/v5/include

The optional firmware check also needs g++. It never flashes an ESP32.
"""
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

root = Path(__file__).resolve().parents[1]
package = 'com.rommelguerra.prototipomusichaptic'
main = root/'app/src/main/java'/package.replace('.', '/')/'haptics'
tests = root/'app/src/test/java'/package.replace('.', '/')
def run(*args):
    subprocess.run([str(x) for x in args], check=True)

with tempfile.TemporaryDirectory() as directory:
    out = Path(directory)
    compiler = [shutil.which('javac')] if shutil.which('javac') else ['java', '-m', 'jdk.compiler/com.sun.tools.javac.Main']
    classes = ['HapticProtocol', 'HapticCommandQueue', 'HapticTimeline', 'HapticIntensity', 'PitchDetector', 'NoteTone']
    suites = ['CoreTests', 'DualControlTests', 'MusicalControlTests', 'LevelFrequencyTests']
    run(*compiler, '-encoding', 'UTF-8', '--release', '11', '-d', out,
        *[main/(c+'.java') for c in classes], *[tests/(c+'.java') for c in suites])
    run('java', '-cp', out, package+'.CoreTests')
    for suite, size in [('DualControlTests', 10), ('MusicalControlTests', 15), ('LevelFrequencyTests', 16)]:
        frame = out/(suite+'.bin')
        run('java', '-cp', out, package+'.'+suite, frame)
        if len(sys.argv) > 1:
            bridge = out/'firmware_roundtrip'
            if not bridge.exists():
                run('g++', '-std=c++17', '-Wall', '-Wextra', '-Werror', '-I', Path(sys.argv[1]).resolve(), '-I', Path(sys.argv[1]).resolve().parent/'tests/stubs',
                    root/'tests/firmware_roundtrip.cpp', '-o', bridge)
            replies = out/(suite+'.acks')
            run(bridge, frame, replies, size)
            run('java', '-cp', out, package+'.'+suite, frame, replies)
print('PASS: selected checks complete. This does not compile or run the Android UI.')
