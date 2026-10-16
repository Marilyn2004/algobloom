package com.ziyi.leetcodereviewsystem.coach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CoachStagingConfigurationTest {
    final String host="ep-synthetic-example.us-west-2.aws.neon.tech";
    String jdbc(){return "jdbc:postgresql://"+host+":5432/neondb?sslmode=verify-full";}
    @Test void acceptsOnlyPinnedStagingTarget(){
        assertDoesNotThrow(()->CoachStagingConfiguration.validateTarget("true","srv-stage","srv-stage",host,jdbc()));
        String regional="ep-synthetic-example.c-2.us-west-2.aws.neon.tech";
        assertDoesNotThrow(()->CoachStagingConfiguration.validateTarget("true","srv-stage","srv-stage",regional,"jdbc:postgresql://"+regional+":5432/neondb?sslmode=verify-full"));
    }
    @Test void rejectsWrongPlatformAndServiceBeforePoolInitialization(){
        assertThrows(IllegalStateException.class,()->CoachStagingConfiguration.validateTarget("false","srv-stage","srv-stage",host,jdbc()));
        assertThrows(IllegalStateException.class,()->CoachStagingConfiguration.validateTarget("true","srv-production","srv-stage",host,jdbc()));
    }
    @Test void rejectsProductionHostAndJdbcOverrides(){
        for(String url:new String[]{"jdbc:postgresql://production.railway.internal:5432/railway",jdbc()+"&host=production","jdbc:postgresql://"+host+":5432/other?sslmode=verify-full"})assertThrows(IllegalStateException.class,()->CoachStagingConfiguration.validateTarget("true","srv-stage","srv-stage",host,url));
        assertThrows(IllegalStateException.class,()->CoachStagingConfiguration.validateTarget("true","srv-stage","srv-stage","production.railway.internal",jdbc()));
    }
}
