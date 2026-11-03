// Run from the repository root after pnpm runtime:bundle (POSIX host, about 46 seconds).
// Pauses only this script's worker; no real phone or model is used.
import {spawn} from 'node:child_process';
import {createInterface} from 'node:readline';
import {once} from 'node:events';
import {mkdtemp,writeFile,rm} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {resolve,join} from 'node:path';
const root=resolve('build/node-runtime-bundle');
const dir=await mkdtemp(join(tmpdir(),'tapilot-expiry-'));
await writeFile(join(dir,'task.yaml'),'cases:\n  - name: expiry probe\n    steps:\n      - home: {}\n');
const child=spawn(process.execPath,[join(root,'runner.mjs'),'prepare','--project-root',dir,'--result-dir',join(dir,'results'),'--config',join(root,'android.config.mjs'),'--project','android-accessibility'],{stdio:['pipe','pipe','pipe']});
const exit=once(child,'exit');
const watchdog=setTimeout(()=>{child.kill('SIGCONT');child.kill('SIGKILL');},65000);
const events=[]; let timer;
child.stderr.on('data',b=>process.stderr.write(b));child.stdin.on('error',()=>{});
const reader=createInterface({input:child.stdout});
reader.on('line',line=>{
 if(!line.startsWith('MIDSCENE_EVENT '))return;
 const e=JSON.parse(line.slice(15));events.push(e.type);console.log(e.type);
 if(e.type==='runtime.prepared'){
  child.kill('SIGSTOP');console.log('Suspended ready worker across its 45s activation deadline');
  timer=setTimeout(()=>{console.log('aliveBeforeActivation='+ (child.exitCode===null));child.stdin.write('{"type":"activate"}\n');child.kill('SIGCONT');},46000);
 }
 if(e.type==='rpc.request') child.stdin.write(JSON.stringify({requestId:e.requestId,ok:true,result:e.method==='device.getCapabilities'?{protocolVersion:1,canCapture:true,canPerformGestures:true,canInputText:true}:null})+'\n');
});
try{
 const [code,signal]=await exit;console.log(JSON.stringify({exitCode:code,signal,events,taskStarted:events.includes('test.started'),completed:events.includes('test.completed')}));
}finally{clearTimeout(watchdog);clearTimeout(timer);child.kill('SIGCONT');child.kill('SIGKILL');reader.close();await rm(dir,{recursive:true,force:true});}
