package com.ziyi.leetcodereviewsystem.coach;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.Semaphore;

/** Streams request state/result; disconnecting never cancels a persisted operation. */
@RestController
@RequestMapping("/api/v2/conversations")
public class CoachStreamController {
    private final CoachChatService chat;
    private final Semaphore capacity=new Semaphore(16);
    public CoachStreamController(CoachChatService chat){this.chat=chat;}
    @PostMapping(value="/{id}/messages/stream",produces="text/event-stream")
    public SseEmitter send(Principal principal,@PathVariable String id,@RequestBody Map<String,Object> input){
        if(!capacity.tryAcquire())throw new ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,"COACH_BUSY");
        String user=principal.getName();var emitter=new SseEmitter(55000L);
        Thread.startVirtualThread(()->{
            try{
                emit(emitter,"status",Map.of("state","pending"));
                var reply=chat.send(user,id,input);
                emit(emitter,"result",reply);
            }catch(ResponseStatusException e){emit(emitter,"error",Map.of("code",e.getReason()==null?"COACH_FAILED":e.getReason(),"status",e.getStatusCode().value()));}
            catch(IllegalArgumentException|ArithmeticException e){emit(emitter,"error",Map.of("code","INVALID_REQUEST","status",400));}
            catch(RuntimeException e){emit(emitter,"error",Map.of("code","COACH_UNAVAILABLE","status",503));}
            finally{capacity.release();try{emitter.complete();}catch(RuntimeException ignored){}}
        });
        return emitter;
    }
    private void emit(SseEmitter emitter,String event,Object data){
        try{emitter.send(SseEmitter.event().name(event).data(data));}
        catch(java.io.IOException|IllegalStateException disconnected){/* Operation still finishes; recover by requestId. */}
    }
}
