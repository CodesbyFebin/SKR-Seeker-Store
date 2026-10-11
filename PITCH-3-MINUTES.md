# Three-minute pitch

Use live device claims only after the device acceptance checks pass. Show the mock labels. Store counts, missing current categories, team size and SKR roadmap dates from the supplied notes need independent verification before using them publicly.

**0:00–0:35 — Problem.** “Mobile app discovery needs useful signals about what an app contributes and why someone endorses it. Our project explores SKR-inspired curation with a DePIN discovery view, clear contribution types and reviewable endorsement receipts. We complement the existing Solana dApp Store and publisher ecosystem.”

**0:35–1:05 — Product.** “Seeker Store is a native Kotlin and Compose app with four tabs: discovery, DePIN, endorsements and a local recommender. The MVP uses ten example apps. Rankings and trust badges are clearly labeled demo signals. Genesis and TEEPIN checks are future integrations, not identity claims we make today.”

**1:05–1:45 — Endorsement demonstration.** “EdgeORE is our featured DePIN example. Endorse opens Mobile Wallet Adapter on devnet. We review the exact amount, destination, message bytes and hash before asking the wallet to sign. This is a 0.001 SOL transfer demonstrating the interaction, not SKR staking. Returned bytes must match the reviewed message, and only RPC-confirmed transfers add local demo weight. Room encrypts the receipt; Android Keystore signs an exportable proof.”

**1:45–2:20 — Offline AI.** “The Agent uses Qwen2.5 1.5B through llama.cpp JNI on CPU, one thread, with a 2048-token context. It reads the ten apps and local endorsement profile, then generates three concise recommendations. There is no cloud AI call. We have verified actual host inference; show airplane-mode phone inference only after measuring it on the device.”

**2:20–2:45 — Standalone extension.** “We added a read-only Cloudflare release catalog and an Android install/update handoff. Downloads require reviewed package identity, version, APK hash and signer identity. The app verifies those properties before Android asks the user to confirm. We do not claim to have mirrored the full store or verified release NFTs.”

**2:45–3:00 — Close.** “The compiled MVP brings discovery, reviewed wallet interactions and offline recommendations into one mobile flow. The next steps are phone validation, genuine SKR integration and verified mobile identity. Our source, APK, test evidence and honest real-versus-mock table make that progress reviewable.”
