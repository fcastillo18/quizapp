package com.fsl.quizapp.attempt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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

/** Integration tests for the GET /attempts/{attemptId} endpoint. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AttemptDetailIntegrationTest {

  private static final UUID QUIZ_1_ID =
      UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
  private static final UUID USER_1_ID =
      UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
  private static final UUID UNKNOWN_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000000");

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

  /**
   * Starts and submits an attempt, then verifies the detail endpoint returns 200 with all fields.
   */
  @Test
  @Rollback
  void getAttemptDetail_submittedAttempt_returns200WithFullBreakdown() throws Exception {
    doNothing().when(notificationService).sendResultEmail(
        any(UUID.class), any(UUID.class), anyString(),
        anyInt(), anyInt(), anyDouble(), anyString(), any(OffsetDateTime.class));

    String attemptId = startAndSubmitAllCorrect();

    mockMvc.perform(get("/attempts/{attemptId}", attemptId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.attemptId").value(attemptId))
        .andExpect(jsonPath("$.userId").value(USER_1_ID.toString()))
        .andExpect(jsonPath("$.quizId").value(QUIZ_1_ID.toString()))
        .andExpect(jsonPath("$.quizTitle").isNotEmpty())
        .andExpect(jsonPath("$.startedAt").isNotEmpty())
        .andExpect(jsonPath("$.submittedAt").isNotEmpty())
        .andExpect(jsonPath("$.score").value(5))
        .andExpect(jsonPath("$.totalQuestions").value(5))
        .andExpect(jsonPath("$.percentage").value(100.00))
        .andExpect(jsonPath("$.results").isArray())
        .andExpect(jsonPath("$.results.length()").value(5))
        .andExpect(jsonPath("$.results[0].questionId").isNotEmpty())
        .andExpect(jsonPath("$.results[0].questionText").isNotEmpty())
        .andExpect(jsonPath("$.results[0].selectedOptionId").isNotEmpty())
        .andExpect(jsonPath("$.results[0].selectedOptionText").isNotEmpty())
        .andExpect(jsonPath("$.results[0].correctOptionId").isNotEmpty())
        .andExpect(jsonPath("$.results[0].correctOptionText").isNotEmpty())
        .andExpect(jsonPath("$.results[0].correct").value(true))
        .andExpect(jsonPath("$.results[0].explanation").isNotEmpty());
  }

  /**
   * Verifies that answering Q1 incorrectly results in correct=false for that question's result.
   */
  @Test
  @Rollback
  void getAttemptDetail_correctAndIncorrectAnswers_reflectsCorrectness() throws Exception {
    doNothing().when(notificationService).sendResultEmail(
        any(UUID.class), any(UUID.class), anyString(),
        anyInt(), anyInt(), anyDouble(), anyString(), any(OffsetDateTime.class));

    MvcResult startResult = startAttempt();
    String attemptId = extractAttemptId(startResult);

    // Q1 answered wrong, rest correct
    SubmitAttemptRequest submitRequest = new SubmitAttemptRequest(USER_1_ID, List.of(
        new SubmitAnswerRequest(Q1_ID, Q1_WRONG_OPT),
        new SubmitAnswerRequest(Q2_ID, Q2_CORRECT_OPT),
        new SubmitAnswerRequest(Q3_ID, Q3_CORRECT_OPT),
        new SubmitAnswerRequest(Q4_ID, Q4_CORRECT_OPT),
        new SubmitAnswerRequest(Q5_ID, Q5_CORRECT_OPT)));

    mockMvc.perform(post("/attempts/{attemptId}/submit", attemptId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(submitRequest)))
        .andExpect(status().isOk());

    mockMvc.perform(get("/attempts/{attemptId}", attemptId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.score").value(4))
        .andExpect(jsonPath("$.results[0].correct").value(false))
        .andExpect(jsonPath("$.results[0].correctOptionId")
            .value(Q1_CORRECT_OPT.toString()))
        .andExpect(jsonPath("$.results[1].correct").value(true));
  }

  /** Verifies that an unknown attemptId returns HTTP 404. */
  @Test
  void getAttemptDetail_unknownAttemptId_returns404() throws Exception {
    mockMvc.perform(get("/attempts/{attemptId}", UNKNOWN_ID))
        .andExpect(status().isNotFound());
  }

  /** Verifies that requesting detail for an unsubmitted attempt returns HTTP 404. */
  @Test
  @Rollback
  void getAttemptDetail_openAttempt_returns404() throws Exception {
    MvcResult startResult = startAttempt();
    String attemptId = extractAttemptId(startResult);

    mockMvc.perform(get("/attempts/{attemptId}", attemptId))
        .andExpect(status().isNotFound());
  }

  /**
   * Verifies that results are ordered by question position ascending,
   * so the first result corresponds to Q1 (position 1).
   */
  @Test
  @Rollback
  void getAttemptDetail_resultsOrderedByPosition() throws Exception {
    doNothing().when(notificationService).sendResultEmail(
        any(UUID.class), any(UUID.class), anyString(),
        anyInt(), anyInt(), anyDouble(), anyString(), any(OffsetDateTime.class));

    String attemptId = startAndSubmitAllCorrect();

    mockMvc.perform(get("/attempts/{attemptId}", attemptId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results[0].questionId").value(Q1_ID.toString()))
        .andExpect(jsonPath("$.results[1].questionId").value(Q2_ID.toString()))
        .andExpect(jsonPath("$.results[4].questionId").value(Q5_ID.toString()));
  }

  // --- helpers ---

  private MvcResult startAttempt() throws Exception {
    return mockMvc.perform(
            post("/quizzes/{quizId}/attempts", QUIZ_1_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new StartAttemptRequest(USER_1_ID))))
        .andExpect(status().isCreated())
        .andReturn();
  }

  private String extractAttemptId(MvcResult result) throws Exception {
    return objectMapper.readTree(
        result.getResponse().getContentAsString()).get("attemptId").asText();
  }

  private String startAndSubmitAllCorrect() throws Exception {
    MvcResult startResult = startAttempt();
    String attemptId = extractAttemptId(startResult);

    SubmitAttemptRequest submitRequest = new SubmitAttemptRequest(USER_1_ID, List.of(
        new SubmitAnswerRequest(Q1_ID, Q1_CORRECT_OPT),
        new SubmitAnswerRequest(Q2_ID, Q2_CORRECT_OPT),
        new SubmitAnswerRequest(Q3_ID, Q3_CORRECT_OPT),
        new SubmitAnswerRequest(Q4_ID, Q4_CORRECT_OPT),
        new SubmitAnswerRequest(Q5_ID, Q5_CORRECT_OPT)));

    mockMvc.perform(post("/attempts/{attemptId}/submit", attemptId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(submitRequest)))
        .andExpect(status().isOk());

    return attemptId;
  }
}
