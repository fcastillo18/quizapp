package com.fsl.quizapp.attempt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsl.quizapp.attempt.dto.StartAttemptRequest;
import com.fsl.quizapp.attempt.repository.AttemptRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for the start-quiz-attempt endpoint. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AttemptIntegrationTest {

  private static final UUID QUIZ_1_ID =
      UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
  private static final UUID USER_1_ID =
      UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
  private static final UUID UNKNOWN_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000000");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private AttemptRepository attemptRepository;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Verifies that a valid request returns 201 with a Location header and the expected response
   * body including questions without correctOptionId.
   */
  @Test
  @Rollback
  void startAttempt_validRequest_returns201WithLocationAndQuestions() throws Exception {
    MvcResult result = mockMvc.perform(
            post("/quizzes/{quizId}/attempts", QUIZ_1_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new StartAttemptRequest(USER_1_ID))))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.attemptId").isNotEmpty())
        .andExpect(jsonPath("$.quizId").value(QUIZ_1_ID.toString()))
        .andExpect(jsonPath("$.userId").value(USER_1_ID.toString()))
        .andExpect(jsonPath("$.startedAt").isNotEmpty())
        .andExpect(jsonPath("$.questions").isArray())
        .andExpect(jsonPath("$.questions[0].id").isNotEmpty())
        .andExpect(jsonPath("$.questions[0].text").isNotEmpty())
        .andExpect(jsonPath("$.questions[0].position").isNumber())
        .andExpect(jsonPath("$.questions[0].options").isArray())
        .andExpect(jsonPath("$.questions[0].options[0].id").isNotEmpty())
        .andExpect(jsonPath("$.questions[0].options[0].text").isNotEmpty())
        .andExpect(jsonPath("$.questions[0].options[0].position").isNumber())
        .andReturn();

    String body = result.getResponse().getContentAsString();
    assertThat(body).doesNotContain("correctOptionId");
    assertThat(body).doesNotContain("explanation");
  }

  /**
   * Verifies that two calls with the same quizId and userId create two distinct attempt rows.
   */
  @Test
  @Rollback
  void startAttempt_calledTwice_createsTwoDistinctRows() throws Exception {
    String requestBody =
        objectMapper.writeValueAsString(new StartAttemptRequest(USER_1_ID));

    MvcResult first = mockMvc.perform(
            post("/quizzes/{quizId}/attempts", QUIZ_1_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andReturn();

    MvcResult second = mockMvc.perform(
            post("/quizzes/{quizId}/attempts", QUIZ_1_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andReturn();

    String firstBody = first.getResponse().getContentAsString();
    String secondBody = second.getResponse().getContentAsString();

    String firstId = objectMapper.readTree(firstBody).get("attemptId").asText();
    String secondId = objectMapper.readTree(secondBody).get("attemptId").asText();

    assertThat(firstId).isNotEqualTo(secondId);
  }

  /** Verifies that an unknown quizId returns HTTP 404. */
  @Test
  void startAttempt_unknownQuizId_returns404() throws Exception {
    mockMvc.perform(
            post("/quizzes/{quizId}/attempts", UNKNOWN_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new StartAttemptRequest(USER_1_ID))))
        .andExpect(status().isNotFound());
  }

  /** Verifies that a missing userId returns HTTP 400. */
  @Test
  void startAttempt_missingUserId_returns400() throws Exception {
    mockMvc.perform(
            post("/quizzes/{quizId}/attempts", QUIZ_1_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\": null}"))
        .andExpect(status().isBadRequest());
  }
}
