# Cloudflare catalog backend

Read-only release catalog for the ten Android demo listings. No API keys, developer credentials, APK URLs or public write endpoint are shipped. The seed entries all have `apk: null`; installation is unavailable until an operator supplies reviewed release metadata. No SeekerTracker endpoint or schema is assumed.

## Run and deploy

Use Node 22+ and the pinned Wrangler version. From `backend/`:

```sh
npm install
npm test
npm run dev
# After selecting your Cloudflare account:
npx wrangler login
npm run deploy
```

The Worker serves the ten seed apps without D1. The resulting HTTPS Worker URL plus `/api/catalog` goes in Android `CATALOG_URL` in `local.properties`; rebuild the APK afterward. The v0.2.1 APK uses https://skr-dstore-catalog.modest-twin-72b.workers.dev/api/catalog and has no endorsement recipient. Claim the temporary deployment within 60 minutes to retain it.

For persistent reviewed releases, run `npx wrangler d1 create skr-dstore`, add the returned DB binding to `worker/wrangler.toml`, and apply `worker/schema.sql`. Edit `catalog.example.json`, then validate and generate imports:

```sh
node indexer/fetch-catalog.mjs catalog.example.json catalog.json worker/catalog.seed.sql
npx wrangler d1 execute skr-dstore --config worker/wrangler.toml --remote --file worker/schema.sql
npx wrangler d1 execute skr-dstore --config worker/wrangler.toml --remote --file worker/catalog.seed.sql
```

The generated seed atomically replaces the catalog. Inspect it before importing. A reviewed HTTPS JSON export can replace the local input file; unapproved redirects, malformed manifests and oversized responses are rejected. The indexer does not scrape an undocumented service.

## Release format

For a known app ID, replace `apk: null` with:

```json
{
  "packageName": "actual.publisher.package",
  "versionCode": 1,
  "url": "https://actual-publisher.example/release.apk",
  "sha256": "64_HEX_CHARACTERS_FROM_THE_ACTUAL_APK",
  "signerSha256": "64_HEX_CHARACTERS_FROM_THE_PUBLISHER_CERTIFICATE"
}
```

The example above is a format guide, not valid release metadata. Determine the exact package and version with `aapt dump badging`, compute file SHA-256, and obtain the signing certificate digest with `apksigner verify --print-certs`. Independently confirm the publisher's certificate. Publish only an authorized, standalone APK whose URLs, integrity and identity have been reviewed. No demo app's download URL is guessed.

The Android app re-fetches metadata before downloading, then requires the reviewed release to be unchanged. It rejects HTTP, unapproved redirects, files over 500 MB, checksum/identity/signer mismatches and downgrades. Existing installed signer identity must match. Android's system installer handles the final user confirmation. Installation cancellation is never recorded as success.

## API and trust

| Route | Result |
|---|---|
| GET /health | Service / D1 configuration status |
| GET /api/catalog | Validated envelope with ten demo IDs |
| GET /api/catalog/depin | DePIN entries, EdgeORE first |
| GET /api/apps/:id | One known app |
| POST or other mutation methods | 405; read-only |

The catalog operator is trusted for release identities. This version does not independently verify release NFTs or support arbitrary full-store ingestion. D1 is edited via account-authenticated administrative commands; the public Worker cannot update it. Client endorsement claims are not indexed: the current transfer has no on-chain app ID, so accepting client-provided scores would allow fabricated or misassigned global weights. Confirmed receipts and demo ranking remain local.

Tests exercise the actual Worker handler and validator in Node with a D1 stub, plus SQLite schema/seed separately. Wrangler bundling, real local D1 schema/seed import and actual Worker HTTP catalog responses also passed; local Node interface enumeration used a loopback shim for sandbox compatibility. A temporary remote Worker deployment also passed HTTPS checks; see evidence/live-catalog-test.txt. It must be claimed. D1 and publisher APK metadata remain unconfigured.

Sources: https://developers.cloudflare.com/d1/worker-api/ and https://developers.cloudflare.com/d1/get-started/

## Reviewed GitHub release import

Use `node indexer/github-releases.mjs github-mappings.json github-catalog.json`, then pass that generated file through `fetch-catalog.mjs` to create the D1 import SQL. The shipped mapping example is empty; no repository or signer identity is invented. Each mapping supplies a known `id`, `repo` (`owner/repository`), actual `packageName`, publisher `signerSha256` and reviewed `assets` entries (`name`, Android `versionCode`, optional independently reviewed `sha256`). If GitHub supplies an asset SHA-256 digest, a supplied pin must match it; otherwise the independent pin is required. Only reviewed standalone APK assets are imported. An optional GITHUB_TOKEN can be supplied via the environment and is never written to output.

Catalog entries may carry `sourceRepo`, `versionName`, `changelog` and `variants` (up to 20 fully pinned APK objects). All variants must share package, versionCode and signer identity. Unknown download filenames require actual archive ABI inspection on the phone. Source links are GitHub HTTPS URLs and do not constitute publisher ownership verification.

The importer passed syntax checks; live GitHub ingestion was not run because publisher repository/asset mappings were not supplied. Unit tests validate variants, provenance and rejection of inconsistent identities. The v0.2 Worker passed Wrangler’s bundle dry run; the v0.2 local HTTP/D1 test is identified in evidence and is not presented as a deployed backend.

## Current owner setup

See OWNER-SETUP.md for the live catalog, recipient validation, device installation and model import steps. No phone was available for device validation.
