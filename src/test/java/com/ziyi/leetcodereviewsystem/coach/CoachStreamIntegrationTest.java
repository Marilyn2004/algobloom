package com.ziyi.leetcodereviewsystem.coach;

import com.ziyi.leetcodereviewsystem.LocalTestDatabaseConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.net.*;
import java.net.http.*;
import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="server.address=127.0.0.1")
@Import(LocalTestDatabaseConfiguration.class)
class CoachStreamIntegrationTest {
    @LocalServerPort int port;
    @Autowired CoachStore db;
    @Autowired CoachLearningService learning;
    @Autowired PasswordEncoder encoder;
    @MockitoBean CoachProvider provider;
    HttpClient client;String user,conversation,csrf,header;
    @BeforeEach void setup() throws Exception {
        user="stream_"+UUID.randomUUID();String password="Synthetic-only-password";
        db.update("insert into coach_users(username,password_hash) values (?,?)",user,encoder.encode(password));
        conversation=learning.conversation(user).get("id").toString();
        client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).connectTimeout(Duration.ofSeconds(3)).build();
        var token=client.send(request("/auth/csrf","GET",null),HttpResponse.BodyHandlers.ofString());
        var data=(Map<?,?>)db.parse(token.body());csrf=data.get("token").toString();header=data.get("headerName").toString();
        assertEquals(200,client.send(request("/auth/login","POST",Map.of("username",user,"password",password)),HttpResponse.BodyHandlers.ofString()).statusCode());
    }
    HttpRequest request(String path,String method,Object body){
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v2"+path)).timeout(Duration.ofSeconds(10));
        if(header!=null)request.header(header,csrf);
        return request.header("Content-Type","application/json").method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(db.json(body))).build();
    }
    @Test void disconnectAfterPendingStillPersistsOneReplyAndRetryRecovers() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        when(provider.respond(any())).thenAnswer(invocation->{entered.countDown();assertTrue(release.await(5,TimeUnit.SECONDS));return new CoachProvider.Reply("Persisted after stream disconnect",List.of(),false);});
        var body=Map.<String,Object>of("requestId","disconnected-stream-request","content","Explain a general concept","intent","concept");
        var response=client.send(request("/conversations/"+conversation+"/messages/stream","POST",body),HttpResponse.BodyHandlers.ofInputStream());
        assertEquals(200,response.statusCode());assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/event-stream"));
        try(var in=response.body()){assertEquals("event:status",new BufferedReader(new InputStreamReader(in)).readLine());assertTrue(entered.await(3,TimeUnit.SECONDS));}
        finally{release.countDown();}
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<deadline){var state=chatState();if("completed".equals(state.get("status")))break;Thread.sleep(20);}
        assertEquals("completed",chatState().get("status"));
        var retry=client.send(request("/conversations/"+conversation+"/messages/stream","POST",body),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,retry.statusCode());assertTrue(retry.body().contains("event:result"));assertTrue(retry.body().contains("Persisted after stream disconnect"));
        assertEquals(1,db.rows("select id from coach_messages where owner=?",user).size());verify(provider,times(1)).respond(any());
    }
    Map<String,Object> chatState(){
        var rows=db.rows("select status,reply from coach_messages where owner=?",user);
        return rows.isEmpty()?Map.of():rows.getFirst();
    }
    @Test void streamErrorsCarryStatusAndDoNotCreateALearningPage() throws Exception {
        when(provider.respond(any())).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"SYNTHETIC_LIMIT"));
        var reply=client.send(request("/conversations/"+conversation+"/messages/stream","POST",Map.of("requestId","stream-error-request","content","a general concept","intent","concept")),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,reply.statusCode());assertTrue(reply.body().contains("event:error"));assertTrue(reply.body().contains("503"));assertTrue(learning.journals(user,"",0).isEmpty());
    }
}
