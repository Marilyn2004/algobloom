#!/usr/bin/env python3
"""Non-AI acceptance: fixed loopback API, newly generated disposable synthetic accounts only."""
import json
from pathlib import Path
import secrets
import time
from urllib.error import HTTPError
from coach_real_smoke import LocalClient

clients=[];identities=[];reads=[];writes=[]
for _ in range(2):
    c=LocalClient();u='synthetic_local_'+secrets.token_hex(5);pw=secrets.token_urlsafe(24)
    c.call('/auth/register','POST',{'username':u,'password':pw});c.call('/auth/login','POST',{'username':u,'password':pw})
    clients.append(c);identities.append((u,pw))
a,b=clients

def timed(path,method='GET',body=None):
    start=time.monotonic();value=a.call(path,method,body)
    (reads if method=='GET' else writes).append((time.monotonic()-start)*1000)
    return value

def expect(code,fn):
    try:fn()
    except HTTPError as e:
        assert e.code==code,(e.code,code)
    else:raise AssertionError('Expected HTTP '+str(code))

c=timed('/conversations','POST',{})
d=timed('/drafts','POST',{'conversationId':c['id'],'budgetMinutes':20})
old=dict(d)
d=timed('/drafts/'+d['id'],'PUT',{'revision':d['revision'],'budgetMinutes':10,'items':[{'problemId':d['items'][0]['problemId'],'minutes':10}]})
expect(409,lambda:a.call('/drafts/'+d['id']+'/confirm','POST',{'revision':old['revision'],'confirmation':'CONFIRM','idempotencyKey':'stale-local-confirm'}))
confirm={'revision':d['revision'],'confirmation':'CONFIRM','idempotencyKey':'same-local-confirm'}
s=timed('/drafts/'+d['id']+'/confirm','POST',confirm)
assert timed('/drafts/'+d['id']+'/confirm','POST',confirm)['id']==s['id']
i=s['items'][0];at=i['attempt'];j=s['journal']
original='合成 API 验收原文：不自动改写 <b>原始字符串</b>。'
f=timed('/attempts/'+at['id'],'PUT',{'version':at['version'],'completed':True,'result':'understanding','actualMinutes':None,'notes':original,'feelings':'synthetic private feeling'})
assert timed('/journals/'+j['id'])['session']['items'][0]['attempt']==f
expect(409,lambda:a.call('/attempts/'+at['id'],'PUT',{'version':at['version'],'notes':'stale overwrite'}))
assert timed('/journals?q='+__import__('urllib.parse',fromlist=['quote']).quote('合成 API 验收原文'))
m=timed('/memories','POST',{'content':'synthetic preference'})
for path in ['/sessions/'+s['id'],'/journals/'+j['id'],'/items/'+i['id']+'/workspace','/drafts/'+d['id'],'/conversations/'+c['id']+'/messages']:
    expect(404,lambda path=path:b.call(path))
assert not b.call('/growth') and not b.call('/memories')
expect(404,lambda:b.call('/memories/'+m['id'],'PUT',{'content':'foreign overwrite'}))
mem=timed('/memories/'+m['id'],'PUT',{'content':'updated synthetic preference'})
timed('/memories/'+m['id'],'DELETE')
assert not timed('/memories')
for state in ['paused','ended','active']:
    assert timed('/sessions/'+s['id']+'/state','POST',{'state':state})['state']==state
for _ in range(20):
    timed('/reviews')
    timed('/profile','PUT',{'timezone':'America/Los_Angeles','budgetMinutes':20,'goal':'Synthetic local checks','analyzeFeelings':False})
export=timed('/export');assert export['attempts'][0]['notes']==original
assert len(export['journals'])==1 and len(export['sessions'])==1
# Deletion is restricted to this script's freshly generated disposable account.
timed('/account','DELETE',{'confirmation':'DELETE_MY_ACCOUNT'})
expect(401,lambda:a.call('/bootstrap'))
a.csrf=None
expect(401,lambda:a.call('/auth/login','POST',{'username':identities[0][0],'password':identities[0][1]}))
assert b.call('/me')['username']==identities[1][0]
def p95(values):return round(sorted(values)[int((len(values)-1)*.95)],2)
report={'status':'passed','checks':['stale revision','confirmation idempotency','shared feedback','raw text','search','foreign ownership','memory edit/delete','pause/end/resume','owned export','disposable-account deletion'],
        'localReadP95Ms':p95(reads),'localWriteP95Ms':p95(writes),'readSamples':len(reads),'writeSamples':len(writes),
        'scope':'loopback synthetic accounts only; no AI calls or production data'}
Path('target/coach-local-checks.json').write_text(json.dumps(report,indent=2))
print(json.dumps(report,indent=2))
