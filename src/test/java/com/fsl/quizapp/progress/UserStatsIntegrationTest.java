package com.fsl.quizapp.progress;

import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsl.quizapp.attempt.dto.StartAttemptRequest;
import com.fsl.quizapp.attempt.dto.SubmitAnswerRequest;
import com.fsl.quizapp.attempt.dto.SubmitAttemptRequest;
import com.fsl.quizapp.notification.NotificationService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for GET /users/{userId}/stats endpoint (QUIZ-11). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserStatsIntegrationTest {

  private static final UUID QUIZ_1_ID =
      UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
  private static final UUID USER_1_ID =
      UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
  private static final UUID USER_2_ID =
      UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");

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

  private static final UUID Q1_WRONG_OPT =
      UUID.fromString("dddddddd-0000-0000-0000-000000000002");

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private NotificationService notificationService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** Suppress async notification interference before each test. */
  @BeforeEach
  void setUp() throws Exception {
    doNothing().when(notificationService).sendResultEmail(
        org.mockito.ArgumentMatchers.any(UUID.class),
        org.mockito.ArgumentMatchers.any(UUID.class),
        org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyInt(),
        org.mockito.ArgumentMatchers.anyInt(),
        org.mockito.ArgumentMatchers.anyDouble(),
        org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.any(OffsetDateTime.class));
  }

  /** Starts a new attempt for the given user and returns the attempt ID. */
  private String startAttempt(UUID userId) throws Exception {
    String body = objectMapper.writeValueAsString(new StartAttemptRequest(userId));
    var result = mockMvc.perform(post("/quizzes/{quizId}/attempts", QUIZ_1_ID)
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString())
        .get("attemptId").asText();
  }

  /** Submits all 5 correct answers for the given attempt (100% score). */
  private void submitAllCorrect(UUID userId, String attemptId) throws Exception {
    SubmitAttemptRequest req = new SubmitAttemptRequest(userId, List.of(
        new SubmitAnswerRequest(Q1_ID, Q1_CORRECT_OPT),
        new SubmitAnswerRequest(Q2_ID, Q2_CORRECT_OPT),
        new SubmitAnswerRequest(Q3_ID, Q3_CORRECT_OPT),
        new SubmitAnswerRequest(Q4_ID, Q4_CORRECT_OPT),
        new SubmitAnswerRequest(Q5_ID, Q5_CORRECT_OPT)));
    mockMvc.perform(post("/attempts/{attemptId}/submit", attemptId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk());
  }

  /** Submits 4 out of 5 correct answers for the given attempt (80% score). */
  private void submitFourCorrect(UUID userId, String attemptId) throws Exception {
    SubmitAttemptRequest req = new SubmitAttemptRequest(userId, List.of(
        new SubmitAnswerRequest(Q1_ID, Q1_WRONG_OPT),
        new SubmitAnswerRequest(Q2_ID, Q2_CORRECT_OPT),
        new SubmitAnswerRequest(Q3_ID, Q3_CORRECT_OPT),
        new SubmitAnswerRequest(Q4_ID, Q4_CORRECT_OPT),
        new SubmitAnswerRequest(Q5_ID, Q5_CORRECT_OPT)));
    mockMvc.perform(post("/attempts/{attemptId}/submit", attemptId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk());
  }

  /**
   * Verifies that a user with three submitted attempts returns totalAttempts=3
   * and a correctly computed averageScore.
   */
  @Test
  void getUserStats_withThreeSubmittedAttempts_returnsTotalThreeAndAvgScore() throws Exception {
    // Three attempts: 100%, 100%, 80% → avg = 93.33
    submitAllCorrect(USER_1_ID, startAttempt(USER_1_ID));
    submitAllCorrect(USER_1_ID, startAttempt(USER_1_ID));
    submitFourCorrect(USER_1_ID, startAttempt(USER_1_ID));

    mockMvc.perform(get("/users/{userId}/stats", USER_1_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(USER_1_ID.toString()))
        .andExpect(jsonPath("$.totalAttempts").value(3))
        .andExpect(jsonPath("$.averageScore").value(93.33));
  }

  /**
   * Verifies that a user with no submitted attempts returns totalAttempts=0
   * and averageScore=0.00.
   */
  @Test
  void getUserStats_withNoSubmittedAttempts_returnsZeroTotals() throws Exception {
    mockMvc.perform(get("/users/{userId}/stats", USER_2_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(USER_2_ID.toString()))
        .andExpect(jsonPath("$.totalAttempts").value(0))
        .andExpect(jsonPath("$.averageScore").value(0.00));
  }

  /**
   * Verifies that open (not-yet-submitted) attempts are excluded from the count
   * when some attempts are submitted and others are not.
   */
  @Test
  void getUserStats_openAttemptsExcluded_countsOnlySubmitted() throws Exception {
    // Submit 2, leave 1 open → totalAttempts must be 2
    submitAllCorrect(USER_1_ID, startAttempt(USER_1_ID));
    submitAllCorrect(USER_1_ID, startAttempt(USER_1_ID));
    startAttempt(USER_1_ID); // open attempt — not submitted

    mockMvc.perform(get("/users/{userId}/stats", USER_1_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalAttempts").value(2));
  }

  /**
   * Verifies that averageScore is rounded to two decimal places when the result
   * is a non-terminating decimal.
   */
  @Test
  void getUserStats_averageScoreRoundedToTwoDecimalPlaces() throws Exception {
    // 100%, 100%, 80% → avg = 93.333... → rounded to 93.33
    submitAllCorrect(USER_1_ID, startAttempt(USER_1_ID));
    submitAllCorrect(USER_1_ID, startAttempt(USER_1_ID));
    submitFourCorrect(USER_1_ID, startAttempt(USER_1_ID));

    mockMvc.perform(get("/users/{userId}/stats", USER_1_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.averageScore").value(93.33));
  }
}
