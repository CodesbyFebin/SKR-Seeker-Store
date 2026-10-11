import { seedApps } from './seed.js';
import { validateCatalog } from './catalog.js';
const headers = {'Content-Type': 'application/json; charset=utf-8', 'X-Content-Type-Options': 'nosniff', 'Access-Control-Allow-Origin': '*'};
const json = (value, status = 200, extra = {}) => new Response(JSON.stringify(value), {status, headers: {...headers, ...extra}});
export default {
  async fetch(request, env) {
    const path = new URL(request.url).pathname;
    if (request.method === 'OPTIONS') return new Response(null, {status: 204, headers: {'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET, HEAD, OPTIONS'}});
    if (!['GET','HEAD'].includes(request.method)) return json({error: 'Read-only catalog. Endorsements remain confirmed local devnet receipts; arbitrary client scores are never accepted.'}, 405, {Allow: 'GET, HEAD, OPTIONS'});
    try {
      if (path === '/health') return json({status: 'ok', chain: 'solana:devnet', service: 'catalog', databaseConfigured: !!env.DB});
      let apps = seedApps;
      if (env.DB) {
        const result = await env.DB.prepare('SELECT payload_json FROM catalog_apps ORDER BY id LIMIT 100').all();
        if (!result.success) throw new Error('Database query failed');
        apps = result.results.map(row => JSON.parse(row.payload_json));
      }
      const catalog = validateCatalog({schema: 'skr-dstore-catalog/v1', apps});
      if (path === '/') {
        const escape = value => String(value).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
        const cards = catalog.apps.map(app => `<article><h2>${escape(app.name)}</h2><p>${escape(app.category)}</p></article>`).join('');
        const html = `<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Seeker Store</title><style>body{margin:0;background:#09090d;color:#f5f5f7;font:16px system-ui}main{max-width:800px;margin:auto;padding:32px 20px}h1{font-size:36px}a{color:#b68aff}section{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px}article{background:#1c1c24;border-radius:12px;padding:16px}h2{font-size:20px}p{color:#b6b6c5;line-height:1.6}</style><main><h1>Seeker Store</h1><p>The discovery and curation layer for Solana mobile apps.</p><p>Devnet demo · Ten curated demo listings. SKR weights, Guardian badges and device attestation are mock signals. Wallet endorsements and local AI run in the Android app.</p><p><a href="/api/catalog">Catalog JSON</a> · <a href="/api/catalog/depin">DePIN catalog</a> · <a href="/health">Service health</a></p><section>${cards}</section></main></html>`;
        return new Response(request.method === 'HEAD' ? null : html, {headers: {'Content-Type':'text/html; charset=utf-8','X-Content-Type-Options':'nosniff','Content-Security-Policy':"default-src 'none'; style-src 'unsafe-inline'; frame-ancestors 'none'",'Cache-Control':'public, max-age=60'}});
      }
      let body;
      if (path === '/api/catalog') body = catalog;
      else if (path === '/api/catalog/depin') body = {...catalog, apps: catalog.apps.filter(a => a.category === 'DePIN').sort((a,b) => Number(b.id==='edgeore') - Number(a.id==='edgeore'))};
      else if (path.startsWith('/api/apps/')) {
        body = catalog.apps.find(a => a.id === decodeURIComponent(path.slice('/api/apps/'.length)));
        if (!body) return json({error: 'App not found'}, 404);
      } else return json({error: 'Route not found'}, 404);
      const response = json(body, 200, {'Cache-Control': 'public, max-age=300'});
      return request.method === 'HEAD' ? new Response(null, {status: response.status, headers: response.headers}) : response;
    } catch {
      return json({error: 'Catalog unavailable or invalid. No unvalidated APK metadata served.'}, 503, {'Cache-Control':'no-store'});
    }
  }
};
