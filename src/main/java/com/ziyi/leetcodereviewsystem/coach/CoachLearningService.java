package com.ziyi.leetcodereviewsystem.coach;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class CoachLearningService {
    final CoachStore db; final Clock clock;
    @org.springframework.beans.factory.annotation.Value("${coach.rules.minimum-samples:3}") private int minimumSamples=3;
    @org.springframework.beans.factory.annotation.Value("${coach.rules.minimum-problems:2}") private int minimumProblems=2;
    @org.springframework.beans.factory.annotation.Value("${coach.rules.independent-days:7}") private int independentDays=7;
    @org.springframework.beans.factory.annotation.Value("${coach.rules.hinted-days:3}") private int hintedDays=3;
    @org.springframework.beans.factory.annotation.Value("${coach.rules.understanding-days:1}") private int understandingDays=1;
    public CoachLearningService(CoachStore db,Clock clock){this.db=db;this.clock=clock;}
    static String id(){return UUID.randomUUID().toString();}
    static void check(boolean condition,String code){if(!condition)throw new ResponseStatusException(HttpStatus.CONFLICT,code);}
    static String text(Map<String,Object> m,String key,int limit){Object v=m.get(key);String s=v==null?"":v.toString();if(s.length()>limit)throw new IllegalArgumentException("Too long");return s;}
    static long number(Map<String,Object> m,String key,long fallback){Object v=m.get(key);if(v==null)return fallback;if(!(v instanceof Number))throw new IllegalArgumentException("Expected integer");return new java.math.BigDecimal(v.toString()).longValueExact();}
    static int budget(Map<String,Object> input,long fallback){long value=number(input,"budgetMinutes",fallback);if(value<1||value>240)throw new IllegalArgumentException("Invalid budget");return (int)value;}
    static List<Map<String,Object>> maps(Object value){
        if(!(value instanceof List<?> list)||list.stream().anyMatch(v->!(v instanceof Map)))throw new IllegalArgumentException("Expected item list");
        return (List<Map<String,Object>>)value;
    }
    public Map<String,Object> profile(String user){var p=new LinkedHashMap<>((Map<String,Object>)db.one("select profile from coach_users where username=?",user).get("profile"));p.put("username",user);return p;}
    public Map<String,Object> updateProfile(String user,Map<String,Object> input){
        var p=profile(user);String zone=text(input,"timezone",80);ZoneId.of(zone);
        p.put("timezone",zone);p.put("goal",text(input,"goal",1000));
        long budget=number(input,"budgetMinutes",30);if(budget<1||budget>240)throw new IllegalArgumentException();
        p.put("budgetMinutes",budget);String target=text(input,"targetDate",10);if(!target.isBlank())LocalDate.parse(target);p.put("targetDate",target);p.put("analyzeFeelings",Boolean.TRUE.equals(input.get("analyzeFeelings")));
        p.remove("username");db.update("update coach_users set profile=?::jsonb where username=?",db.json(p),user);return profile(user);
    }
    public List<Map<String,Object>> catalog(){
        var rows=db.rows("select p.id,p.title,p.difficulty,p.platform,p.external_problem_id,p.slug,p.url,c.context_version,c.provenance,c.premium from coach_catalog c join problems p on p.id=c.id where not c.archived and not c.premium and p.url='https://leetcode.com/problems/'||p.slug||'/' and p.slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$' and p.difficulty in ('Easy','Medium','Hard') order by c.position");
        for(var row:rows) row.put("patterns",db.rows("select t.id,t.name,t.slug from patterns t join problem_patterns pp on pp.pattern_id=t.id where pp.problem_id=? order by t.slug",row.get("id")));
        return rows;
    }
    public Map<String,Object> conversation(String user){String id=id();db.update("insert into coach_conversations(id,owner) values (?::uuid,?)",id,user);return db.owned("conversations",id,user,false);}
    public List<Map<String,Object>> evidence(String user){return db.rows("select a.*,i.problem_id,i.snapshot,s.timezone from coach_attempts a join coach_items i on i.id=a.item_id join coach_sessions s on s.id=i.session_id where a.owner=? order by a.first_completed_at nulls last,a.id",user);}
    public List<Map<String,Object>> reviews(String user){
        Map<Integer,Map<String,Object>> states=new LinkedHashMap<>();ZoneId zone=ZoneId.of(profile(user).get("timezone").toString());
        for(var a:evidence(user)){
            if(!Boolean.TRUE.equals(a.get("completed"))||a.get("result")==null)continue;
            int problem=((Number)a.get("problemId")).intValue();var previous=states.get(problem);
            String result=a.get("result").toString();int streak=result.equals("independent")?(previous==null?1:((Number)previous.get("streak")).intValue()+1):0;
            int days=switch(result){case "independent"->Math.min(30,Math.clamp(independentDays,1,30)*(1<<Math.min(5,Math.max(0,streak-1))));case "hinted"->Math.clamp(hintedDays,1,30);default->Math.clamp(understandingDays,1,30);};
            LocalDate practiced=Instant.parse(a.get("firstCompletedAt").toString()).atZone(zone).toLocalDate();
            states.put(problem,new LinkedHashMap<>(Map.of("problemId",problem,"dueAt",practiced.plusDays(days).toString(),"intervalDays",days,"streak",streak,"evidenceId",a.get("id"),"ruleVersion","result-v1")));
        }return states.values().stream().sorted(Comparator.comparing(a->a.get("dueAt").toString())).toList();
    }
    public List<Map<String,Object>> skills(String user){
        Map<String,List<Map<String,Object>>> groups=new LinkedHashMap<>();
        for(var a:evidence(user)) if(Boolean.TRUE.equals(a.get("completed"))&&a.get("result")!=null){
            var snapshot=(Map<String,Object>)a.get("snapshot");
            for(var pattern:maps(snapshot.get("patterns")))groups.computeIfAbsent(pattern.get("slug").toString(),k->new ArrayList<>()).add(a);
        }
        List<Map<String,Object>> result=new ArrayList<>();
        for(var entry:groups.entrySet()){
            var records=entry.getValue();long different=records.stream().map(a->a.get("problemId")).distinct().count();
            var recent=records.subList(Math.max(0,records.size()-3),records.size());
            long success=recent.stream().filter(a->"independent".equals(a.get("result"))&&number(a,"highestHint",0)==0).count();
            String status=records.size()<Math.max(3,minimumSamples)||different<Math.max(2,minimumProblems)?"still_learning":success>=2?"stable":success==1?"getting_familiar":"practice_suggested";
            result.add(Map.of("pattern",entry.getKey(),"status",status,"sampleSize",records.size(),"distinctProblems",different,"evidenceIds",records.stream().map(a->a.get("id")).toList(),"assistedAttempts",records.stream().filter(a->number(a,"highestHint",0)>0).count(),"difficultySuggestion",success>=2?"slightly_harder":recent.stream().filter(a->Set.of("understanding","solution").contains(a.get("result"))).count()>=2?"foundations":"unchanged"));
        }return result;
    }
    public Map<String,Object> propose(String user,String conversation,int budget,String focus,String difficulty){
        db.owned("conversations",conversation,user,false);if(budget<1||budget>240)throw new IllegalArgumentException();
        var candidates=catalog();var review=reviews(user);var attempts=evidence(user);
        Set<Integer> practiced=new HashSet<>();for(var a:attempts)if(Boolean.TRUE.equals(a.get("completed")))practiced.add(((Number)a.get("problemId")).intValue());
        LocalDate today=LocalDate.now(clock.withZone(ZoneId.of(profile(user).get("timezone").toString())));
        Set<Integer> due=new HashSet<>();for(var reviewItem:review)if(!LocalDate.parse(reviewItem.get("dueAt").toString()).isAfter(today))due.add(((Number)reviewItem.get("problemId")).intValue());
        var adaptations=skills(user);var harder=adaptations.stream().filter(a->a.get("difficultySuggestion").equals("slightly_harder")).map(a->a.get("pattern")).collect(java.util.stream.Collectors.toSet());
        var foundations=adaptations.stream().filter(a->a.get("difficultySuggestion").equals("foundations")).map(a->a.get("pattern")).collect(java.util.stream.Collectors.toSet());
        var weak=adaptations.stream().filter(a->a.get("status").equals("practice_suggested")).map(a->a.get("pattern").toString()).collect(java.util.stream.Collectors.toSet());
        candidates=new ArrayList<>(candidates);
        candidates.sort(Comparator.comparingInt((Map<String,Object> c)->due.contains(((Number)c.get("id")).intValue())?0:maps(c.get("patterns")).stream().anyMatch(p->focus.toLowerCase().contains(p.get("name").toString().toLowerCase())||weak.contains(p.get("slug"))||harder.contains(p.get("slug"))&&c.get("difficulty").equals("Medium")||foundations.contains(p.get("slug"))&&c.get("difficulty").equals("Easy"))?1:2));
        List<Map<String,Object>> items=new ArrayList<>();int remaining=budget;int reviewUsed=0;
        for(var problem:candidates){
            int pid=((Number)problem.get("id")).intValue();boolean repeat=practiced.contains(pid);
            if(repeat&&!due.contains(pid))continue;
            if(difficulty!=null&&!difficulty.isBlank()&&!difficulty.equals(problem.get("difficulty")))continue;
            int minutes=problem.get("difficulty").equals("Easy")?10:20;
            if(repeat&&reviewUsed>0&&reviewUsed+minutes>Math.max(10,budget/3))continue;
            if(minutes>remaining){if(items.isEmpty())minutes=remaining;else continue;}
            String reason=repeat?"A review based on your recorded practice":"A foundation exercise from our original learning path";
            var evidenceIds=attempts.stream().filter(a->((Number)a.get("problemId")).intValue()==pid).map(a->a.get("id")).toList();
            items.add(Map.of("problemId",pid,"problem",problem,"minutes",minutes,"type",repeat?"review":"new","reason",reason,"reasonCode",repeat?"DUE_REVIEW":"FOUNDATION","evidenceIds",evidenceIds));
            remaining-=minutes;if(repeat)reviewUsed+=minutes;if(remaining<=0||items.size()>=4)break;
        }
        check(!items.isEmpty(),"NO_MATCHING_CANDIDATES");String draft=id();
        db.update("insert into coach_drafts(id,owner,conversation_id,budget_minutes,focus,items,assumptions) values (?::uuid,?,?::uuid,?,?,?::jsonb,?)",draft,user,conversation,budget,focus,db.json(items),"Time is a practice budget, not a promise to solve. Unspecified budget defaults to 30 minutes.");
        return db.owned("drafts",draft,user,false);
    }
    public Map<String,Object> revise(String user,String draft,Map<String,Object> input){
        var d=db.owned("drafts",draft,user,true);check(d.get("sessionId")==null,"ALREADY_CONFIRMED");check(!Boolean.TRUE.equals(d.get("cancelled")),"DRAFT_CANCELLED");check(number(input,"revision",-1)==number(d,"revision",0),"STALE_REVISION");
        int budget=budget(input,number(d,"budgetMinutes",30));if(budget<1||budget>240)throw new IllegalArgumentException();
        var requested=maps(input.get("items"));validateItems(requested,budget);
        var allowed=catalog();List<Map<String,Object>> items=new ArrayList<>();
        for(var item:requested){var problem=allowed.stream().filter(c->number(c,"id",-1)==number(item,"problemId",-2)).findFirst().orElseThrow();
            items.add(Map.of("problemId",problem.get("id"),"problem",problem,"minutes",number(item,"minutes",10),"type",evidence(user).stream().anyMatch(a->Boolean.TRUE.equals(a.get("completed"))&&number(a,"problemId",-1)==number(problem,"id",-2))?"review":"new","reason","Explicitly adjusted by you","reasonCode","USER_ADJUSTMENT","evidenceIds",List.of()));}
        db.update("update coach_drafts set revision=revision+1,budget_minutes=?,items=?::jsonb where id=?::uuid and owner=?",budget,db.json(items),draft,user);return db.owned("drafts",draft,user,false);
    }
    public Map<String,Object> reviseFromCoach(String user,String draft,Map<String,Object> input){
        var revised=revise(user,draft,input);var attempts=evidence(user);
        var items=new ArrayList<Map<String,Object>>();
        for(var item:maps(revised.get("items"))){
            var copy=new LinkedHashMap<>(item);var problem=(Map<String,Object>)item.get("problem");
            var patterns=maps(problem.get("patterns")).stream().map(p->p.get("slug")).collect(java.util.stream.Collectors.toSet());
            var related=attempts.stream().filter(a->Boolean.TRUE.equals(a.get("completed"))&&a.get("result")!=null)
                .filter(a->a.get("problemId").equals(problem.get("id"))||maps(((Map<?,?>)a.get("snapshot")).get("patterns")).stream().anyMatch(p->patterns.contains(p.get("slug")))).toList();
            copy.put("evidenceIds",related.stream().map(a->a.get("id")).toList());
            copy.put("reasonCode",related.isEmpty()?"FOUNDATION":"RELATED_PRACTICE");
            copy.put("reason",related.isEmpty()?"A foundation exercise from our original learning path":"Builds on your recorded practice in a related topic; self-reported results remain unchanged");
            items.add(copy);
        }
        db.update("update coach_drafts set items=?::jsonb where id=?::uuid and owner=?",db.json(items),draft,user);
        return db.owned("drafts",draft,user,false);
    }
    void validateItems(List<Map<String,Object>> items,int budget){
        if(items==null||items.isEmpty()||items.size()>12)throw new IllegalArgumentException();
        int total=0;Set<Long> seen=new HashSet<>();Set<Long> ids=new HashSet<>();for(var c:catalog())ids.add(((Number)c.get("id")).longValue());
        for(var item:items){long pid=number(item,"problemId",-1);long minutes=number(item,"minutes",0);if(!ids.contains(pid)||!seen.add(pid)||minutes<1||minutes>240)throw new IllegalArgumentException();total+=minutes;}
        if(total>budget)throw new IllegalArgumentException("Over budget");
    }
    public Map<String,Object> confirm(String user,String draft,Map<String,Object> input){
        if(!"CONFIRM".equals(input.get("confirmation"))||text(input,"idempotencyKey",100).length()<8)throw new IllegalArgumentException("Explicit confirmation required");
        var d=db.owned("drafts",draft,user,true);check(number(input,"revision",-1)==number(d,"revision",0),"STALE_REVISION");
        if(d.get("sessionId")!=null)return session(user,d.get("sessionId").toString());
        check(!Boolean.TRUE.equals(d.get("cancelled")),"DRAFT_CANCELLED");
        check(Instant.parse(d.get("expiresAt").toString()).isAfter(clock.instant()),"DRAFT_EXPIRED");
        var items=maps(d.get("items"));validateItems(items,((Number)d.get("budgetMinutes")).intValue());
        String sid=id();String title=d.get("focus").toString().isBlank()?"A little practice":d.get("focus").toString();
        db.update("insert into coach_sessions(id,owner,draft_id,title,focus,timezone,snapshot) values (?::uuid,?,?::uuid,?,?,?,?::jsonb)",sid,user,draft,title,d.get("focus"),profile(user).get("timezone"),db.json(items));
        var catalog=catalog();int position=0;
        for(var item:items){
            long pid=number(item,"problemId",-1);var problem=catalog.stream().filter(c->((Number)c.get("id")).longValue()==pid).findFirst().orElseThrow();
            String iid=id();db.update("insert into coach_items(id,owner,session_id,problem_id,snapshot,position,minutes) values (?::uuid,?,?::uuid,?,?::jsonb,?,?)",iid,user,sid,pid,db.json(problem),position++,number(item,"minutes",10));
            db.update("insert into coach_attempts(id,owner,item_id) values (?::uuid,?,?::uuid)",id(),user,iid);
        }
        db.update("insert into coach_journals(id,owner,session_id,title) values (?::uuid,?,?::uuid,?)",id(),user,sid,title);
        db.update("update coach_drafts set session_id=?::uuid where id=?::uuid and owner=?",sid,draft,user);return session(user,sid);
    }
    public Map<String,Object> session(String user,String id){
        var s=db.owned("sessions",id,user,false);var items=db.rows("select * from coach_items where session_id=?::uuid and owner=? order by position",id,user);
        for(var item:items){item.put("problem",item.get("snapshot"));item.put("attempt",db.one("select * from coach_attempts where item_id=?::uuid and owner=?",item.get("id"),user));}
        s.put("items",items);s.put("journal",db.one("select * from coach_journals where session_id=?::uuid and owner=?",id,user));return s;
    }
    public Map<String,Object> workspace(String user,String item){
        var i=db.owned("items",item,user,false);i.put("problem",i.get("snapshot"));
        var attempt=db.one("select * from coach_attempts where item_id=?::uuid and owner=?",item,user);i.put("attempt",attempt);
        i.put("hints",db.rows("select * from coach_hints where attempt_id=?::uuid and owner=? order by created_at",attempt.get("id"),user));
        i.put("history",evidence(user).stream().filter(a->a.get("problemId").equals(i.get("problemId"))&&!a.get("id").equals(attempt.get("id"))).toList());return i;
    }
    public Map<String,Object> feedback(String user,String id,Map<String,Object> input){
        var a=db.owned("attempts",id,user,true);check(number(input,"version",-1)==number(a,"version",0),"STALE_VERSION");
        Object result=input.containsKey("result")?input.get("result"):a.get("result");if(result!=null&&!Set.of("independent","hinted","solution","understanding").contains(result))throw new IllegalArgumentException();
        Object minutes=input.containsKey("actualMinutes")?input.get("actualMinutes"):a.get("actualMinutes");if(minutes!=null){long exact=number(Map.of("minutes",minutes),"minutes",0);if(exact<0||exact>1440)throw new IllegalArgumentException();minutes=exact;}
        boolean completed=Boolean.TRUE.equals(input.containsKey("completed")?input.get("completed"):a.get("completed"));
        db.update("update coach_attempts set completed=?,result=?,actual_minutes=?,notes=?,feelings=?,version=version+1,first_completed_at=case when ? then coalesce(first_completed_at,now()) else first_completed_at end,updated_at=now() where id=?::uuid and owner=?",completed,result,minutes,input.containsKey("notes")?text(input,"notes",12000):a.get("notes"),input.containsKey("feelings")?text(input,"feelings",4000):a.get("feelings"),completed,id,user);
        db.update("update coach_items set state=? where id=?::uuid and owner=?",completed?"completed":"in_progress",a.get("itemId"),user);
        db.update("update coach_sessions set version=version+1 where id=(select session_id from coach_items where id=?::uuid) and owner=?",a.get("itemId"),user);
        return db.owned("attempts",id,user,false);
    }
    public List<Map<String,Object>> journals(String user,String query,int page){
        if(page<0||page>10000)throw new IllegalArgumentException();String q="%"+(query==null?"":query)+"%";
        return db.rows("select j.*,s.started_at,s.ended_at,s.timezone,s.focus,s.state from coach_journals j join coach_sessions s on s.id=j.session_id where j.owner=? and (j.title ilike ? or j.body ilike ? or exists(select 1 from coach_items i join coach_attempts a on a.item_id=i.id where i.session_id=s.id and (i.snapshot::text ilike ? or a.notes ilike ?))) order by s.started_at desc limit 20 offset ?",user,q,q,q,q,page*20);
    }
    public Map<String,Object> journal(String user,String id){var j=db.owned("journals",id,user,false);j.put("session",session(user,j.get("sessionId").toString()));return j;}
    public Map<String,Object> saveJournal(String user,String id,Map<String,Object> input){
        var j=db.owned("journals",id,user,true);check(number(input,"version",-1)==number(j,"version",0),"STALE_VERSION");
        db.update("update coach_journals set title=?,body=?,feelings=?,version=version+1,updated_at=now() where id=?::uuid and owner=?",text(input,"title",200),text(input,"body",20000),text(input,"feelings",4000),id,user);return db.owned("journals",id,user,false);
    }
    public Map<String,Object> state(String user,String id,String state){db.owned("sessions",id,user,true);if(!Set.of("active","paused","ended").contains(state))throw new IllegalArgumentException();db.update("update coach_sessions set state=?,ended_at=case when ?='ended' then now() else null end,version=version+1 where id=?::uuid and owner=?",state,state,id,user);return session(user,id);}
    public List<Map<String,Object>> memories(String user){return db.rows("select * from coach_memories where owner=? order by created_at",user);}
    public Map<String,Object> memory(String user,String id,Map<String,Object> input){String content=text(input,"content",2000);if(content.isBlank())throw new IllegalArgumentException();
        if(id==null){id=id();db.update("insert into coach_memories(id,owner,content) values (?::uuid,?,?)",id,user,content);}else{db.owned("memories",id,user,true);db.update("update coach_memories set content=?,version=version+1 where id=?::uuid and owner=?",content,id,user);db.update("update coach_users set profile=jsonb_set(profile,'{memoryEpoch}',to_jsonb(?::text),true) where username=?",clock.instant().toString(),user);}return db.owned("memories",id,user,false);}
    public void deleteMemory(String user,String id){db.owned("memories",id,user,true);db.update("delete from coach_memories where id=?::uuid and owner=?",id,user);
        db.update("update coach_users set profile=jsonb_set(profile,'{memoryEpoch}',to_jsonb(?::text),true) where username=?",clock.instant().toString(),user);}
    public List<Map<String,Object>> growth(String user){return db.rows("select s.id,s.title,s.started_at,s.ended_at,s.timezone,s.state,j.id as journal_id,greatest(count(a.first_completed_at),case when length(j.body)>0 or exists(select 1 from coach_items ni join coach_attempts na on na.item_id=ni.id where ni.session_id=s.id and (length(na.notes)>0 or na.result is not null or na.actual_minutes is not null)) or exists(select 1 from coach_hints h join coach_attempts ha on ha.id=h.attempt_id join coach_items hi on hi.id=ha.item_id where hi.session_id=s.id) then 1 else 0 end) as leaves from coach_sessions s join coach_journals j on j.session_id=s.id left join coach_items i on i.session_id=s.id left join coach_attempts a on a.item_id=i.id where s.owner=? group by s.id,j.id order by s.started_at",user);}
    public Map<String,Object> bootstrap(String user){var result=new LinkedHashMap<String,Object>();
        result.put("profile",profile(user));result.put("catalog",catalog());result.put("conversations",db.rows("select * from coach_conversations where owner=? order by created_at desc",user));
        result.put("sessions",db.rows("select * from coach_sessions where owner=? order by started_at desc",user));result.put("journals",journals(user,"",0));
        result.put("memories",memories(user));result.put("growth",growth(user));result.put("skills",skills(user));result.put("reviews",reviews(user));
        result.put("changePreviews",db.rows("select * from coach_changes where owner=? and not confirmed and not cancelled order by created_at desc limit 20",user));
        result.put("drafts",db.rows("select * from coach_drafts where owner=? and session_id is null and not cancelled and expires_at>now() order by created_at desc limit 20",user));return result;}
    public Map<String,Object> export(String user){var all=new LinkedHashMap<>(bootstrap(user));
        all.put("sessions",db.rows("select id from coach_sessions where owner=? order by started_at",user).stream().map(row->session(user,row.get("id").toString())).toList());
        all.put("journals",db.rows("select * from coach_journals where owner=? order by updated_at",user));
        all.put("attempts",evidence(user));all.put("messages",db.rows("select * from coach_messages where owner=? order by created_at",user));all.put("drafts",db.rows("select * from coach_drafts where owner=?",user));all.put("hints",db.rows("select * from coach_hints where owner=?",user));all.put("changes",db.rows("select * from coach_changes where owner=?",user));all.put("tutoringCorrections",db.rows("select * from coach_hint_reports where owner=?",user));return all;}
    public void deleteAccount(String user,String confirmation){if(!"DELETE_MY_ACCOUNT".equals(confirmation))throw new IllegalArgumentException();db.update("delete from coach_users where username=?",user);}

    public Map<String,Object> proposeChange(String user,String sessionId,Map<String,Object> input){
        var session=session(user,sessionId);List<Map<String,Object>> desired=maps(input.get("items"));
        if(desired==null||desired.size()>12)throw new IllegalArgumentException();
        List<Map<String,Object>> safe=new ArrayList<>();Set<String> seen=new HashSet<>();var allowed=catalog();
        for(var item:desired){
            if(item.get("sessionItemId")!=null){var old=db.owned("items",item.get("sessionItemId").toString(),user,false);
                check(old.get("sessionId").equals(sessionId),"WRONG_SESSION");if(!seen.add(old.get("id").toString()))throw new IllegalArgumentException();
                safe.add(Map.of("sessionItemId",old.get("id")));}
            else {long pid=number(item,"problemId",-1);check(allowed.stream().anyMatch(c->number(c,"id",-2)==pid),"INVALID_PROBLEM");
                int minutes=(int)number(item,"minutes",10);if(minutes<1||minutes>240)throw new IllegalArgumentException();safe.add(Map.of("problemId",pid,"minutes",minutes));}
        }
        long originalBudget=maps(session.get("snapshot")).stream().mapToLong(item->number(item,"minutes",0)).sum();
        long budget=number(input,"budgetMinutes",originalBudget);if(budget<1||budget>240)throw new IllegalArgumentException("Invalid budget");
        var existing=maps(session.get("items"));Set<Long> selectedProblems=new HashSet<>();long total=0;
        for(var item:existing)if(Boolean.TRUE.equals(((Map<?,?>)item.get("attempt")).get("completed"))){selectedProblems.add(number(item,"problemId",-1));total+=number(item,"minutes",0);}
        for(var item:safe){if(item.get("sessionItemId")!=null){var old=existing.stream().filter(e->e.get("id").equals(item.get("sessionItemId"))).findFirst().orElseThrow();if(Boolean.TRUE.equals(((Map<?,?>)old.get("attempt")).get("completed")))continue;if(!selectedProblems.add(number(old,"problemId",-1)))throw new IllegalArgumentException("Duplicate problem");total+=number(old,"minutes",0);}
            else {if(!selectedProblems.add(number(item,"problemId",-1)))throw new IllegalArgumentException("Duplicate problem");total+=number(item,"minutes",0);}}
        if(total>budget)throw new IllegalArgumentException("Over budget");
        String id=id();db.update("insert into coach_changes(id,owner,session_id,revision,items) values (?::uuid,?,?::uuid,?,?::jsonb)",id,user,sessionId,session.get("version"),db.json(safe));
        var preview=db.owned("changes",id,user,false);preview.put("original",session);preview.put("notice","Completed items and all notes are retained; omitted incomplete items are deferred only after confirmation.");return preview;
    }
    public Map<String,Object> confirmChange(String user,String id,Map<String,Object> input){
        if(!"CONFIRM".equals(input.get("confirmation"))||text(input,"idempotencyKey",100).length()<8)throw new IllegalArgumentException();
        var change=db.owned("changes",id,user,true);String sid=change.get("sessionId").toString();
        var session=db.owned("sessions",sid,user,true);check(!Boolean.TRUE.equals(change.get("cancelled")),"CHANGE_CANCELLED");if(Boolean.TRUE.equals(change.get("confirmed")))return session(user,sid);
        check(number(input,"revision",-1)==number(change,"revision",-2)&&number(session,"version",-3)==number(change,"revision",-2),"STALE_REVISION");
        Set<String> keep=new HashSet<>();var desired=maps(change.get("items"));
        for(var item:desired)if(item.get("sessionItemId")!=null)keep.add(item.get("sessionItemId").toString());
        var old=db.rows("select i.*,a.completed from coach_items i join coach_attempts a on a.item_id=i.id where i.session_id=?::uuid and i.owner=?",sid,user);
        for(var item:old)if(!Boolean.TRUE.equals(item.get("completed"))&&!keep.contains(item.get("id").toString()))db.update("update coach_items set state='deferred' where id=?::uuid and owner=?",item.get("id"),user);
        int position=old.size();var allowed=catalog();
        for(var item:desired)if(item.get("sessionItemId")==null){long pid=number(item,"problemId",-1);var problem=allowed.stream().filter(c->number(c,"id",-2)==pid).findFirst().orElseThrow();String iid=id();
            db.update("insert into coach_items(id,owner,session_id,problem_id,snapshot,position,minutes) values (?::uuid,?,?::uuid,?,?::jsonb,?,?)",iid,user,sid,pid,db.json(problem),position++,item.get("minutes"));
            db.update("insert into coach_attempts(id,owner,item_id) values (?::uuid,?,?::uuid)",id(),user,iid);}
        db.update("update coach_changes set confirmed=true where id=?::uuid and owner=?",id,user);db.update("update coach_sessions set version=version+1 where id=?::uuid and owner=?",sid,user);return session(user,sid);
    }
    public Map<String,Object> recap(String user,String from,String to){
        LocalDate start=LocalDate.parse(from),end=LocalDate.parse(to);if(end.isBefore(start)||end.isAfter(start.plusYears(1)))throw new IllegalArgumentException();
        ZoneId zone=ZoneId.of(profile(user).get("timezone").toString());
        var selected=evidence(user).stream().filter(a->{if(a.get("firstCompletedAt")==null)return false;LocalDate date=Instant.parse(a.get("firstCompletedAt").toString()).atZone(zone).toLocalDate();return !date.isBefore(start)&&!date.isAfter(end);}).toList();
        var results=new LinkedHashMap<String,Long>();for(String value:List.of("independent","hinted","solution","understanding"))results.put(value,selected.stream().filter(a->Boolean.TRUE.equals(a.get("completed"))&&value.equals(a.get("result"))).count());
        var notes=selected.stream().filter(a->!a.get("notes").toString().isBlank()).map(a->Map.of("attemptId",a.get("id"),"problemId",a.get("problemId"),"originalNotes",a.get("notes"))).toList();
        return Map.of("from",from,"to",to,"timezone",zone.toString(),"practiceCount",selected.size(),"evidenceIds",selected.stream().map(a->a.get("id")).toList(),"results",results,"notes",notes,"observations","This on-demand review includes only your recorded practice in the selected period. Results are self-reported, not a score.");
    }

    public Map<String,Object> recapFromMessage(String user,String message){
        LocalDate today=LocalDate.now(clock.withZone(ZoneId.of(profile(user).get("timezone").toString()))),from,to=today;
        var dates=java.util.regex.Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(message);var found=new ArrayList<String>();while(dates.find())found.add(dates.group(1));
        String text=message.toLowerCase(Locale.ROOT);
        if(found.size()==2)return recap(user,found.get(0),found.get(1));
        if(text.contains("上周")||text.contains("last week")){to=today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusDays(1);from=to.minusDays(6);}
        else if(text.contains("本周")||text.contains("this week"))from=today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        else if(text.contains("过去7天")||text.contains("last 7 days"))from=today.minusDays(6);
        else if(text.contains("今天")||text.contains("today"))from=today;
        else return null;
        return recap(user,from.toString(),to.toString());
    }
    public Map<String,Object> draftDetail(String user,String id){return db.owned("drafts",id,user,false);}
    public Map<String,Object> interviewPlan(String user){
        var profile=profile(user);ZoneId zone=ZoneId.of(profile.get("timezone").toString());LocalDate today=LocalDate.now(clock.withZone(zone));
        String target=profile.getOrDefault("targetDate","").toString();LocalDate end=target.isBlank()?today.plusDays(13):LocalDate.parse(target);
        long days=Math.max(0,java.time.temporal.ChronoUnit.DAYS.between(today,end)+1);
        var topics=catalog().stream().flatMap(c->maps(c.get("patterns")).stream()).map(pattern->pattern.get("name")).distinct().toList();
        var phases=new ArrayList<Map<String,Object>>();String[] labels={"Foundation and small examples","Practice core patterns","Review recorded difficulties and explain trade-offs"};
        if(days>0){int segments=(int)Math.min(3,days);for(int n=0;n<segments;n++){
            long from=days*n/segments,to=days*(n+1)/segments-1;
            phases.add(Map.of("from",today.plusDays(from).toString(),"to",today.plusDays(to).toString(),"focus",labels[n],"minutesPerSession",profile.get("budgetMinutes"),"topics",topics));
        }}
        return Map.of("targetDate",target,"planningWindowDays",days,"budgetMinutes",profile.get("budgetMinutes"),"topics",topics,"phases",phases,
            "assumption",target.isBlank()?"An adjustable 14-day planning window; no target date was saved":"Use your selected target date; choose session days yourself",
            "notice","A general interview foundation path, not company-specific questions or an outcome guarantee. No absence or streak assessment.");
    }

    public void cancelDraft(String user,String id){var d=db.owned("drafts",id,user,true);check(d.get("sessionId")==null,"ALREADY_CONFIRMED");db.update("update coach_drafts set cancelled=true where id=?::uuid and owner=?",id,user);}
    public void cancelChange(String user,String id){var c=db.owned("changes",id,user,true);check(!Boolean.TRUE.equals(c.get("confirmed")),"ALREADY_CONFIRMED");db.update("update coach_changes set cancelled=true where id=?::uuid and owner=?",id,user);}

    public Map<String,Object> reportTutoring(String user,String message,Map<String,Object> input){
        var m=db.owned("messages",message,user,false);check(m.get("reply")!=null,"NO_COACH_RESPONSE");
        String note=text(input,"note",2000),request=text(input,"requestId",100);if(note.isBlank()||request.length()<8)throw new IllegalArgumentException();
        db.update("insert into coach_hint_reports(id,owner,message_id,request_id,note) values (?::uuid,?,?::uuid,?,?) on conflict(owner,message_id,request_id) do nothing",id(),user,message,request,note);
        return db.one("select * from coach_hint_reports where owner=? and message_id=?::uuid and request_id=?",user,message,request);
    }
}
