# Free endpoint configuration

No paid API or API key is required for the default app. Official reference: https://solana.com/docs/references/clusters and https://solana.com/developers.

| Endpoint | Use in Seeker Store |
|---|---|
| https://api.devnet.solana.com | Default active RPC; wallet balance, genesis verification, blockhash, submitted transaction and receipt confirmation |
| https://api.testnet.solana.com | Documented public endpoint; not used by the devnet wallet flow |
| https://api.mainnet.solana.com | Documented public endpoint; not used by the devnet wallet flow |
| https://explorer.solana.com/tx/SIGNATURE?cluster=devnet | Inspect an actual confirmed demo transaction |
| https://faucet.solana.com | Manual testing faucet; availability/limits apply |

The app uses getGenesisHash, getBalance, getLatestBlockhash, sendTransaction, getSignatureStatuses and getTransaction through OkHttp JSON-RPC. It rejects any configured RPC whose genesis hash is not devnet. Never fail over a reviewed devnet transaction to mainnet or testnet. No automatic signing, endpoint rotation, transaction resend or automatic faucet requests.

Public endpoints are rate-limited and do not provide a production service guarantee. HTTP failures are shown as errors; a failed response never becomes a confirmed endorsement. Local AI needs no network endpoint. The catalog uses the configured read-only Worker or ten local demo listings; Solana RPC is not an app-directory API. Publisher APK digests and signing pins still require independent review.

Helius is optional, not required. Free provider plans needing credentials must be configured by the owner through ignored local.properties; no secrets are included. Adding unrelated APIs from the developer hub would not implement additional app functionality.
