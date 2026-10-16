package com.ziyi.leetcodereviewsystem.coach;

import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/v2")
public class CoachController {
    private final CoachLearningService service; private final CoachChatService chat;
    public CoachController(CoachLearningService service,CoachChatService chat){this.service=service;this.chat=chat;}
    @GetMapping("/me") public Object me(Principal p){return service.profile(p.getName());}
    @GetMapping("/bootstrap") public Object bootstrap(Principal p){return service.bootstrap(p.getName());}
    @GetMapping("/catalog") public Object catalog(){return service.catalog();}
    @PutMapping("/profile") public Object profile(Principal p,@RequestBody Map<String,Object> input){return service.updateProfile(p.getName(),input);}
    @PostMapping("/conversations") public Object conversation(Principal p){return service.conversation(p.getName());}
    @GetMapping("/conversations/{id}/messages") public Object messages(Principal p,@PathVariable String id,@RequestParam(defaultValue="0") int page){return chat.messages(p.getName(),id,page);}
    @GetMapping("/conversations/{id}/requests/{requestId}") public Object requestStatus(Principal p,@PathVariable String id,@PathVariable String requestId){return chat.requestStatus(p.getName(),id,requestId);}
    @PostMapping("/conversations/{id}/messages") public Object send(Principal p,@PathVariable String id,@RequestBody Map<String,Object> input){return chat.send(p.getName(),id,input);}
    @PostMapping("/drafts") public Object draft(Principal p,@RequestBody Map<String,Object> input){return service.propose(p.getName(),CoachLearningService.text(input,"conversationId",40),CoachLearningService.budget(input,((Number)service.profile(p.getName()).get("budgetMinutes")).longValue()),CoachLearningService.text(input,"focus",1000),CoachLearningService.text(input,"difficulty",20));}
    @GetMapping("/drafts/{id}") public Object draftDetail(Principal p,@PathVariable String id){return service.draftDetail(p.getName(),id);}
    @PutMapping("/drafts/{id}") public Object revise(Principal p,@PathVariable String id,@RequestBody Map<String,Object> input){return service.revise(p.getName(),id,input);}
    @PostMapping("/drafts/{id}/confirm") public Object confirm(Principal p,@PathVariable String id,@RequestBody Map<String,Object> input){return service.confirm(p.getName(),id,input);}
    @GetMapping("/sessions/{id}") public Object session(Principal p,@PathVariable String id){return service.session(p.getName(),id);}
    @PostMapping("/sessions/{id}/state") public Object state(Principal p,@PathVariable String id,@RequestBody Map<String,Object> input){return service.state(p.getName(),id,CoachLearningService.text(input,"state",20));}
    @GetMapping("/items/{id}/workspace") public Object workspace(Principal p,@PathVariable String id){return service.workspace(p.getName(),id);}
    @PutMapping("/attempts/{id}") public Object feedback(Principal p,@PathVariable String id,@RequestBody Map<String,Object> input){return service.feedback(p.getName(),id,input);}
    @GetMapping("/journals") public Object journals(Principal p,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="0") int page){return service.journals(p.getName(),q,page);}
    @GetMapping("/journals/{id}") public Object journal(Principal p,@PathVariable String id){return service.journal(p.getName(),id);}
    @PutMapping("/journals/{id}") public Object saveJournal(Principal p,@PathVariable String id,@RequestBody Map<String,Object> input){return service.saveJournal(p.getName(),id,input);}
    @GetMapping("/memories") public Object memories(Principal p){return service.memories(p.getName());}
    @PostMapping("/memories") public Object memory(Principal p,@RequestBody Map<String,Object> input){return service.memory(p.getName(),null,input);}
    @PutMapping("/memories/{id}") public Object memoryUpdate(Principal p,@PathVariable String id,@RequestBody Map<String,Object> input){return service.memory(p.getName(),id,input);}
    @DeleteMapping("/memories/{id}") public void deleteMemory(Principal p,@PathVariable String id){service.deleteMemory(p.getName(),id);}
    @GetMapping("/growth") public Object growth(Principal p){return service.growth(p.getName());}
    @GetMapping("/reviews") public Object reviews(Principal p){return service.reviews(p.getName());}
    @GetMapping("/skills") public Object skills(Principal p){return service.skills(p.getName());}
    @GetMapping("/export") public Object export(Principal p){return service.export(p.getName());}
    @DeleteMapping("/account") public void deleteAccount(Principal p,@RequestBody Map<String,Object> input,HttpServletRequest req){service.deleteAccount(p.getName(),CoachLearningService.text(input,"confirmation",40));req.getSession().invalidate();}

    @PostMapping("/sessions/{id}/changes") public Object proposeChange(Principal p,@PathVariable String id,@RequestBody Map<String,Object> input){return service.proposeChange(p.getName(),id,input);}
    @PostMapping("/changes/{id}/confirm") public Object confirmChange(Principal p,@PathVariable String id,@RequestBody Map<String,Object> input){return service.confirmChange(p.getName(),id,input);}
    @GetMapping("/recap") public Object recap(Principal p,@RequestParam String from,@RequestParam String to){return service.recap(p.getName(),from,to);}
    @GetMapping("/interview-plan") public Object interviewPlan(Principal p){return service.interviewPlan(p.getName());}

    @DeleteMapping("/drafts/{id}") public void cancelDraft(Principal p,@PathVariable String id){service.cancelDraft(p.getName(),id);}
    @PostMapping("/changes/{id}/cancel") public void cancelChange(Principal p,@PathVariable String id){service.cancelChange(p.getName(),id);}
    @PostMapping("/messages/{id}/report") public Object report(Principal p,@PathVariable String id,@RequestBody Map<String,Object> input){return service.reportTutoring(p.getName(),id,input);}
}
