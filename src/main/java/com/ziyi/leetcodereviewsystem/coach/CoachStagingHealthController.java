package com.ziyi.leetcodereviewsystem.coach;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@Profile("staging")
public class CoachStagingHealthController {
    private final JdbcTemplate jdbc;
    public CoachStagingHealthController(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @GetMapping("/health") public Map<String,String> health(){
        jdbc.queryForObject("select 1",Integer.class);
        return Map.of("status","ok","application","AlgoBloom");
    }
}
