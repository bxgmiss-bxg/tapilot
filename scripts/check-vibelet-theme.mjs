import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
import {execFileSync} from 'node:child_process';
import {runInNewContext} from 'node:vm';
import ts from 'typescript';
// Evaluate the upstream theme in memory, including its default accent transform.
// --github reads individual files through the API; it never clones or pulls Studio.
const root=fileURLToPath(new URL('../',import.meta.url));
const remote=process.argv[2]==='--github';
const source=resolve(process.argv[2]??resolve(root,'../studio/packages/mobile-theme/src'));
const revision=remote ? execFileSync('gh',['api','repos/quanru/studio/commits/HEAD','--jq','.sha'],{encoding:'utf8'}).trim() : null;
const brand=JSON.parse(readFileSync(resolve(root,'theme/brand.json'),'utf8'));
const cache=new Map();
function load(name) {
 assert(/^[a-zA-Z]+$/.test(name), `Unexpected theme module: ${name}`);
 if(cache.has(name)) return cache.get(name).exports;
 const module={exports:{}};
 cache.set(name,module);
 const text=remote
  ? Buffer.from(execFileSync('gh',['api',`repos/quanru/studio/contents/packages/mobile-theme/src/${name}.ts?ref=${revision}`,'--jq','.content'],{encoding:'utf8'}),'base64').toString('utf8')
  : readFileSync(resolve(source,`${name}.ts`),'utf8');
 const code=ts.transpileModule(text,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText;
 runInNewContext(code,{module,exports:module.exports,require:id=>{
  assert(id.startsWith('./'), `Unexpected theme import: ${id}`);
  return load(id.slice(2));
 }});
 return module.exports;
}
const {getThemeColors}=load('palettes');
const normalize=value=>{
 if(value.startsWith('#'))return value.toUpperCase();
 const rgba=value.match(/^rgba\((\d+), (\d+), (\d+), ([\d.]+)\)$/);
 assert(rgba,`Unsupported source color ${value}`);
 return '#'+rgba.slice(1,4).map(Number).concat(Math.round(Number(rgba[4])*255)).map(n=>n.toString(16).padStart(2,'0')).join('').toUpperCase();
};
const mapping={background:'bg',surface:'card',text:'text',muted:'textMuted',line:'border',primary:'accent',primaryDark:'accentSecondary',soft:'accentSoft',success:'success',danger:'danger',bubbleStart:'userBubble',bubbleMiddle:'userBubble',bubbleEnd:'userBubble',input:'input',header:'headerBg',secondaryText:'textSecondary',contrast:'accentContrast',assistantBubble:'assistantBubble',cardAlt:'cardAlt',strongLine:'borderStrong',codeBackground:'codeBg',codeText:'codeText',softStrong:'accentSoftStrong',softAlt:'accentSoftAlt'};
let count=0;
for(const mode of ['Light','Dark']){
 const name='minimal'+mode;
 const colors=getThemeColors('minimal',mode.toLowerCase());
 for(const [key,sourceKey] of Object.entries(mapping)){
  assert.equal(brand[name][key],normalize(colors[sourceKey]),`${name}.${key}`);count++;
 }
 assert.equal(brand[name].bubbleText,normalize(colors.userBubbleText??colors.accentContrast),`${name}.bubbleText`);count++;
}
console.log(`Vibelet Minimal alignment passed: ${count} colors (${revision??source}).`);
