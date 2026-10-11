#!/usr/bin/env node
import {readFile,writeFile} from 'node:fs/promises';
import {validateCatalog} from '../worker/src/catalog.js';
const [mappingPath, output='github-catalog.json']=process.argv.slice(2);
if(!mappingPath) throw Error('Usage: node indexer/github-releases.mjs reviewed-mappings.json output.json');
const mappings=JSON.parse(await readFile(mappingPath,'utf8'));
if(!Array.isArray(mappings) || mappings.length>10) throw Error('Expected at most ten reviewed repository mappings');
const apps=[];
for(const m of mappings) {
 if(!/^[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+$/.test(m.repo)) throw Error('Invalid owner/repository');
 const headers={Accept:'application/vnd.github+json','X-GitHub-Api-Version':'2022-11-28','User-Agent':'SKR-DSTORE-indexer'};
 if(process.env.GITHUB_TOKEN) headers.Authorization='Bearer '+process.env.GITHUB_TOKEN;
 const res=await fetch('https://api.github.com/repos/'+m.repo+'/releases/latest',{headers,redirect:'error',signal:AbortSignal.timeout(30000)});
 if(!res.ok) throw Error('GitHub release request failed: '+res.status);
 const release=await res.json(); if(release.draft || release.prerelease) throw Error('Stable published release required');
 const variants=release.assets.filter(a=>a.name.toLowerCase().endsWith('.apk')).map(a=>{
  // GitHub tag names cannot establish Android versionCode. Require reviewed asset identity.
  const pin=m.assets?.find(p=>p.name===a.name); if(!pin) return null;
  const hash=a.digest?.startsWith('sha256:')?a.digest.slice(7):pin.sha256;
  if(pin.sha256 && hash!==pin.sha256) throw Error('Reviewed hash differs from release digest');
  return {packageName:m.packageName,versionCode:pin.versionCode,url:a.browser_download_url,sha256:hash,signerSha256:m.signerSha256,assetName:a.name};
 }).filter(Boolean);
 if(!variants.length) throw Error('No reviewed standalone APK assets for '+m.repo);
 apps.push({id:m.id,sourceRepo:'https://github.com/'+m.repo,versionName:release.tag_name,changelog:release.body ?? '',variants});
}
const catalog=validateCatalog({schema:'skr-dstore-catalog/v1',apps});
await writeFile(output,JSON.stringify(catalog,null,2)+'\n'); console.log('Wrote '+catalog.apps.length+' reviewed GitHub releases; no secrets saved.');
