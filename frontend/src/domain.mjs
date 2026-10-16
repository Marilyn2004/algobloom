export function contextFor(path){
 let m=path.match(/^\/sessions\/([^/]+)\/problems\/([^/]+)$/);if(m)return {page:'workspace',sessionId:m[1],sessionItemId:m[2]};
 m=path.match(/^\/sessions\/([^/]+)$/);if(m)return {page:'session',sessionId:m[1]};
 m=path.match(/^\/journal\/([^/]+)$/);if(m)return {page:'journal',journalId:m[1]};return {page:'general'};
}
export function officialUrl(value){try{const u=new URL(value);return u.protocol==='https:'&&u.hostname==='leetcode.com'&&/^\/problems\/[a-z0-9-]+\/$/.test(u.pathname)&&!u.search&&!u.hash&&!u.username&&!u.password&&!u.port?u.href:null;}catch{return null;}}
export function responseBelongsTo(response,item){return (response?.message?.context?.sessionItemId??null)===(item??null);}

// A dirty local draft takes precedence until the learner explicitly resolves a stale version.
export function recoverFeedback(server,cached){
 if(!cached||cached.id!==server.id)return {form:server,conflict:null};
 return {form:cached,conflict:cached.version===server.version?null:server};
}
