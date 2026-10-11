# Remaining owner setup

The v0.2.1 APK is configured to use https://skr-dstore-catalog.modest-twin-72b.workers.dev/api/catalog. Live HTTP verification passed. It serves the ten demo listings; publisher APK release metadata has not been supplied. This is a temporary Cloudflare deployment: claim it through the privately provided owner link within 60 minutes, or Cloudflare deletes its resources. The claim URL is not stored in this source package. After claiming, authenticate Wrangler to your own account for subsequent deployments.

## Recipient

No recipient is invented. Provide the public address of an independently reviewed, existing devnet receiving account that you control, or a verified PDA with known program/derivation. The supplied prompt did not identify one. From the project root, run `python3 scripts/configure-demo.py --recipient YOUR_REVIEWED_DEVNET_ADDRESS`. Optionally set RPC_URL in your local environment for a Helius devnet endpoint; its key is not printed. The script validates Base58, devnet genesis and account existence, then writes ignored local.properties. Rebuild the APK afterward. No wallet signing is automated.

## Phone and model

No phone is attached to this workspace. On your own computer, install adb, connect and authorize the ARM64 phone, and run `adb devices -l`. Download the model from the official link in MODEL.md. Then run `python3 scripts/prepare-device.py --serial YOUR_DEVICE_SERIAL --model /path/to/qwen2.5-1.5b-instruct-q4_k_m.gguf`. This checks Android API/ABI and the pinned checksum, installs the APK, stages the model in Downloads and starts the app.

In Agent, tap Import verified GGUF and select that Downloads file. Wait for checksum verification. Ask a question, then turn on airplane mode and ask again. Measure model load/inference time and record three generated recommendations. Wallet connection/signing must be tested separately after recipient configuration with test SOL. Confirm the exact-message review, rejected signing, confirmed receipt, restart persistence and weight update. Return from a verified APK installation and check Open; cancel another installation and ensure no false success.

The script writes device-setup-report.json with only the checks it executed; import, inference, wallet and airplane-mode checks remain NOT_RUN until actually performed. Share the report and measured results for reconciliation. No screen capture is attempted: FLAG_SECURE remains enabled.

## Current model evidence

The official GGUF is available and checksum-verified on the build host. Actual JNI inference was rerun and returned EdgeORE, Grass and Helium Mobile with one-line reasons. This is host validation, not on-device import or performance evidence. Model weights are not included in the ZIP.
