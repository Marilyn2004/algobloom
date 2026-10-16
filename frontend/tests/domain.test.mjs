import test from 'node:test';import assert from 'node:assert/strict';import {contextFor,officialUrl,responseBelongsTo} from '../src/domain.mjs';
test('workspace context binds both session and exact item',()=>assert.deepEqual(contextFor('/sessions/a/problems/b'),{page:'workspace',sessionId:'a',sessionItemId:'b'}));
test('journal and growth do not create attempts',()=>{assert.deepEqual(contextFor('/journal/x'),{page:'journal',journalId:'x'});assert.deepEqual(contextFor('/growth'),{page:'general'});});
test('official links reject malicious hosts and protocols',()=>{assert.equal(officialUrl('https://leetcode.com/problems/two-sum/'),'https://leetcode.com/problems/two-sum/');for(const x of ['javascript:alert(1)','https://leetcode.com.evil.test/problems/x/','https://evil.test/problems/x/','https://leetcode.com/problems/x/?redirect=evil'])assert.equal(officialUrl(x),null);});
test('late responses never become the new item context',()=>{const r={message:{context:{sessionItemId:'A'}}};assert.equal(responseBelongsTo(r,'B'),false);assert.equal(responseBelongsTo(r,'A'),true);});

test('unsaved feedback survives reload and stale server versions require explicit reconciliation',async()=>{
 const {recoverFeedback}=await import('../src/domain.mjs');
 const server={id:'attempt-A',version:2,notes:'saved original'},draft={id:'attempt-A',version:1,notes:'unsaved learner text'};
 assert.deepEqual(recoverFeedback(server,draft),{form:draft,conflict:server});
 assert.deepEqual(recoverFeedback(server,{...draft,version:2}),{form:{...draft,version:2},conflict:null});
 assert.deepEqual(recoverFeedback(server,{...draft,id:'attempt-B'}),{form:server,conflict:null});
});
