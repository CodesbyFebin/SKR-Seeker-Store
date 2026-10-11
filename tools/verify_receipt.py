#!/usr/bin/env python3
"""Verify a receipt exported from My Endorsements. Requires cryptography."""
import argparse, base64, json
from pathlib import Path
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
parser = argparse.ArgumentParser()
parser.add_argument("receipt", type=Path)
args = parser.parse_args()
envelope = json.loads(args.receipt.read_text())
key = serialization.load_der_public_key(base64.b64decode(envelope["publicKeySpkiBase64"], validate=True))
assert isinstance(key, ec.EllipticCurvePublicKey) and isinstance(key.curve, ec.SECP256R1)
key.verify(base64.b64decode(envelope["signatureBase64"], validate=True), envelope["payloadUtf8"].encode("utf-8"), ec.ECDSA(hashes.SHA256()))
payload = json.loads(envelope["payloadUtf8"])
assert payload["schema"] == "skr-dstore-receipt/v1" and payload["chain"] == "solana:devnet"
assert payload["lamports"] == 1_000_000 and payload["staking"] == "MOCK_SOL_TRANSFER"
print("VALID local P-256 signature; independently verify transaction commitment:", payload["transaction"])
print("Unsigned observed status:", envelope.get("observedStatus", "unspecified"))
