package com.fsl.quizapp.quiz;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for quiz endpoints against seeded H2 data. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Rollback
class QuizIntegrationTest {

  @Autowired
  MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** GET /quizzes returns both seeded quizzes. */
  @Test
  void listQuizzes_returnsBothSeededQuizzes() throws Exception {
    mockMvc.perform(get("/quizzes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[?(@.id == 'bbbbbbbb-0000-0000-0000-000000000001')].title")
            .value("LLM Fundamentals"))
        .andExpect(jsonPath("$[?(@.id == 'bbbbbbbb-0000-0000-0000-000000000002')].title")
            .value("Agent Design Patterns"));
  }

  /** GET /quizzes response does not include questions field. */
  @Test
  void listQuizzes_doesNotIncludeQuestions() throws Exception {
    mockMvc.perform(get("/quizzes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].questions").doesNotExist())
        .andExpect(jsonPath("$[1].questions").doesNotExist());
  }

  /** GET /quizzes/{id} returns 200 with quiz detail for seeded quiz 1. */
  @Test
  void getQuizDetails_returnsSeededQuiz1() throws Exception {
    String id = "bbbbbbbb-0000-0000-0000-000000000001";

    mockMvc.perform(get("/quizzes/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.title").value("LLM Fundamentals"))
        .andExpect(jsonPath("$.questions").isArray())
        .andExpect(jsonPath("$.questions.length()").value(5));
  }

  /** GET /quizzes/{id} returns questions ordered by position ascending. */
  @Test
  void getQuizDetails_questionsOrderedByPosition() throws Exception {
    String id = "bbbbbbbb-0000-0000-0000-000000000001";

    mockMvc.perform(get("/quizzes/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.questions[0].position").value(1))
        .andExpect(jsonPath("$.questions[1].position").value(2))
        .andExpect(jsonPath("$.questions[2].position").value(3));
  }

  /** GET /quizzes/{id} response does not expose correctOptionId or explanation. */
  @Test
  void getQuizDetails_doesNotExposeCorrectOptionIdOrExplanation() throws Exception {
    String id = "bbbbbbbb-0000-0000-0000-000000000001";

    mockMvc.perform(get("/quizzes/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.questions[0].correctOptionId").doesNotExist())
        .andExpect(jsonPath("$.questions[0].explanation").doesNotExist());
  }

  /** GET /quizzes/{id} returns 404 for unknown UUID. */
  @Test
  void getQuizDetails_returns404ForUnknownId() throws Exception {
    mockMvc.perform(get("/quizzes/00000000-0000-0000-0000-000000000000"))
        .andExpect(status().isNotFound());
  }
}
