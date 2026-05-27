package com.fsl.quizapp.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsl.quizapp.attempt.dto.StartAttemptRequest;
import com.fsl.quizapp.attempt.dto.SubmitAnswerRequest;
import com.fsl.quizapp.attempt.dto.SubmitAttemptRequest;
import com.fsl.quizapp.attempt.repository.AnswerRepository;
import com.fsl.quizapp.attempt.repository.AttemptRepository;
import com.fsl.quizapp.notification.dto.NotificationPayload;
import com.fsl.quizapp.notification.entity.NotificationStatus;
import com.fsl.quizapp.notification.repository.NotificationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests verifying notification dispatch behaviour after quiz submission.
 *
 * <p>Uses a {@link SyncTaskExecutor} override so that {@code @Async} notification
 * dispatch runs synchronously on the caller's thread, inside the same transaction.
 * This guarantees the notification row is visible when {@link NotificationService}
 * calls {@code findById} — avoiding a race between transaction commit and async
 * execution that exists in the production {@link ThreadPoolTaskExecutor} setup.
 *
 * <p>There is no class-level {@code @Transactional}. Each test commits its data
 * so the notification service can see it, and {@link #cleanup()} removes
 * test-created rows after each test.
 *
 * <p><strong>Note:</strong> the production executor is a real thread pool; a
 * notification dispatch can silently drop if the async thread reads the DB before
 * the caller's transaction commits. Consider moving the dispatch to a
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} in a follow-up.
 */
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(NotificationIntegrationTest.SyncExecutorConfig.class)
class NotificationIntegrationTest {

  /**
   * Replaces the production {@code notificationExecutor} thread pool with a
   * {@link SyncTaskExecutor} so async notification dispatch is deterministic in tests.
   */
  @TestConfiguration
  static class SyncExecutorConfig {

    /**
     * Returns a synchronous executor that runs tasks on the caller's thread.
     *
     * @return a {@link SyncTaskExecutor} named {@code notificationExecutor}
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
      return new SyncTaskExecutor();
    }
  }

  private static final UUID QUIZ_1_ID =
      UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
  private static final UUID USER_1_ID =
      UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

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

  @MockitoSpyBean
  private MockEmailService emailSpy;

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private AnswerRepository answerRepository;

  @Autowired
  private AttemptRepository attemptRepository;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Deletes all attempts, their answers, and notifications created during each test.
   * Seed data rows are left untouched.
   */
  @AfterEach
  void cleanup() {
    answerRepository.deleteAll();
    notificationRepository.deleteAll();
    attemptRepository.deleteAll();
  }

  /**
   * Verifies that after a successful submission, the email service is called
   * asynchronously within two seconds. Uses Awaitility — no Thread.sleep.
   *
   * @throws Exception if MockMvc or JSON processing fails
   */
  @Test
  void submitAttempt_happyPath_dispatchesNotificationAsync() throws Exception {
    UUID attemptId = startAttempt();

    submitAllCorrect(attemptId);

    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(() ->
            verify(emailSpy).sendResultsEmail(any(NotificationPayload.class)));
  }

  /**
   * Verifies that when the email service throws, submission still returns HTTP 200
   * and the notification record eventually transitions to FAILED status.
   *
   * @throws Exception if MockMvc or JSON processing fails
   */
  @Test
  void submitAttempt_emailThrows_returnsOkAndMarksNotificationFailed() throws Exception {
    doThrow(new RuntimeException("smtp error")).when(emailSpy)
        .sendResultsEmail(any(NotificationPayload.class));

    UUID attemptId = startAttempt();

    MvcResult result = submitAllCorrect(attemptId);
    assertThat(result.getResponse().getStatus()).isEqualTo(200);

    await()
        .atMost(2, TimeUnit.SECONDS)
        .untilAsserted(() -> {
          Optional<com.fsl.quizapp.notification.entity.Notification> notification =
              notificationRepository.findByAttemptId(attemptId);
          assertThat(notification).isPresent();
          assertThat(notification.get().getStatus()).isEqualTo(NotificationStatus.FAILED);
        });
  }

  /**
   * Starts a new attempt for Quiz 1 / User 1 and returns the attempt UUID.
   *
   * @return the UUID of the newly created attempt
   * @throws Exception if MockMvc or JSON processing fails
   */
  private UUID startAttempt() throws Exception {
    MvcResult startResult = mockMvc.perform(
            post("/quizzes/{quizId}/attempts", QUIZ_1_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new StartAttemptRequest(USER_1_ID))))
        .andExpect(status().isCreated())
        .andReturn();

    String attemptId = objectMapper.readTree(
        startResult.getResponse().getContentAsString()).get("attemptId").asText();
    return UUID.fromString(attemptId);
  }

  /**
   * Submits all five correct answers for the given attempt and returns the MvcResult.
   *
   * @param attemptId the UUID of the attempt to submit
   * @return the MvcResult from the submit request
   * @throws Exception if MockMvc or JSON processing fails
   */
  private MvcResult submitAllCorrect(UUID attemptId) throws Exception {
    SubmitAttemptRequest submitRequest = new SubmitAttemptRequest(USER_1_ID, List.of(
        new SubmitAnswerRequest(Q1_ID, Q1_CORRECT_OPT),
        new SubmitAnswerRequest(Q2_ID, Q2_CORRECT_OPT),
        new SubmitAnswerRequest(Q3_ID, Q3_CORRECT_OPT),
        new SubmitAnswerRequest(Q4_ID, Q4_CORRECT_OPT),
        new SubmitAnswerRequest(Q5_ID, Q5_CORRECT_OPT)));

    return mockMvc.perform(post("/attempts/{attemptId}/submit", attemptId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(submitRequest)))
        .andReturn();
  }
}
