#!/usr/bin/env python3
"""Opt-in synthetic acceptance for the fixed AlgoBloom free demo only; never Railway production.
Real AI calls consume existing provider credit. No API key or database credential is read.
"""
import argparse
import http.cookiejar
import json
from pathlib import Path
import secrets
import ssl
import urllib.request
import urllib.error

BASE = 'https://algobloom-demo.onrender.com'

class DemoClient:
    def __init__(self):
        context=ssl.create_default_context()
        system_bundle=Path('/etc/ssl/cert.pem')
        if system_bundle.exists():context.load_verify_locations(cafile=str(system_bundle))
        self.opener=urllib.request.build_opener(urllib.request.HTTPSHandler(context=context),urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        self.csrf=None
    def call(self,path,method='GET',body=None):
        if not path.startswith('/') or '://' in path:raise ValueError('Fixed demo paths only')
        if method!='GET' and self.csrf is None:self.csrf=self.call('/auth/csrf')
        headers={'Accept':'application/json'}
        if method!='GET':headers.update({self.csrf['headerName']:self.csrf['token'],'Content-Type':'application/json'})
        req=urllib.request.Request(BASE+'/api/v2'+path,method=method,headers=headers,data=None if body is None else json.dumps(body).encode())
        with self.opener.open(req,timeout=60) as r:text=r.read().decode()
        return json.loads(text) if text else None

def run():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--real',action='store_true');args=parser.parse_args()
    if not args.real:parser.error('--real required for the bounded public AI smoke')
    client=DemoClient();username='synthetic_public_'+secrets.token_hex(5);password=secrets.token_urlsafe(24)
    report={'scope':BASE,'syntheticUser':username,'checks':[],'transcripts':[],'status':'running'}
    output=Path('target/coach-public-smoke.json');output.parent.mkdir(exist_ok=True)
    def check(name):report['checks'].append(name);print('PASS:',name,flush=True);output.write_text(json.dumps(report,ensure_ascii=False,indent=2))
    def send(text,intent,item=None):
        data={'requestId':secrets.token_hex(12),'content':text,'intent':intent,'contextRevision':1}
        if item:data.update({'sessionItemId':item['id'],'sessionId':item['sessionId']})
        reply=client.call('/conversations/'+conversation+'/messages','POST',data)
        assert not reply.get('demo') and reply['message']['content'].strip()
        assert reply['message'].get('source') not in ['rules','application'],'Application fallback is not real AI acceptance'
        report['transcripts'].append({'intent':intent,'reply':reply})
        return reply
    try:
        client.call('/auth/register','POST',{'username':username,'password':password})
        client.call('/auth/login','POST',{'username':username,'password':password});client.csrf=None
        assert client.call('/me')['username']==username;check('HTTPS registration/login with secure session')
        conversation=client.call('/conversations','POST',{})['id'];assert client.call('/journals')==[]
        reply=send('这是合成上线验收。我有20分钟，想练Easy题。请用合法目录创建可修改计划草稿，不要替我确认。','plan')
        assert reply.get('draft');assert client.call('/journals')==[];check('Real AI planning creates a draft, not a Journal')
        draft=client.call('/drafts/'+reply['draft']['id'])
        confirm={'revision':draft['revision'],'confirmation':'CONFIRM','idempotencyKey':secrets.token_hex(12)}
        session=client.call('/drafts/'+draft['id']+'/confirm','POST',confirm)
        assert client.call('/drafts/'+draft['id']+'/confirm','POST',confirm)['id']==session['id'];check('Explicit confirmation is idempotent')
        item=session['items'][0];workspace=client.call('/items/'+item['id']+'/workspace')
        assert workspace['problem']['url'].startswith('https://leetcode.com/problems/');check('Practice Workspace has canonical official link')
        send('我还没开始，只给一个很小的观察问题，不要数据结构、完整策略或代码。','hint',item)
        workspace=client.call('/items/'+item['id']+'/workspace');assert workspace['attempt']['highestHint']==1 and not workspace['attempt']['completed'];check('Real L1 tutoring does not mark completion')
        send('只用两句话解释时间复杂度是什么，不给当前题完整解法。','concept',item)
        workspace=client.call('/items/'+item['id']+'/workspace');assert workspace['attempt']['highestHint']==1;check('Concept discussion preserves hint level')
        a=workspace['attempt'];note='合成公开验收：提示后仍在理解，原文应持久保存。'
        saved=client.call('/attempts/'+a['id'],'PUT',{'version':a['version'],'completed':True,'result':'understanding','actualMinutes':8,'notes':note})
        jid=session['journal']['id'];journal=client.call('/journals/'+jid)
        assert next(i for i in journal['session']['items'] if i['id']==item['id'])['attempt']==saved
        body='Synthetic public demo acceptance: one session, one Journal.'
        client.call('/journals/'+jid,'PUT',{'version':journal['version'],'title':'Synthetic launch check','body':body,'feelings':''})
        assert any(x['journalId']==jid for x in client.call('/growth'));check('Workspace/Journal share feedback and Growth links the session')
        client.call('/auth/logout','POST',{});client.csrf=None
        client.call('/auth/login','POST',{'username':username,'password':password});client.csrf=None
        assert client.call('/journals/'+jid)['body']==body
        assert len(client.call('/conversations/'+conversation+'/messages'))>=6;check('Conversation and Journal survive logout/login')
        send('结合我刚保存的练习结果，为下一次安排10分钟的一道Easy题，只创建草稿，不确认。不要说成独立解出。','plan')
        assert len(client.call('/journals'))==1;check('Future personalized planning does not create another Journal')
        report['status']='passed; semantic transcripts retained for review'
    except Exception as e:
        report['status']='failed';report['errorType']=type(e).__name__
        if isinstance(e,urllib.error.HTTPError):report['httpStatus']=e.code
        raise
    finally:output.write_text(json.dumps(report,ensure_ascii=False,indent=2))
    print('Public synthetic acceptance report:',output,flush=True)

if __name__=='__main__':run()
