import test from 'node:test';
import assert from 'node:assert/strict';
import {api,clearCsrf,ApiError} from '../src/api.ts';
const originalFetch=globalThis.fetch;
test.afterEach(()=>{globalThis.fetch=originalFetch;clearCsrf();});
test('mutations obtain CSRF and send only same-origin cookie credentials',async()=>{
 const calls=[];globalThis.fetch=async(url,init)=>{calls.push({url,init});return new Response(JSON.stringify(url.endsWith('/csrf')?{headerName:'X-CSRF-TOKEN',token:'synthetic-csrf'}:{saved:true}));};
 assert.deepEqual(await api('/attempts/synthetic','PUT',{notes:'local synthetic'}),{saved:true});
 assert.equal(calls.length,2);assert.equal(calls[1].init.credentials,'same-origin');assert.equal(calls[1].init.headers['X-CSRF-TOKEN'],'synthetic-csrf');
 assert.equal(JSON.parse(calls[1].init.body).notes,'local synthetic');
});
test('CSRF rejection clears cached token and does not replay a mutation blindly',async()=>{
 let tokens=0,mutations=0;globalThis.fetch=async(url)=>{if(url.endsWith('/csrf'))return new Response(JSON.stringify({headerName:'X-CSRF-TOKEN',token:'t'+(++tokens)}));mutations++;return new Response('{}',{status:403});};
 for(let i=0;i<2;i++)await assert.rejects(api('/conversations','POST',{}),e=>e instanceof ApiError&&e.status===403);
 assert.equal(tokens,2);assert.equal(mutations,2);
});
test('network interruption is distinguishable from logout and sends no automatic duplicate',async()=>{
 let requests=0;globalThis.fetch=async()=>{requests++;throw new TypeError('synthetic offline');};
 await assert.rejects(api('/bootstrap'),e=>e.status===0&&e.message.includes('输入仍保留'));
 assert.equal(requests,1);
});
test('version conflict preserves a distinct recoverable status',async()=>{
 globalThis.fetch=async()=>new Response('{"code":"STALE_VERSION"}',{status:409});
 await assert.rejects(api('/bootstrap'),e=>e.status===409&&e.message.includes('保留输入'));
});
test('empty successful deletion response is supported',async()=>{
 globalThis.fetch=async()=>new Response(null,{status:204});assert.equal(await api('/me'),undefined);
});

test('stream status followed by split UTF-8 result restores exactly one reply',async()=>{
 const {streamMessage}=await import('../src/api.ts');let sent=0;
 const encoded=new TextEncoder().encode('event: status\ndata: {"state":"pending"}\n\nevent: result\ndata: {"message":{"content":"合成回复"}}\n\n');
 globalThis.fetch=async(url)=>url.endsWith('/csrf')?new Response('{"headerName":"X-CSRF-TOKEN","token":"synthetic"}'):new Response(new ReadableStream({start(controller){sent++;for(let n=0;n<encoded.length;n+=7)controller.enqueue(encoded.slice(n,n+7));controller.close();}}));
 assert.equal((await streamMessage('/conversations/synthetic/messages/stream',{})).message.content,'合成回复');assert.equal(sent,1);
});
test('disconnected stream never pretends the write failed or resends blindly',async()=>{
 const {streamMessage}=await import('../src/api.ts');let sent=0;
 globalThis.fetch=async(url)=>url.endsWith('/csrf')?new Response('{"headerName":"X-CSRF-TOKEN","token":"synthetic"}'):new Response(new ReadableStream({start(controller){sent++;controller.enqueue(new TextEncoder().encode('event: status\ndata: {"state":"pending"}\n\n'));controller.close();}}));
 await assert.rejects(streamMessage('/conversations/synthetic/messages/stream',{}),e=>e.status===0&&e.message.includes('检查原请求'));
 assert.equal(sent,1);
});
