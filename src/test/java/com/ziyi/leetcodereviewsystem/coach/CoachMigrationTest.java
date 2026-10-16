package com.ziyi.leetcodereviewsystem.coach;

import com.ziyi.leetcodereviewsystem.LocalTestDatabaseConfiguration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(LocalTestDatabaseConfiguration.class)
class CoachMigrationTest {
    @Autowired DataSource source;
    @Test void additiveUpgradePreservesSyntheticLegacyRowsWithoutInventingSessions(){
        JdbcTemplate jdbc=new JdbcTemplate(source);String schema="legacy_fixture_"+UUID.randomUUID().toString().replace("-","");
        Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).target("1").cleanDisabled(true).load().migrate();
        jdbc.update("insert into "+schema+".problems(id,title,difficulty,pattern,notes,times_reviewed,solved,last_reviewed,next_review_date) values (927,'Synthetic legacy','Easy','Legacy','Original synthetic notes',5,true,'2026-01-01','2026-01-31')");
        jdbc.update("insert into "+schema+".review_sessions(id,problem_id,reviewed_at,next_review_date) values (916,927,'2026-01-01 12:00:00','2026-01-31')");
        var before=jdbc.queryForMap("select * from "+schema+".problems where id=927");
        var history=jdbc.queryForMap("select * from "+schema+".review_sessions where id=916");
        var migrated=Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).cleanDisabled(true).load();migrated.migrate();migrated.migrate();
        assertEquals(before,jdbc.queryForMap("select * from "+schema+".problems where id=927"));
        assertEquals(history,jdbc.queryForMap("select * from "+schema+".review_sessions where id=916"));
        assertEquals(0,jdbc.queryForObject("select count(*) from "+schema+".coach_sessions",Integer.class));
        assertEquals(0,jdbc.queryForObject("select count(*) from "+schema+".coach_journals",Integer.class));
        // Keep the tiny synthetic fixture for inspection; no DROP/TRUNCATE/clean is executed.
    }
}
