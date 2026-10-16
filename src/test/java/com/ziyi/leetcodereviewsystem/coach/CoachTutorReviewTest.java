package com.ziyi.leetcodereviewsystem.coach;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CoachTutorReviewTest {
    CoachProvider provider=mock(CoachProvider.class);
    CoachProvider.Reply reply(String text){return new CoachProvider.Reply(text,List.of(),false);}
    CoachProvider.Reply assessment(boolean safe){return new CoachProvider.Reply("",List.of(new CoachProvider.ToolCall("review","assess_tutoring","{\"safe\":"+safe+",\"reason\":\"synthetic assessment\"}")),false);}
    CoachProvider.Context context(){return new CoachProvider.Context("Original tutor policy","Current learner question",List.of());}
    @Test void safeReplyPassesWithoutRewriting(){
        when(provider.respond(any())).thenReturn(assessment(true));var initial=reply("One small question?");
        var checked=new CoachTutorReview(provider,true).check(initial,context(),"hint",1,false,"reviewed facts");
        assertTrue(checked.accepted());assertEquals(initial,checked.reply());verify(provider).respond(any());
    }
    @Test void unsafeReplyIsRewrittenOnceAndRecheckedWithinSameDeadline(){
        when(provider.respond(any())).thenReturn(assessment(false),reply("What does your small example show?"),assessment(true));
        var context=context();var checked=new CoachTutorReview(provider,true).check(reply("full algorithm"),context,"hint",1,false,"reviewed facts");
        assertTrue(checked.accepted());assertEquals("What does your small example show?",checked.reply().text());
        var c=org.mockito.ArgumentCaptor.forClass(CoachProvider.Context.class);verify(provider,times(3)).respond(c.capture());
        assertTrue(c.getAllValues().stream().allMatch(x->x.deadline().equals(context.deadline())));
        assertEquals(List.of("assess_tutoring"),c.getAllValues().getFirst().tools().stream().map(t->t.get("name")).toList());
    }
    @Test void repeatedUnsafeOrMalformedReviewDoesNotCountAsHint(){
        when(provider.respond(any())).thenReturn(reply("malformed review"),reply("still unsafe"),assessment(false));
        var checked=new CoachTutorReview(provider,true).check(reply("unchecked code"),context(),"hint",2,false,"facts");
        assertFalse(checked.accepted());assertFalse(checked.reply().text().contains("unchecked code"));verify(provider,times(3)).respond(any());
    }
    @Test void reviewFailureFallsBackWithoutExposingUpstreamDetails(){
        when(provider.respond(any())).thenThrow(new IllegalStateException("synthetic-secret-detail"));
        var checked=new CoachTutorReview(provider,true).check(reply("unchecked"),context(),"debug",0,false,"facts");
        assertFalse(checked.accepted());assertFalse(checked.reply().text().contains("synthetic-secret-detail"));
    }
    @Test void explicitCurrentRevealAndClearlyMarkedDemoDoNotNeedNonRevealReview(){
        var reviewer=new CoachTutorReview(provider,true);
        assertTrue(reviewer.check(reply("explicit original solution"),context(),"solution",4,true,"facts").accepted());
        assertTrue(reviewer.check(new CoachProvider.Reply("DEMO",List.of(),true),context(),"hint",1,false,"facts").accepted());
        verifyNoInteractions(provider);
    }
}
