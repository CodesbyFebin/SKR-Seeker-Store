# Project instructions

Use Kotlin, Compose, OkHttp and the pinned Android toolchain. Preserve four tabs and explicit devnet / mock labels. Do not introduce real SKR staking, a new on-chain program, WebView, cloud inference, analytics or silent APK installation.

Run `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` and `node --test backend/test/*.test.mjs` for relevant changes. Keep exact-message signing review, confirmed transaction / receipt binding, SQLCipher, P-256 receipts and FLAG_SECURE. Never turn a signing callback into confirmed UI state.

Do not invent catalog endpoints or APK metadata. Release manifests require independently reviewed APK hashes and signer identities. Backend API stays read-only unless a future implementation verifies on-chain application attribution and replay protection.

Never commit local.properties, keys, keystores, wallet tokens, GGUF files, node_modules or machine paths. Report host, Android device and Cloudflare deployment evidence separately.
