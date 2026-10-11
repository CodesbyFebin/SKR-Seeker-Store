# Seeker utility suite

18 separate Android application IDs; no shared sandbox data or wallet credentials. `:app` is the store, linking to installed utilities in Discover. All are Kotlin/Compose, min API 26, target 34, JDK 17. No analytics or Google login. Internet permissions exist only where the feature needs a network. No Android WebView: Browser embeds the Mozilla Gecko engine.

## What is implemented

| Module | Working source paths | Important limits |
|---|---|---|
| notes | Edit and encrypted save/load journal | Single document; no multi-note folders |
| vault | Device-credential-gated encrypted records | Not audited; no autofill, recovery or Seed Vault integration |
| calculator | Arithmetic parser with precedence/unary/parentheses | No scientific functions |
| files | SAF folder listing, open, confirmed delete | Granted folder only; no silent deletion or cross-app repair |
| music | Selected local audio play/pause | Foreground only, no playlists |
| gallery | Local image selection/downsampled preview | No albums/editor |
| tasks | Add, complete, delete; encrypted persistence | No notifications or on-chain work claims |
| backup | One-file passphrase-encrypted export/restore | 10 MB limit; no system/Keystore/seed export |
| device | Actual device/ABI/battery/RAM/storage reads | No per-app battery or CPU thermal attribution |
| appmanager | Visible installed launcher apps, search/open/system settings | No repair of another store or broad package inventory |
| scanner | Offline photo QR/barcode decode | No live camera scanning, OCR or document PDF capture |
| camera | Native CameraX preview, gallery capture, actual file SHA-256 | No on-chain proof or authenticity guarantee |
| weather | Open-Meteo coordinates lookup, encrypted timestamped cache | Refresh needs Internet; cached values can be stale |
| fitness | Actual step-counter foreground sessions | Sensor/permission needed; no background tracking, rewards or medical inference |
| messaging | Manual same-LAN host/connect, passphrase-AES-GCM messages | No phone number/cloud; not BLE/Wi-Fi Direct discovery; foreground, unreviewed protocol |
| translation | Imported checksum-pinned Qwen, real JNI offline translation | ARM64 model required separately; accuracy and phone inference unverified |
| maps | Download small OSM road region; encrypted cache, native canvas, Dijkstra cached path | Approximate walking path only; no GPS guidance/road access/turn restrictions or safe navigation guarantee |
| browser | Mozilla GeckoView private browsing, HTTPS address entry, back/forward/reload | No download manager, bookmarks, permission prompts or audited tracker guarantee |

## Verification and delivery

First-wave commit `95dff53` passed store build, backend job and nine utility build/lint/test jobs. It predates scanner/appmanager and later utility additions. Current complete suite must pass its own CI; success on an earlier commit does not qualify later changes. No phone/emulator validation was performed. Catalog statuses stay SOURCE_IMPLEMENTED_BUILD_PENDING until exact current binaries qualify. No download URLs, APK digests or signer identities are fabricated. GitHub Actions builds every utility and uploads APK artifacts when successful. Build one with `./gradlew :utilities:notes:testDebugUnitTest :utilities:notes:lintDebug :utilities:notes:assembleDebug`.

Store installation still needs reviewed hosted APK metadata and a stable owner-controlled signing identity. The Worker/root-page source is present; existing live Worker update needs owner credentials. Devnet endorsements need an owner-reviewed recipient; Genesis/TEEPIN/Guardian/SKR weights remain explicit mock signals. No utility reward, mined ORE or SKR payment implementation was added.

## Security and dependencies

Notes/tasks/vault/cache use app-scoped Android Keystore AES-GCM and atomic ciphertext writes. Vault requires a secure screen lock and recent authentication (30 seconds), locking UI on background. No guaranteed TEE/StrongBox, Seed Vault or attestation claim. FLAG_SECURE applies throughout. Portable backup uses PBKDF2-HMAC-SHA256 (210,000 iterations), random salt/IV and authenticated AES-GCM; minimum 12-character passphrase. Lost passphrases cannot be recovered. Device keys and wallet seeds are never exported. Selected media are user-owned files; capture saves ordinary photos to the gallery.

Nearby chat uses that portable encryption with per-message salts/IVs. It is experimental, not an audited secure messenger; no identity/SAS/replay protocol or persistent history. Use a trusted LAN and an in-person strong shared passphrase, not wallet credentials. Browser's private mode does not make you anonymous or prevent website tracking. Qwen inference has no cloud calls. Imported model is the official Qwen2.5 1.5B Q4_K_M GGUF with SHA-256 6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e.

Browser pins Mozilla GeckoView 157.0.20261005135250 from Mozilla Maven; SDK licenses/updates matter independently of our Kotlin source (Gecko MPL-2.0). QR decoding uses ZXing core 3.5.3 (Apache-2.0). CameraX is Apache-2.0. No Google Play Services requirement. Map data: © OpenStreetMap contributors, ODbL; https://www.openstreetmap.org/copyright. Overpass endpoint is community-operated, not unlimited; only explicit small-region downloads. Weather: Open-Meteo attribution/CC BY 4.0; free API terms are non-commercial and rate-limited: https://open-meteo.com/en/terms. Neither weather nor maps silently acquire new data while offline.

Consumer-service alternatives (social networks, DRM streaming, ride booking, UPI/SolPay and collaborative office services) are not part of these 18 utility implementations. They require separate complete products, integrations and operating services; none is advertised as delivered.

Browser is an independent Gradle project (utilities/browser/gradlew), using AGP 9.1.1, built-in Kotlin with KGP/Compose 2.4.21, Gradle 9.3.1 and compile SDK 37 for current Gecko/media dependencies. JDK 17, min 26 and target SDK 34 remain. The store and other utilities retain their pinned older toolchain and compile/target 34.

Successful CI artifacts include the APK, verified signature report and observed package/version/digest/signer metadata. Those observed hashes are not APK hosting URLs, release approval or device validation. Stable owner-controlled release signing and reviewed hosted metadata are still required for distribution through the store.
