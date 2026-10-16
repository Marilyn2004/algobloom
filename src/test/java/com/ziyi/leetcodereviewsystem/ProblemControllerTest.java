package com.ziyi.leetcodereviewsystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProblemController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProblemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProblemService problemService;

    @Test
    void createProblemRejectsInvalidBody() throws Exception {
        mockMvc.perform(post("/problems")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","difficulty":"Expert","pattern":"","timesReviewed":-1,"solved":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void updateProblemRejectsInvalidBody() throws Exception {
        mockMvc.perform(put("/problems/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Two Sum","difficulty":"Easy","pattern":"","timesReviewed":0,"solved":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getProblemReturnsNotFoundWhenMissing() throws Exception {
        when(problemService.findProblemById(999)).thenThrow(new ProblemNotFoundException(999));

        mockMvc.perform(get("/problems/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Problem not found: 999"));
    }

    @Test
    void updateProblemReturnsNotFoundWhenMissing() throws Exception {
        when(problemService.updateProblem(eq(999), any(Problem.class)))
                .thenThrow(new ProblemNotFoundException(999));

        mockMvc.perform(put("/problems/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Two Sum","difficulty":"Easy","pattern":"HashMap","timesReviewed":0,"solved":false}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void reviewProblemReturnsNotFoundWhenMissing() throws Exception {
        when(problemService.markReviewed(999)).thenThrow(new ProblemNotFoundException(999));

        mockMvc.perform(post("/problems/999/review"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProblemReturnsNotFoundWhenMissing() throws Exception {
        doThrow(new ProblemNotFoundException(999)).when(problemService).deleteProblemById(999);

        mockMvc.perform(delete("/problems/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProblemReturnsNoContentWhenSuccessful() throws Exception {
        mockMvc.perform(delete("/problems/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void listProblemsPassesCombinedFilters() throws Exception {
        when(problemService.getProblems("Medium", "Two Pointers", true)).thenReturn(List.of());

        mockMvc.perform(get("/problems")
                        .param("difficulty", "Medium")
                        .param("pattern", "Two Pointers")
                        .param("solved", "true"))
                .andExpect(status().isOk());

        verify(problemService).getProblems("Medium", "Two Pointers", true);
    }

    @Test
    void listProblemsWithoutQueryParamsReturnsAll() throws Exception {
        when(problemService.getProblems(isNull(), isNull(), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/problems"))
                .andExpect(status().isOk());

        verify(problemService).getProblems(null, null, null);
    }

    @Test
    void getReviewHistoryReturnsNotFoundWhenMissing() throws Exception {
        when(problemService.getReviewHistory(999)).thenThrow(new ProblemNotFoundException(999));

        mockMvc.perform(get("/problems/999/reviews"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
