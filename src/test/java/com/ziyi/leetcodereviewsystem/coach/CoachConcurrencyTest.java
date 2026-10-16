package com.ziyi.leetcodereviewsystem.coach;

import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class CoachConcurrencyTest {
    @Test void concurrentConfirmationsCommitOneSessionAndOneJournal() throws Exception {
        String schema="confirm_fixture_"+UUID.randomUUID().toString().replace("-","");
        // Explicit literal loopback target; no Spring/environment binding. Keep fixture, never clean.
        var source=new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:5432/leetcode_review_test?currentSchema="+schema,"marilyn","");
        Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).cleanDisabled(true).load().migrate();
        var db=new CoachStore(new JdbcTemplate(source));var learning=new CoachLearningService(db,Clock.systemUTC());
        var seed=new CoachCatalogInitializer(db);var tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        String user="concurrent_"+UUID.randomUUID();
        tx.executeWithoutResult(s->{db.update("insert into coach_users(username,password_hash) values (?,?)",user,"synthetic-unused-hash");seed.seed();});
        var conversation=learning.conversation(user);var d=learning.propose(user,conversation.get("id").toString(),20,"Synthetic concurrency","");
        var input=Map.<String,Object>of("revision",d.get("revision"),"confirmation","CONFIRM","idempotencyKey","same-confirmation");
        ExecutorService executor=Executors.newFixedThreadPool(2);
        try {
            var one=executor.submit(()->tx.execute(s->learning.confirm(user,d.get("id").toString(),input)));
            var two=executor.submit(()->tx.execute(s->learning.confirm(user,d.get("id").toString(),input)));
            assertEquals(one.get(10,TimeUnit.SECONDS).get("id"),two.get(10,TimeUnit.SECONDS).get("id"));
            assertEquals(1,learning.journals(user,"",0).size());
            assertEquals(1,db.rows("select id from coach_sessions where owner=?",user).size());
        } finally {executor.shutdownNow();}
    }

    @Test void journalWriteFailureRollsBackWholeConfirmationAndRetryWorks(){
        String schema="atomic_fixture_"+UUID.randomUUID().toString().replace("-","");
        var source=new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:5432/leetcode_review_test?currentSchema="+schema,"marilyn","");
        Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).cleanDisabled(true).load().migrate();
        var jdbc=new JdbcTemplate(source);var db=new CoachStore(jdbc);var tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        var learning=new CoachLearningService(db,Clock.systemUTC());String user="atomic_"+UUID.randomUUID();
        tx.executeWithoutResult(t->{db.update("insert into coach_users(username,password_hash) values (?,?)",user,"synthetic-unused-hash");new CoachCatalogInitializer(db).seed();});
        var c=learning.conversation(user);var d=learning.propose(user,c.get("id").toString(),10,"","");
        var failing=new CoachLearningService(new CoachStore(jdbc){
            @Override public int update(String sql,Object...args){if(sql.startsWith("insert into coach_journals"))throw new org.springframework.dao.DataAccessResourceFailureException("SYNTHETIC_WRITE_FAILURE");return super.update(sql,args);}
        },Clock.systemUTC());
        var in=Map.<String,Object>of("revision",d.get("revision"),"confirmation","CONFIRM","idempotencyKey","atomic-confirmation");
        assertThrows(org.springframework.dao.DataAccessResourceFailureException.class,()->tx.execute(t->failing.confirm(user,d.get("id").toString(),in)));
        for(String table:List.of("sessions","items","attempts","journals"))assertTrue(db.rows("select id from coach_"+table+" where owner=?",user).isEmpty());
        assertNull(learning.draftDetail(user,d.get("id").toString()).get("sessionId"));
        assertNotNull(tx.execute(t->learning.confirm(user,d.get("id").toString(),in)));
        assertEquals(1,learning.journals(user,"",0).size());
    }

    @Test void delayedProviderReplyKeepsOriginalAttemptWhileAnotherItemChanges() throws Exception {
        String schema="late_fixture_"+UUID.randomUUID().toString().replace("-","");
        var source=new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:5432/leetcode_review_test?currentSchema="+schema,"marilyn","");
        Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).cleanDisabled(true).load().migrate();
        var db=new CoachStore(new JdbcTemplate(source));var manager=new DataSourceTransactionManager(source);var tx=new TransactionTemplate(manager);
        var learning=new CoachLearningService(db,Clock.systemUTC());String user="late_"+UUID.randomUUID();
        tx.executeWithoutResult(t->{db.update("insert into coach_users(username,password_hash) values (?,?)",user,"synthetic-unused-hash");new CoachCatalogInitializer(db).seed();});
        String conversation=learning.conversation(user).get("id").toString();var d=learning.propose(user,conversation,30,"","");
        var session=tx.execute(t->learning.confirm(user,d.get("id").toString(),Map.of("revision",d.get("revision"),"confirmation","CONFIRM","idempotencyKey","late-session-key")));
        var items=(List<Map<String,Object>>)session.get("items");var first=items.getFirst();var second=items.get(1);
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        CoachProvider provider=context->{entered.countDown();try{if(!release.await(5,TimeUnit.SECONDS))throw new IllegalStateException("synthetic timeout");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}return new CoachProvider.Reply("A local observation",List.of(),false);};
        var chat=new CoachChatService(db,learning,provider,manager);var executor=Executors.newSingleThreadExecutor();
        try{
            var reply=executor.submit(()->chat.send(user,conversation,Map.of("requestId","delayed-actual-request","content","small hint","intent","hint","sessionItemId",first.get("id"),"contextRevision",4)));
            assertTrue(entered.await(5,TimeUnit.SECONDS));
            var other=(Map<String,Object>)second.get("attempt");tx.executeWithoutResult(t->learning.feedback(user,other.get("id").toString(),Map.of("version",0,"notes","B stays independent")));
            release.countDown();var response=reply.get(5,TimeUnit.SECONDS);
            assertEquals(first.get("id"),((Map<?,?>)((Map<?,?>)response.get("message")).get("context")).get("sessionItemId"));
            var a=(Map<?,?>)learning.workspace(user,first.get("id").toString()).get("attempt");var b=(Map<?,?>)learning.workspace(user,second.get("id").toString()).get("attempt");
            assertEquals(1,a.get("highestHint"));assertEquals(0,b.get("highestHint"));assertEquals("B stays independent",b.get("notes"));
            assertFalse((Boolean)b.get("completed"));
        }finally{release.countDown();executor.shutdownNow();}
    }
}
