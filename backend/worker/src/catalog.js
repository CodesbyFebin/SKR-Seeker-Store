import { seedApps } from './seed.js';
const known = new Map(seedApps.map(a => [a.id, a]));
export function validateCatalog(input) {
  if (!input || input.schema !== 'skr-dstore-catalog/v1' || !Array.isArray(input.apps) || input.apps.length > 100) throw new Error('Invalid catalog envelope');
  const seen = new Set();
  const apps = input.apps.map(a => {
    const baseline = known.get(a?.id);
    if (!baseline || seen.has(a.id)) throw new Error('Unknown or duplicate app ID');
    seen.add(a.id);
    let apk = null;
    if (a.apk != null) {
      const r = a.apk;
      const url = new URL(r.url);
      if (url.protocol !== 'https:' || url.username || url.password || url.hash || !url.hostname) throw new Error('APK URL must be HTTPS');
      if (!/^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$/.test(r.packageName) || !Number.isSafeInteger(r.versionCode) || r.versionCode <= 0) throw new Error('Invalid APK identity');
      if (!/^[a-fA-F0-9]{64}$/.test(r.sha256) || !/^[a-fA-F0-9]{64}$/.test(r.signerSha256)) throw new Error('Both APK and signer certificate SHA-256 are required');
      if(!String(r.assetName ?? 'release.apk').toLowerCase().endsWith('.apk')) throw new Error('Standalone APK required');
      apk = { packageName: r.packageName, versionCode: r.versionCode, url: url.href, sha256: r.sha256.toLowerCase(), signerSha256: r.signerSha256.toLowerCase(), assetName: String(r.assetName ?? 'release.apk') };
    }
    const sourceRepo=a.sourceRepo ?? '';
    if(sourceRepo && !/^https:\/\/github\.com\/[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+$/.test(sourceRepo)) throw new Error('Invalid source repository');
    const out={...baseline, apk, sourceRepo, versionName:String(a.versionName ?? '').slice(0,80), changelog:String(a.changelog ?? '').slice(0,3000)};
    if(a.variants != null) {
      if(!Array.isArray(a.variants) || !a.variants.length || a.variants.length>20) throw new Error('Invalid APK variants');
      out.variants=a.variants.map(r=>validateCatalog({schema:'skr-dstore-catalog/v1',apps:[{id:a.id,apk:r}]}).apps[0].apk);
      if(new Set(out.variants.map(r=>r.packageName+':'+r.versionCode+':'+r.signerSha256)).size!==1) throw new Error('Variant identities differ');
    }
    return out;
  });
  return {schema: 'skr-dstore-catalog/v1', apps};
}
