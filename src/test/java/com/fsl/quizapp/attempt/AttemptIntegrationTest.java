package com.fsl.quizapp.attempt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsl.quizapp.attempt.dto.StartAttemptRequest;
import com.fsl.quizapp.attempt.dto.SubmitAnswerRequest;
import com.fsl.quizapp.attempt.dto.SubmitAttemptRequest;
import com.fsl.quizapp.attempt.repository.AttemptRepository;
import com.fsl.quizapp.notification.NotificationService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for quiz attempt endpoints. */
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

  // Quiz 1 — correct option UUIDs from seed data
  private static final UUID Q1_ID =
      UUID.fromString("cccccccc-0000-0000-0000-000000000001");
  private static final UUID Q2_ID =
      UUID.fromString("cccccccc-0000-0000-0000-000000000002");
  private static final UUID Q3_ID =
      UUID.fromString("cccccccc-0000-0000-0000-000000000003");
  private static final UUID Q4_ID =
      UUID.fromString("cccccccc-0000-0000-0000-000000000004");
  private static final UUID Q5_ID =
      UUID.fromString("cccccccc-0000-0000-0000-000000000005");

  private static final UUID Q1_CORRECT_OPT =
      UUID.fromString("dddddddd-0000-0000-0000-000000000001");
  private static final UUID Q2_CORRECT_OPT =
      UUID.fromString("dddddddd-0000-0000-0000-000000000006");
  private static final UUID Q3_CORRECT_OPT =
      UUID.fromString("dddddddd-0000-0000-0000-000000000010");
  private static final UUID Q4_CORRECT_OPT =
      UUID.fromString("dddddddd-0000-0000-0000-000000000014");
  private static final UUID Q5_CORRECT_OPT =
      UUID.fromString("dddddddd-0000-0000-0000-000000000018");

  // A wrong option for Q1 (not the correct one)
  private static final UUID Q1_WRONG_OPT =
      UUID.fromString("dddddddd-0000-0000-0000-000000000002");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private AttemptRepository attemptRepository;

  @MockitoBean
  private NotificationService notificationService;

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

  /**
   * Happy path: start an attempt, submit all 5 correct answers, verify score, percentage,
   * feedback, and per-question results in the response.
   */
  @Test
  @Rollback
  void submitAttempt_allCorrect_returns200WithFullScore() throws Exception {
    doNothing().when(notificationService).sendResultEmail(
        any(UUID.class), any(UUID.class), anyString(),
        anyInt(), anyInt(), anyDouble(), anyString(), any(OffsetDateTime.class));

    // Start attempt first
    MvcResult startResult = mockMvc.perform(
            post("/quizzes/{quizId}/attempts", QUIZ_1_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new StartAttemptRequest(USER_1_ID))))
        .andExpect(status().isCreated())
        .andReturn();

    String attemptId = objectMapper.readTree(
        startResult.getResponse().getContentAsString()).get("attemptId").asText();

    SubmitAttemptRequest submitRequest = new SubmitAttemptRequest(USER_1_ID, List.of(
        new SubmitAnswerRequest(Q1_ID, Q1_CORRECT_OPT),
        new SubmitAnswerRequest(Q2_ID, Q2_CORRECT_OPT),
        new SubmitAnswerRequest(Q3_ID, Q3_CORRECT_OPT),
        new SubmitAnswerRequest(Q4_ID, Q4_CORRECT_OPT),
        new SubmitAnswerRequest(Q5_ID, Q5_CORRECT_OPT)));

    mockMvc.perform(post("/attempts/{attemptId}/submit", attemptId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(submitRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.score").value(5))
        .andExpect(jsonPath("$.totalQuestions").value(5))
        .andExpect(jsonPath("$.percentage").value(100.00))
        .andExpect(jsonPath("$.feedbackMessage").value("Excellent work! Keep it up!"))
        .andExpect(jsonPath("$.results").isArray())
        .andExpect(jsonPath("$.results.length()").value(5))
        .andExpect(jsonPath("$.results[0].questionId").isNotEmpty())
        .andExpect(jsonPath("$.results[0].correct").value(true))
        .andExpect(jsonPath("$.results[0].explanation").isNotEmpty());
  }

  /**
   * Verifies that submitting an already-submitted attempt returns HTTP 409.
   */
  @Test
  @Rollback
  void submitAttempt_alreadySubmitted_returns409() throws Exception {
    doNothing().when(notificationService).sendResultEmail(
        any(UUID.class), any(UUID.class), anyString(),
        anyInt(), anyInt(), anyDouble(), anyString(), any(OffsetDateTime.class));

    // Start attempt
    MvcResult startResult = mockMvc.perform(
            post("/quizzes/{quizId}/attempts", QUIZ_1_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new StartAttemptRequest(USER_1_ID))))
        .andExpect(status().isCreated())
        .andReturn();

    String attemptId = objectMapper.readTree(
        startResult.getResponse().getContentAsString()).get("attemptId").asText();

    SubmitAttemptRequest submitRequest = new SubmitAttemptRequest(USER_1_ID, List.of(
        new SubmitAnswerRequest(Q1_ID, Q1_CORRECT_OPT),
        new SubmitAnswerRequest(Q2_ID, Q2_CORRECT_OPT),
        new SubmitAnswerRequest(Q3_ID, Q3_CORRECT_OPT),
        new SubmitAnswerRequest(Q4_ID, Q4_CORRECT_OPT),
        new SubmitAnswerRequest(Q5_ID, Q5_CORRECT_OPT)));

    // First submission should succeed
    mockMvc.perform(post("/attempts/{attemptId}/submit", attemptId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(submitRequest)))
        .andExpect(status().isOk());

    // Second submission of same attempt must return 409
    mockMvc.perform(post("/attempts/{attemptId}/submit", attemptId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(submitRequest)))
        .andExpect(status().isConflict());
  }

  /**
   * Verifies that submitting with a non-existent attemptId returns HTTP 404.
   */
  @Test
  void submitAttempt_unknownAttemptId_returns404() throws Exception {
    SubmitAttemptRequest submitRequest = new SubmitAttemptRequest(USER_1_ID, List.of(
        new SubmitAnswerRequest(Q1_ID, Q1_CORRECT_OPT)));

    mockMvc.perform(post("/attempts/{attemptId}/submit", UNKNOWN_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(submitRequest)))
        .andExpect(status().isNotFound());
  }
}
