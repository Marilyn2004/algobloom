let csrf:{token:string;headerName:string}|null=null;
export class ApiError extends Error {
 status:number;
 constructor(message:string,status:number){super(message);this.name='ApiError';this.status=status;}
}
export function clearCsrf(){csrf=null;}
const friendly:Record<string,string>={
 DAILY_COACH_LIMIT:'今天的 Coach 调用额度已用完，你仍可以保存练习和笔记。',
 REQUEST_IN_PROGRESS:'上一条请求仍在处理中，请稍后重试，原请求不会重复执行。',
 'Coach temporarily unavailable':'AI 服务暂时不可用，可能是额度或限流问题，请稍后重试。',
 'Coach input exceeds limit':'这条消息和相关上下文较长，请缩短消息后重试。',
 'Coach is not configured':'尚未配置 AI 服务；规则草稿和笔记保存仍可使用。'
};
export async function api<T=any>(path:string,method='GET',body?:unknown):Promise<T>{
 const headers:Record<string,string>={'Accept':'application/json'};
 if(method!=='GET'){
  if(!csrf){const r=await fetch('/api/v2/auth/csrf',{credentials:'same-origin'});if(!r.ok)throw new ApiError('无法取得安全令牌',r.status);csrf=await r.json();}
  headers[csrf!.headerName]=csrf!.token;headers['Content-Type']='application/json';
 }
 let r:Response;
 try{r=await fetch('/api/v2'+path,{method,headers,credentials:'same-origin',body:body===undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(55000)});}
 catch(e:any){throw new ApiError(e.name==='TimeoutError'?'等待超时；输入仍保留，可以检查原请求后重试。':'连接暂时中断；输入仍保留，请恢复连接后重试。',0);}
 if(!r.ok){if(r.status===403)clearCsrf();const value=await r.json().catch(()=>({}));throw new ApiError(r.status===409?'版本已更新，请保留输入并重新载入核对。':r.status===401?'请先登录。':friendly[value.code]||value.code||`请求失败 (${r.status})`,r.status);}
 if(r.status===204)return undefined as T;const text=await r.text();return text?JSON.parse(text):undefined;
}

// Complete messages are persisted server-side; this stream carries status and the final result.
export async function streamMessage<T=any>(path:string,body:unknown):Promise<T>{
 if(!csrf){const r=await fetch('/api/v2/auth/csrf',{credentials:'same-origin'});if(!r.ok)throw new ApiError('无法取得安全令牌',r.status);csrf=await r.json();}
 let response:Response;
 try{response=await fetch('/api/v2'+path,{method:'POST',credentials:'same-origin',headers:{Accept:'text/event-stream','Content-Type':'application/json',[csrf!.headerName]:csrf!.token},body:JSON.stringify(body),signal:AbortSignal.timeout(55000)});}
 catch{throw new ApiError('连接暂时中断；输入仍保留，可以检查原请求后重试。',0);}
 if(!response.ok){if(response.status===403)clearCsrf();const value=await response.json().catch(()=>({}));throw new ApiError(friendly[value.code]||value.code||'无法开始对话',response.status);}
 if(!response.body)throw new ApiError('回复连接不可用；输入仍保留。',0);
 const reader=response.body.getReader(),decoder=new TextDecoder();let buffer='';
 try{
  while(true){const chunk=await reader.read();if(chunk.done)break;buffer+=decoder.decode(chunk.value,{stream:true}).replace(/\r\n/g,'\n');
   let end:number;while((end=buffer.indexOf('\n\n'))>=0){const block=buffer.slice(0,end);buffer=buffer.slice(end+2);const event=block.split('\n').find(line=>line.startsWith('event:'))?.slice(6).trim();const raw=block.split('\n').filter(line=>line.startsWith('data:')).map(line=>line.slice(5).trimStart()).join('\n');if(!raw)continue;const data=JSON.parse(raw);
    if(event==='error')throw new ApiError(friendly[data.code]||data.code||'Coach 暂时不可用',data.status||503);
    if(event==='result')return data as T;
   }
  }
 }catch(e){if(e instanceof ApiError)throw e;throw new ApiError('回复连接中断；输入仍保留，可以检查原请求后重试。',0);}
 finally{reader.releaseLock();}
 throw new ApiError('回复尚未完整接收；输入仍保留，可以检查原请求后重试。',0);
}
