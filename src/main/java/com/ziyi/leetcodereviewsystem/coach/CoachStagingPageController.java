package com.ziyi.leetcodereviewsystem.coach;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Only known client routes forward to the SPA; API failures remain API failures. */
@Controller
@Profile("staging")
public class CoachStagingPageController {
    @GetMapping({"/","/coach","/journal","/growth","/settings","/journal/{journalId}",
            "/sessions/{sessionId}","/sessions/{sessionId}/problems/{itemId}"})
    public String page(){return "forward:/index.html";}
}
