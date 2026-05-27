package com.fsl.quizapp.attempt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsl.quizapp.attempt.controller.AttemptController;
import com.fsl.quizapp.attempt.dto.AttemptOptionResponse;
import com.fsl.quizapp.attempt.dto.AttemptQuestionResponse;
import com.fsl.quizapp.attempt.dto.AttemptStartResponse;
import com.fsl.quizapp.attempt.dto.StartAttemptRequest;
import com.fsl.quizapp.attempt.service.AttemptService;
import com.fsl.quizapp.common.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Controller-layer tests for {@link AttemptController}. */
@WebMvcTest(AttemptController.class)
class AttemptControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AttemptService attemptService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** Verifies that a valid request returns HTTP 201 with Location header. */
  @Test
  void startAttempt_validRequest_returns201WithLocation() throws Exception {
    UUID quizId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    UUID userId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    UUID attemptId = UUID.randomUUID();

    AttemptStartResponse response = new AttemptStartResponse(
        attemptId, quizId, userId, OffsetDateTime.now(),
        List.of(new AttemptQuestionResponse(
            UUID.randomUUID(), "Q1", 1,
            List.of(new AttemptOptionResponse(UUID.randomUUID(), "A", 1)))));

    when(attemptService.startAttempt(eq(quizId), any(StartAttemptRequest.class)))
        .thenReturn(response);

    mockMvc.perform(post("/quizzes/{quizId}/attempts", quizId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new StartAttemptRequest(userId))))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location",
            org.hamcrest.Matchers.endsWith("/attempts/" + attemptId)))
        .andExpect(jsonPath("$.attemptId").value(attemptId.toString()))
        .andExpect(jsonPath("$.quizId").value(quizId.toString()))
        .andExpect(jsonPath("$.userId").value(userId.toString()))
        .andExpect(jsonPath("$.questions").isArray());
  }

  /** Verifies that a missing userId in the request body returns HTTP 400. */
  @Test
  void startAttempt_missingUserId_returns400() throws Exception {
    UUID quizId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");

    mockMvc.perform(post("/quizzes/{quizId}/attempts", quizId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\": null}"))
        .andExpect(status().isBadRequest());
  }

  /** Verifies that an unknown quizId returns HTTP 404. */
  @Test
  void startAttempt_unknownQuizId_returns404() throws Exception {
    UUID quizId = UUID.fromString("00000000-0000-0000-0000-000000000000");
    UUID userId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

    when(attemptService.startAttempt(eq(quizId), any(StartAttemptRequest.class)))
        .thenThrow(new ResourceNotFoundException("Quiz", quizId));

    mockMvc.perform(post("/quizzes/{quizId}/attempts", quizId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new StartAttemptRequest(userId))))
        .andExpect(status().isNotFound());
  }
}
