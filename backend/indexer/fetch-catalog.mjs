#!/usr/bin/env node
import { readFile, writeFile } from 'node:fs/promises';
import { validateCatalog } from '../worker/src/catalog.js';
// No guessed SeekerTracker endpoint. Supply a reviewed export URL or local JSON.
const [source, output = 'catalog.json', sqlFile = 'catalog.seed.sql'] = process.argv.slice(2);
if (!source) throw new Error('Usage: node indexer/fetch-catalog.mjs INPUT.json|https://REVIEWED_EXPORT OUTPUT.json OUTPUT.sql');
let text;
if (source.startsWith('https://')) {
  const response = await fetch(source, {redirect:'error', signal:AbortSignal.timeout(30000)});
  if (!response.ok || Number(response.headers.get('content-length')) > 2000000) throw new Error('Catalog download failed or too large');
  const chunks=[]; let size=0;
  for await (const chunk of response.body) { size+=chunk.length; if(size>2000000) throw new Error('Catalog too large'); chunks.push(chunk); }
  text=Buffer.concat(chunks).toString('utf8');
} else {
  text=await readFile(source,'utf8'); if(Buffer.byteLength(text)>2000000) throw new Error('Catalog too large');
}
const catalog = validateCatalog(JSON.parse(text));
const quote = s => "'" + s.replaceAll("'", "''") + "'";
const sql = ['-- Generated from validated catalog; inspect before remote import.', 'BEGIN TRANSACTION;', 'DELETE FROM catalog_apps;', ...catalog.apps.map(a => `INSERT INTO catalog_apps(id,payload_json,updated_at) VALUES(${quote(a.id)},${quote(JSON.stringify(a))},${Date.now()});`), 'COMMIT;'].join('\n')+'\n';
await writeFile(output, JSON.stringify(catalog,null,2)+'\n'); await writeFile(sqlFile,sql);
console.log(`Validated ${catalog.apps.length} apps; ${catalog.apps.filter(a=>a.apk).length} APK releases. Wrote ${output} and ${sqlFile}.`);
