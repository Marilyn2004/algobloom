package com.ziyi.leetcodereviewsystem.coach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class CoachStagingPageControllerTest {
    @Test void clientRoutesForwardButMissingApiDoesNot() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new CoachStagingPageController()).build();
        for(String route:new String[]{"/coach","/journal/example","/sessions/one/problems/two","/growth","/settings"})mvc.perform(get(route)).andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/api/v2/missing")).andExpect(status().isNotFound());
    }
}
