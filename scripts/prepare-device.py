#!/usr/bin/env python3
"""Install the APK and stage the verified public GGUF on an explicitly selected phone.
Model import and wallet approvals remain explicit Android UI actions.
"""
import argparse,hashlib,subprocess,shutil,json,datetime
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--serial',required=True);p.add_argument('--model',required=True,type=Path);p.add_argument('--adb',default='adb');p.add_argument('--apk',required=True,type=Path);args=p.parse_args()
root=Path(__file__).resolve().parents[1];apk=args.apk.resolve()
if not apk.is_file():raise SystemExit('Provide the downloaded, reviewed Seeker Store APK with --apk')
def adb(*words):return subprocess.check_output([args.adb,'-s',args.serial,*words],text=True).strip()
if adb('get-state')!='device':raise SystemExit('Selected phone is not connected/authorized')
if int(adb('shell','getprop','ro.build.version.sdk'))<26:raise SystemExit('Android API 26+ required')
abis=adb('shell','getprop','ro.product.cpu.abilist')
if 'arm64-v8a' not in abis:raise SystemExit('ARM64 phone required by this APK')
h=hashlib.sha256()
with args.model.open('rb') as f:
 for block in iter(lambda:f.read(1048576),b''):h.update(block)
if h.hexdigest()!='6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e':raise SystemExit('Model checksum mismatch; no files transferred')
adb('install','-r',str(apk));adb('push',str(args.model),'/sdcard/Download/qwen2.5-1.5b-instruct-q4_k_m.gguf')
adb('shell','am','start','-n','com.codesbyfebin.skrdstore/.MainActivity')
(root/'device-setup-report.json').write_text(json.dumps({'time':datetime.datetime.now(datetime.timezone.utc).isoformat(),'api':int(adb('shell','getprop','ro.build.version.sdk')),'abis':abis,'apk_install':'PASS','model_checksum':'PASS','model_staged':'PASS','model_import':'NOT_RUN','phone_inference':'NOT_RUN','wallet_signing':'NOT_RUN','airplane_mode':'NOT_RUN'},indent=2)+'\n')
print('APK installed and verified model staged. On the phone: Agent → Import verified GGUF → Downloads → Qwen file. Then test in airplane mode. This command has not validated inference or signed a wallet transaction.')
