import test from 'node:test';
import assert from 'node:assert/strict';
import {lanCandidates,findPairedPhone} from '../server/lan-discovery.mjs';
const address = (ip, netmask='255.255.255.0') => ({address:ip,netmask,family:'IPv4',internal:false});
test('fallback discovery stays on physical subnets and excludes self, network and broadcast', () => {
 const candidates=lanCandidates({en0:[address('192.168.31.114','255.255.255.252')],utun1:[address('10.0.0.1')],lo0:[{...address('127.0.0.1'),internal:true}]});
 assert.deepEqual(candidates,[{host:'192.168.31.113',localAddress:'192.168.31.114'}]);
 assert.equal(lanCandidates({en0:[address('192.168.31.114','255.255.0.0')]}).length,253);
});
test('only matching identity is returned; bounded concurrent probes receive no pairing token', async () => {
 const fingerprint='a'.repeat(64); let active=0,max=0;
 const candidates=Array.from({length:40},(_,i)=>({host:`192.168.1.${i+1}`,localAddress:'192.168.1.254'}));
 const result=await findPairedPhone({fingerprint,candidates,probe:async (candidate,identity,port)=>{
  assert.equal(identity,fingerprint);assert.equal(port,39877);assert.equal('token' in candidate,false);
  max=Math.max(max,++active); await new Promise(resolve=>setTimeout(resolve,1));active--;
  return candidate.host==='192.168.1.17';
 }});
 assert.equal(result[0].host,'192.168.1.17');assert.ok(max<=16);
 assert.deepEqual(await findPairedPhone({fingerprint:'invalid',probe:()=>{throw Error('must not scan');}}),[]);
});
