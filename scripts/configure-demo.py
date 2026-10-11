#!/usr/bin/env python3
"""Configure an owner-supplied recipient; never invent addresses or expose RPC keys."""
import argparse,os,json,urllib.request,urllib.parse
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--recipient',required=True);p.add_argument('--catalog',default='https://skr-dstore-catalog.modest-twin-72b.workers.dev/api/catalog');args=p.parse_args()
rpc=os.environ.get('RPC_URL','https://api.devnet.solana.com')
alphabet='123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz';n=0
for c in args.recipient:
 if c not in alphabet:raise SystemExit('Invalid Base58 recipient')
 n=n*58+alphabet.index(c)
b=(n.to_bytes((n.bit_length()+7)//8,'big') if n else b'');b=b'\x00'*(len(args.recipient)-len(args.recipient.lstrip('1')))+b
if len(b)!=32 or not any(b):raise SystemExit('Reviewed 32-byte recipient required')
for url in [rpc,args.catalog]:
 u=urllib.parse.urlparse(url)
 if u.scheme!='https' or not u.hostname or u.username or u.fragment or '\n' in url:raise SystemExit('HTTPS configuration required')
def call(method,params):
 data=json.dumps({'jsonrpc':'2.0','id':1,'method':method,'params':params}).encode()
 with urllib.request.urlopen(urllib.request.Request(rpc,data=data,headers={'Content-Type':'application/json'}),timeout=25) as response:r=json.load(response)
 if 'error' in r:raise SystemExit('RPC validation failed')
 return r['result']
if call('getGenesisHash',[])!='EtWTRABZaYq6iMfeYKouRu166VU2xqa1wcaWoxPkrZBG':raise SystemExit('Endpoint is not devnet')
if call('getAccountInfo',[args.recipient,{'encoding':'base64','commitment':'confirmed'}])['value'] is None:raise SystemExit('Recipient has no existing devnet account; independently review and provision it first')
root=Path(__file__).resolve().parents[1];path=root/'local.properties';settings={}
if path.exists():
 for line in path.read_text().splitlines():
  if '=' in line and not line.lstrip().startswith('#'):k,v=line.split('=',1);settings[k]=v
settings.update(RPC_URL=rpc,ENDORSEMENT_RECIPIENT=args.recipient,CATALOG_URL=args.catalog)
path.write_text('\n'.join(k+'='+v for k,v in settings.items())+'\n')
print('Owner-supplied recipient and devnet endpoint validated; local settings updated. Rebuild the APK. No secret configuration values printed.')
