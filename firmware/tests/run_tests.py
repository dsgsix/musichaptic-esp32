"""Optional developer checks. Requires Python 3 and g++; not needed to upload."""
from pathlib import Path
import os,subprocess,tempfile
root=Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory(prefix='vibrafil_dual_') as tmp:
    for name in ('protocol_test','model_test','module_test','dual_test','level_test'):
        executable=Path(tmp)/(name+('.exe' if os.name=='nt' else ''))
        subprocess.run([os.environ.get('CXX','g++'),'-std=c++11','-Wall','-Wextra','-Werror','-pedantic',
            '-I',str(root/'tests/stubs'),'-I',str(root/'include'),str(root/'tests'/f'{name}.cpp'),'-o',str(executable)],check=True)
        subprocess.run([str(executable)],check=True)
