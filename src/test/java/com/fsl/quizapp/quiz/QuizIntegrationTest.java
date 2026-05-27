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
}
