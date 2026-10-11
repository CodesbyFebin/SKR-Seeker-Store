# 90-second device walkthrough

Record only after the device acceptance flow passes. No actual recording is included. Film with an external camera because FLAG_SECURE blocks screen recording. Use a devnet-only demo wallet without private information visible.

| Time | Action | Voiceover |
|---|---|---|
| 0–12s | Open Discover; show catalog and demo status chips | “Seeker Store adds a discovery and endorsement layer over the Solana dApp Store. This build uses ten example apps and clearly labels its demo trust signals.” |
| 12–25s | Tap DePIN; show EdgeORE first, then Grass / Helium / Hivemapper | “The DePIN view brings contribution types together. EdgeORE is featured here; the two-times synergy display describes a planned SKR reward mechanism.” |
| 25–40s | Tap EdgeORE Endorse; connect MWA on devnet | “Endorsement currently uses a real wallet flow for a 0.001 devnet SOL transfer. It is a demonstration of the interaction, not real SKR staking.” |
| 40–55s | Show review destination, amount and message hash; approve in wallet | “The exact message is frozen before signing. A changed account or message is rejected. Only a confirmed transaction changes the demo ranking.” |
| 55–67s | Show confirmed Endorsements row and export receipt | “Room stores the encrypted record. Android Keystore signs the receipt, which can be independently verified after export.” |
| 67–75s | Turn on airplane mode using device controls; return to Agent | “Now the network is off. The imported Qwen model runs locally through llama.cpp.” |
| 75–88s | Ask ‘What should I endorse?’; show three generated recommendations | “The recommender reads this catalog and local endorsement profile. No cloud AI call is involved.” |
| 88–90s | Hold final Agent view | “Discover, review, endorse, and keep the proof.” |

Model speed must be measured first: if it takes longer than the allocated shot, shorten other shots or use clearly labeled cuts. Never substitute canned output while describing it as model inference. Keep Genesis, TEEPIN, SKR weights and reward displays visibly marked demo / planned. Don’t publish a ‘confirmed’ row unless RPC actually confirms it.

Before filming: configure recipient and devnet RPC, install a compatible wallet, fund test SOL, import the checksum-pinned Qwen GGUF, replace EdgeORE's placeholder package ID, and perform one rehearsal. A separate editor may combine the real footage and recorded voiceover after capture; no video-generation service is needed.
