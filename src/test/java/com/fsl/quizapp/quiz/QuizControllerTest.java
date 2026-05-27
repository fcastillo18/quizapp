package com.fsl.quizapp.quiz;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsl.quizapp.common.exception.ResourceNotFoundException;
import com.fsl.quizapp.quiz.controller.QuizController;
import com.fsl.quizapp.quiz.dto.OptionResponse;
import com.fsl.quizapp.quiz.dto.QuestionResponse;
import com.fsl.quizapp.quiz.dto.QuizDetailResponse;
import com.fsl.quizapp.quiz.dto.QuizSummaryResponse;
import com.fsl.quizapp.quiz.service.QuizService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Controller slice tests for QuizController. */
@WebMvcTest(QuizController.class)
class QuizControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  QuizService quizService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** GET /quizzes returns 200 with list of quiz summaries. */
  @Test
  void getQuizzes_returns200WithList() throws Exception {
    UUID id1 = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    UUID id2 = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    when(quizService.listQuizzes()).thenReturn(List.of(
        new QuizSummaryResponse(id1, "LLM Fundamentals", "Intro quiz"),
        new QuizSummaryResponse(id2, "Agent Design Patterns", null)
    ));

    mockMvc.perform(get("/quizzes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(id1.toString()))
        .andExpect(jsonPath("$[0].title").value("LLM Fundamentals"))
        .andExpect(jsonPath("$[1].id").value(id2.toString()))
        .andExpect(jsonPath("$[1].title").value("Agent Design Patterns"));
  }

  /** GET /quizzes returns 200 with empty array when no quizzes exist. */
  @Test
  void getQuizzes_returns200WithEmptyList() throws Exception {
    when(quizService.listQuizzes()).thenReturn(List.of());

    mockMvc.perform(get("/quizzes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  /** GET /quizzes/{id} returns 200 with quiz detail including questions and options. */
  @Test
  void getQuizDetails_returns200WithDetail() throws Exception {
    UUID quizId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    UUID questionId = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    UUID optId = UUID.randomUUID();
    OptionResponse opt = new OptionResponse(optId, "option text", 1);
    QuestionResponse question = new QuestionResponse(questionId, "question text", 1, List.of(opt));
    QuizDetailResponse detail = new QuizDetailResponse(
        quizId, "LLM Fundamentals", "description", List.of(question));
    when(quizService.getQuizDetails(quizId)).thenReturn(detail);

    mockMvc.perform(get("/quizzes/" + quizId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(quizId.toString()))
        .andExpect(jsonPath("$.title").value("LLM Fundamentals"))
        .andExpect(jsonPath("$.questions[0].id").value(questionId.toString()))
        .andExpect(jsonPath("$.questions[0].options[0].position").value(1))
        .andExpect(jsonPath("$.questions[0].correctOptionId").doesNotExist())
        .andExpect(jsonPath("$.questions[0].explanation").doesNotExist());
  }

  /** GET /quizzes/{id} returns 404 when quiz does not exist. */
  @Test
  void getQuizDetails_returns404WhenNotFound() throws Exception {
    UUID unknown = UUID.fromString("00000000-0000-0000-0000-000000000000");
    when(quizService.getQuizDetails(unknown))
        .thenThrow(new ResourceNotFoundException("Quiz", unknown));

    mockMvc.perform(get("/quizzes/" + unknown))
        .andExpect(status().isNotFound());
  }
}
