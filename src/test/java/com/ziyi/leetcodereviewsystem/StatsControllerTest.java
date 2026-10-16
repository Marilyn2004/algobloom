package com.ziyi.leetcodereviewsystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
@AutoConfigureMockMvc(addFilters = false)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProblemService problemService;

    @Test
    void getStatsReturnsStructuredJson() throws Exception {
        when(problemService.getStats()).thenReturn(new StatsResponse(
                100,
                70,
                12,
                250,
                Map.of("Easy", 20L, "Medium", 50L, "Hard", 30L),
                Map.of("Arrays", 20L, "Two Pointers", 15L)
        ));

        mockMvc.perform(get("/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProblems").value(100))
                .andExpect(jsonPath("$.solvedProblems").value(70))
                .andExpect(jsonPath("$.dueProblems").value(12))
                .andExpect(jsonPath("$.totalReviews").value(250))
                .andExpect(jsonPath("$.difficultyBreakdown.Easy").value(20))
                .andExpect(jsonPath("$.patternBreakdown.Arrays").value(20));
    }
}
