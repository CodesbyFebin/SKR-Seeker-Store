# Independently installed Seeker utilities

Nine application modules with unique application IDs. No Google login, analytics, Internet permission or shared wallet credentials. Apps share source code, not sandbox data or Keystore keys. `:app` is Seeker Store; its Discover tab links to installed utilities. `catalog.json` contains source metadata, not approved APK releases.

Build `./gradlew :utilities:notes:assembleDebug` (replace notes with any module). CI builds and lints all nine modules independently and uploads debug APK artifacts; it does not certify device behavior. No binaries were built in this workspace because the Gradle download failed. Do not describe these apps as fully validated.

| Module | Implemented source behavior | Missing / validation |
|---|---|---|
| notes | Private editable journal, encrypted save/load | Multi-note organization; device validation |
| tasks | Add, complete, delete, encrypted persistence | Notifications; device validation |
| calculator | Parentheses, unary signs, arithmetic precedence, zero-division rejection | Build/device validation |
| files | SAF folder listing, provider open, confirmed delete | Rename, move, APK repair; device validation |
| music | Local selected audio playback/pause | Playlists, background playback; device validation |
| gallery | Selected local photo, downsampled preview | Albums/editing; device validation |
| device | Actual model, ABI, battery capacity, RAM/free-storage reads | Per-app battery attribution, CPU thermals |
| backup | One-file encrypted portable export/restore, 10 MB limit | Full-device backup and key export are unsupported |
| vault | Device-credential-gated encrypted records | Autofill, audited password manager, recovery |

Storage uses app-scoped Android Keystore AES-GCM and atomic ciphertext writes. Vault requires a secure lock screen; keys require recent authentication (30 seconds). No guaranteed TEE/StrongBox, Seed Vault or remote attestation is claimed. On background vault UI locks. FLAG_SECURE applies to every utility. Portable backups use PBKDF2-HMAC-SHA256 (210,000 iterations), random salt/IV, authenticated AES-GCM; minimum 12-character passphrase. No Keystore or wallet key can be exported. Sensitive data is not sent to a network. Backups are user-selected file exports; restoring a vault ciphertext on another phone does not restore its Keystore key.

All implementations still require Android compilation, permission-denial, provider-failure, process-restart, rotation and device-security tests. Vault is not an audited replacement for a mature password manager.

## Requested apps still unimplemented

Native camera and capture proofs; offline maps/navigation and map packages; weather with timestamped cached forecasts; browser; fitness and step sensors; P2P messaging; offline translation and model bundles; QR/document scanning. These need real implementations before a catalog can advertise them. Weather cannot acquire new forecasts offline. P2P proximity chat does not replace an Internet messenger. Streaming video, social networks, ride booking, office collaboration and UPI/SolPay payments need independently scoped services, rights, trust and integration work. No rewards, mined ORE, SKR payments or productivity proof claims were added.
