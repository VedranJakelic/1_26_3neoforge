from pathlib import Path
import subprocess,re,shutil
r=Path(__file__).parent.resolve();cp=':'.join(map(str,[r/'client-patched.jar',r/'neoforge-universal.jar',*(r/'libs').glob('*.jar')]))
jdk=next((r/'jdk').glob('*'));(r/'probe').mkdir(exist_ok=True);(r/'client-run/mods').mkdir(parents=True,exist_ok=True)
subprocess.run([str(jdk/'bin/javac'),'-cp',cp,'-d',str(r/'probe'),str(r/'ClientMixinProbe.java')],check=True)
shutil.copy(r/'shamaschizm-dev.jar',r/'client-run/mods')
names=[]
for p in Path('shamaschizm-26.3/src/main/java/net/beamex/shamaschizm/mixin').glob('*.java'):
 s=p.read_text();m=re.search(r'@Mixin\((\w+)\.class\)',s)
 if m:
  imp=re.search(r'import ([\w.]+\.'+m[1]+r');',s)
  if imp:names.append(imp[1])
 for m in re.finditer(r'@Mixin\(targets\s*=\s*"([^"]+)"\)',s):names.append(m[1])
with (r/'client-probe.log').open('w') as f:
 p=subprocess.run([str(jdk/'bin/java'),'--add-opens','java.base/java.lang.invoke=ALL-UNNAMED','-cp',str(r/'probe')+':'+cp,'ClientMixinProbe',*sorted(set(names))],cwd=r/'client-run',stdout=f,stderr=subprocess.STDOUT)
print('client probe exit',p.returncode)
