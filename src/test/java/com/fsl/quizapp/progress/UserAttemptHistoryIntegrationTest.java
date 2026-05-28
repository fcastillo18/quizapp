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

/** Integration tests for GET /users/{userId}/attempts endpoint (QUIZ-09). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserAttemptHistoryIntegrationTest {

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

  /** Starts a new attempt for USER_1 and returns the attempt ID. */
  private String startAttempt(UUID userId) throws Exception {
    String body = objectMapper.writeValueAsString(new StartAttemptRequest(userId));
    var result = mockMvc.perform(post("/quizzes/{quizId}/attempts", QUIZ_1_ID)
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString())
        .get("attemptId").asText();
  }

  /** Submits all 5 correct answers for the given attempt. */
  private void submitAttemptAllCorrect(UUID userId, String attemptId) throws Exception {
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

  /**
   * Verifies that a user with a submitted attempt receives a 200 response containing
   * quizTitle, score, and percentage in the history list.
   */
  @Test
  void getUserAttempts_withSubmittedAttempt_returnsAttemptWithQuizTitleAndScore()
      throws Exception {
    String attemptId = startAttempt(USER_1_ID);
    submitAttemptAllCorrect(USER_1_ID, attemptId);

    mockMvc.perform(get("/users/{userId}/attempts", USER_1_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].attemptId").value(attemptId))
        .andExpect(jsonPath("$[0].quizId").value(QUIZ_1_ID.toString()))
        .andExpect(jsonPath("$[0].quizTitle").isNotEmpty())
        .andExpect(jsonPath("$[0].startedAt").isNotEmpty())
        .andExpect(jsonPath("$[0].submittedAt").isNotEmpty())
        .andExpect(jsonPath("$[0].score").value(5))
        .andExpect(jsonPath("$[0].percentage").value(100.00));
  }

  /**
   * Verifies that a user with no attempts receives a 200 response with an empty array.
   */
  @Test
  void getUserAttempts_withNoAttempts_returnsEmptyArray() throws Exception {
    mockMvc.perform(get("/users/{userId}/attempts", USER_2_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  /**
   * Verifies that an open (not-yet-submitted) attempt is included in the history
   * with null submittedAt, score, and percentage.
   */
  @Test
  void getUserAttempts_withOpenAttempt_includesAttemptWithNullScoreAndPercentage()
      throws Exception {
    String attemptId = startAttempt(USER_1_ID);

    mockMvc.perform(get("/users/{userId}/attempts", USER_1_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].attemptId").value(attemptId))
        .andExpect(jsonPath("$[0].submittedAt").doesNotExist())
        .andExpect(jsonPath("$[0].score").doesNotExist())
        .andExpect(jsonPath("$[0].percentage").doesNotExist());
  }

  /**
   * Verifies that two attempts for the same quiz both appear in the history
   * referencing the same quizId.
   */
  @Test
  void getUserAttempts_withTwoAttempts_returnsBothReferencingSameQuiz() throws Exception {
    startAttempt(USER_1_ID);
    startAttempt(USER_1_ID);

    mockMvc.perform(get("/users/{userId}/attempts", USER_1_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].quizId").value(QUIZ_1_ID.toString()))
        .andExpect(jsonPath("$[1].quizId").value(QUIZ_1_ID.toString()));
  }

  /**
   * Verifies that results are ordered most-recent first by checking startedAt ordering.
   */
  @Test
  void getUserAttempts_orderedMostRecentFirst_startedAtDescending() throws Exception {
    String firstAttemptId = startAttempt(USER_1_ID);
    String secondAttemptId = startAttempt(USER_1_ID);

    var result = mockMvc.perform(get("/users/{userId}/attempts", USER_1_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andReturn();

    var tree = objectMapper.readTree(result.getResponse().getContentAsString());
    String topId = tree.get(0).get("attemptId").asText();
    String bottomId = tree.get(1).get("attemptId").asText();

    // Most recent attempt (second started) should be first in the list
    org.assertj.core.api.Assertions.assertThat(topId).isEqualTo(secondAttemptId);
    org.assertj.core.api.Assertions.assertThat(bottomId).isEqualTo(firstAttemptId);
  }
}
