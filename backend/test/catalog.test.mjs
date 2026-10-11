import test from 'node:test';
import assert from 'node:assert/strict';
import worker from '../worker/src/index.js';
import {validateCatalog} from '../worker/src/catalog.js';
const req=(p, method='GET')=>new Request('https://catalog.example'+p,{method});
const release={packageName:'com.example.edgeore',versionCode:1,url:'https://publisher.example/edgeore.apk',sha256:'a'.repeat(64),signerSha256:'b'.repeat(64)};
test('fallback serves ten honest listings without invented APK URLs',async()=>{const r=await worker.fetch(req('/api/catalog'),{});assert.equal(r.status,200);const c=await r.json();assert.equal(c.apps.length,10);assert.ok(c.apps.every(a=>a.apk===null));});
test('DePIN has EdgeORE first',async()=>{const c=await (await worker.fetch(req('/api/catalog/depin'),{})).json();assert.equal(c.apps[0].id,'edgeore');assert.ok(c.apps.every(a=>a.category==='DePIN'));});
test('verified manifest preserves release metadata',()=>{const c=validateCatalog({schema:'skr-dstore-catalog/v1',apps:[{id:'edgeore',apk:release}]});assert.equal(c.apps[0].apk.sha256,release.sha256);});
test('reject HTTP, missing signer, duplicate app, unsafe version',()=>{for(const bad of [{...release,url:'http://publisher.example/a.apk'},{...release,signerSha256:''},{...release,versionCode:-1}]) assert.throws(()=>validateCatalog({schema:'skr-dstore-catalog/v1',apps:[{id:'edgeore',apk:bad}]}));assert.throws(()=>validateCatalog({schema:'skr-dstore-catalog/v1',apps:[{id:'edgeore'},{id:'edgeore'}]}));});
test('public clients cannot write fabricated endorsements',async()=>{const r=await worker.fetch(req('/api/endorse','POST'),{});assert.equal(r.status,405);});
test('D1 query failure is unavailable, never fabricated success',async()=>{const r=await worker.fetch(req('/api/catalog'),{DB:{prepare(){return {async all(){throw Error('offline');}};}}});assert.equal(r.status,503);});
test('D1 release is validated before response',async()=>{const env={DB:{prepare(){return {async all(){return {success:true,results:[{payload_json:JSON.stringify({id:'edgeore',apk:release})}]};}};}}};const c=await (await worker.fetch(req('/api/catalog'),env)).json();assert.equal(c.apps[0].apk.packageName,'com.example.edgeore');});

test('variants preserve provenance and require consistent package identity',()=>{const c=validateCatalog({schema:'skr-dstore-catalog/v1',apps:[{id:'edgeore',sourceRepo:'https://github.com/owner/repo',variants:[{...release,assetName:'app-arm64.apk'},{...release,assetName:'app-universal.apk'}]}]});assert.equal(c.apps[0].variants.length,2);assert.equal(c.apps[0].sourceRepo,'https://github.com/owner/repo');assert.throws(()=>validateCatalog({schema:'skr-dstore-catalog/v1',apps:[{id:'edgeore',variants:[release,{...release,versionCode:2}]}]}));});
test('source links cannot be executable URLs',()=>{assert.throws(()=>validateCatalog({schema:'skr-dstore-catalog/v1',apps:[{id:'edgeore',sourceRepo:'javascript:alert(1)'}]}));});

test('Seeker Store root supports HTML GET and empty HEAD',async()=>{const r=await worker.fetch(req('/'),{});assert.equal(r.status,200);assert.match(r.headers.get('content-type'),/text\/html/);const html=await r.text();assert.ok(html.includes('Seeker Store'));assert.ok(html.includes('EdgeORE'));assert.equal(await (await worker.fetch(req('/','HEAD'),{})).text(),'');});
