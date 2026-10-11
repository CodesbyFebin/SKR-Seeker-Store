#!/usr/bin/env python3
"""Verify builder output and publish observed metadata, never invented publisher pins."""
import argparse,hashlib,json,os,re,subprocess
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('apk',type=Path);p.add_argument('--package',required=True);args=p.parse_args()
sdk=Path(os.environ.get('ANDROID_HOME') or os.environ['ANDROID_SDK_ROOT']);tools=sdk/'build-tools'/'36.0.0'
if not tools.exists():tools=sdk/'build-tools'/'34.0.0'
signature=subprocess.check_output([str(tools/'apksigner'),'verify','--verbose','--print-certs',str(args.apk)],text=True)
signers=re.findall(r'Signer #\d+ certificate SHA-256 digest: ([a-fA-F0-9]{64})',signature)
if len(signers)!=1:raise SystemExit('Exactly one current APK signing identity required')
aapt=tools/'aapt'
if not aapt.exists():aapt=sdk/'build-tools'/'34.0.0'/'aapt'
badging=subprocess.check_output([str(aapt),'dump','badging',str(args.apk)],text=True)
match=re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']*)'",badging)
if not match or match[1]!=args.package:raise SystemExit('APK package identity mismatch')
if "sdkVersion:'26'" not in badging or "targetSdkVersion:'34'" not in badging:raise SystemExit('Unexpected min/target SDK')
digest=hashlib.sha256()
with args.apk.open('rb') as stream:
 while block:=stream.read(1024*1024):digest.update(block)
metadata={'packageName':match[1],'versionCode':int(match[2]),'versionName':match[3],'assetName':args.apk.name,'sha256':digest.hexdigest(),'signerSha256':signers[0].lower(),'sourceCommit':os.environ.get('GITHUB_SHA'),'url':None,'publisherReview':'PENDING','deviceValidation':'NOT_RUN'}
args.apk.with_suffix('.metadata.json').write_text(json.dumps(metadata,indent=2)+'\n')
args.apk.with_suffix('.signature.txt').write_text(signature)
print('APK signature, identity, min26/target34 and observed metadata PASS:',args.package)
