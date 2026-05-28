package com.fsl.quizapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EndToEndQuizFlowTest {

  private static final UUID QUIZ_2_ID =
      UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
  private static final UUID USER_1_ID =
      UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

  private static final UUID Q11_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000011");
  private static final UUID Q12_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000012");
  private static final UUID Q13_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000013");
  private static final UUID Q14_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000014");
  private static final UUID Q15_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000015");

  private static final UUID Q11_CORRECT = UUID.fromString("dddddddd-0000-0000-0000-000000000022");
  private static final UUID Q12_CORRECT = UUID.fromString("dddddddd-0000-0000-0000-000000000026");
  private static final UUID Q13_CORRECT = UUID.fromString("dddddddd-0000-0000-0000-000000000030");
  private static final UUID Q14_CORRECT = UUID.fromString("dddddddd-0000-0000-0000-000000000034");
  private static final UUID Q15_CORRECT = UUID.fromString("dddddddd-0000-0000-0000-000000000038");
  private static final UUID Q15_WRONG   = UUID.fromString("dddddddd-0000-0000-0000-000000000037");

  @Autowired private MockMvc mockMvc;
  @MockitoBean  private NotificationService notificationService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @Rollback
  void completeQuizWorkflow_happyPath_validatesAllElevenSteps() throws Exception {
    doNothing().when(notificationService).sendResultEmail(
        any(UUID.class), any(UUID.class), anyString(),
        anyInt(), anyInt(), anyDouble(), anyString(), any(OffsetDateTime.class));

    // ── Step 1: List available quiz categories ──────────────────────────────
    MvcResult listResult = mockMvc.perform(get("/quizzes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andReturn();

    JsonNode quizList = objectMapper.readTree(listResult.getResponse().getContentAsString());
    assertThat(quizList.isArray()).isTrue();
    assertThat(quizList.size()).isGreaterThanOrEqualTo(1);

    // ── Step 2: Locate "Agent Design Patterns" quiz by title ────────────────
    String quizId = null;
    for (JsonNode quiz : quizList) {
      if ("Agent Design Patterns".equals(quiz.get("title").asText())) {
        quizId = quiz.get("id").asText();
        break;
      }
    }
    assertThat(quizId).as("Agent Design Patterns quiz must be present in seed data")
        .isEqualTo(QUIZ_2_ID.toString());

    // ── Step 3: Get quiz details — 5 questions, options present, no answers ─
    MvcResult detailResult = mockMvc.perform(get("/quizzes/{id}", QUIZ_2_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(QUIZ_2_ID.toString()))
        .andExpect(jsonPath("$.title").value("Agent Design Patterns"))
        .andExpect(jsonPath("$.questions").isArray())
        .andExpect(jsonPath("$.questions.length()").value(5))
        .andReturn();

    String detailBody = detailResult.getResponse().getContentAsString();
    assertThat(detailBody).doesNotContain("correctOptionId");

    // ── Step 4: Start a quiz attempt ────────────────────────────────────────
    MvcResult startResult = mockMvc.perform(
            post("/quizzes/{quizId}/attempts", QUIZ_2_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new StartAttemptRequest(USER_1_ID))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.attemptId").isNotEmpty())
        .andExpect(jsonPath("$.quizId").value(QUIZ_2_ID.toString()))
        .andExpect(jsonPath("$.userId").value(USER_1_ID.toString()))
        .andExpect(jsonPath("$.questions.length()").value(5))
        .andReturn();

    String attemptId = objectMapper.readTree(
        startResult.getResponse().getContentAsString()).get("attemptId").asText();

    // ── Step 5: Verify questions expose no correct answers ──────────────────
    String startBody = startResult.getResponse().getContentAsString();
    assertThat(startBody).doesNotContain("correctOptionId");
    assertThat(startBody).doesNotContain("explanation");

    // ── Step 6: Submit answers — 4 correct, Q15 wrong → 80% ────────────────
    SubmitAttemptRequest submitRequest = new SubmitAttemptRequest(USER_1_ID, List.of(
        new SubmitAnswerRequest(Q11_ID, Q11_CORRECT),
        new SubmitAnswerRequest(Q12_ID, Q12_CORRECT),
        new SubmitAnswerRequest(Q13_ID, Q13_CORRECT),
        new SubmitAnswerRequest(Q14_ID, Q14_CORRECT),
        new SubmitAnswerRequest(Q15_ID, Q15_WRONG)));

    // ── Step 7: Verify score, percentage, feedback, and per-question results ─
    mockMvc.perform(post("/attempts/{attemptId}/submit", attemptId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(submitRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.score").value(4))
        .andExpect(jsonPath("$.totalQuestions").value(5))
        .andExpect(jsonPath("$.percentage").value(80.00))
        .andExpect(jsonPath("$.feedbackMessage").value("Excellent work! Keep it up!"))
        .andExpect(jsonPath("$.results.length()").value(5))
        .andExpect(jsonPath("$.results[4].correct").value(false))
        .andExpect(jsonPath("$.results[4].explanation").isNotEmpty());

    // ── Step 8: Verify async notification was triggered ─────────────────────
    verify(notificationService).sendResultEmail(
        any(UUID.class), any(UUID.class), anyString(),
        anyInt(), anyInt(), anyDouble(), anyString(), any(OffsetDateTime.class));

    // ── Step 9: View attempt history — 1 completed attempt ──────────────────
    MvcResult historyResult = mockMvc.perform(
            get("/users/{userId}/attempts", USER_1_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andReturn();

    JsonNode history = objectMapper.readTree(historyResult.getResponse().getContentAsString());
    assertThat(history.size()).isEqualTo(1);
    assertThat(history.get(0).get("attemptId").asText()).isEqualTo(attemptId);
    assertThat(history.get(0).get("submittedAt").asText()).isNotEmpty();
    assertThat(history.get(0).get("score").asInt()).isEqualTo(4);
    assertThat(history.get(0).get("percentage").asDouble()).isEqualTo(80.0);

    // ── Step 10: Retake the quiz — start a new attempt ───────────────────────
    MvcResult retakeResult = mockMvc.perform(
            post("/quizzes/{quizId}/attempts", QUIZ_2_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new StartAttemptRequest(USER_1_ID))))
        .andExpect(status().isCreated())
        .andReturn();

    String retakeAttemptId = objectMapper.readTree(
        retakeResult.getResponse().getContentAsString()).get("attemptId").asText();
    assertThat(retakeAttemptId).isNotEqualTo(attemptId);

    // ── Step 11: History now shows 2 attempts; previous attempt preserved ───
    MvcResult updatedHistoryResult = mockMvc.perform(
            get("/users/{userId}/attempts", USER_1_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andReturn();

    JsonNode updatedHistory = objectMapper.readTree(
        updatedHistoryResult.getResponse().getContentAsString());
    assertThat(updatedHistory.size()).isEqualTo(2);

    List<String> attemptIds = new java.util.ArrayList<>();
    updatedHistory.forEach(a -> attemptIds.add(a.get("attemptId").asText()));
    assertThat(attemptIds).contains(attemptId, retakeAttemptId);
  }
}
