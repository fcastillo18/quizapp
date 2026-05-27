package com.fsl.quizapp.quiz;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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

  /** GET /quizzes/{id} returns 200 with quiz detail for seeded quiz 1. */
  @Test
  void getQuizDetails_returnsSeededQuiz1() throws Exception {
    String id = "bbbbbbbb-0000-0000-0000-000000000001";

    mockMvc.perform(get("/quizzes/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.title").value("LLM Fundamentals"))
        .andExpect(jsonPath("$.questions").isArray())
        .andExpect(jsonPath("$.questions.length()").value(5));
  }

  /** GET /quizzes/{id} returns questions ordered by position ascending. */
  @Test
  void getQuizDetails_questionsOrderedByPosition() throws Exception {
    String id = "bbbbbbbb-0000-0000-0000-000000000001";

    mockMvc.perform(get("/quizzes/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.questions[0].position").value(1))
        .andExpect(jsonPath("$.questions[1].position").value(2))
        .andExpect(jsonPath("$.questions[2].position").value(3));
  }

  /** GET /quizzes/{id} response does not expose correctOptionId or explanation. */
  @Test
  void getQuizDetails_doesNotExposeCorrectOptionIdOrExplanation() throws Exception {
    String id = "bbbbbbbb-0000-0000-0000-000000000001";

    mockMvc.perform(get("/quizzes/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.questions[0].correctOptionId").doesNotExist())
        .andExpect(jsonPath("$.questions[0].explanation").doesNotExist());
  }

  /** GET /quizzes/{id} returns 404 for unknown UUID. */
  @Test
  void getQuizDetails_returns404ForUnknownId() throws Exception {
    mockMvc.perform(get("/quizzes/00000000-0000-0000-0000-000000000000"))
        .andExpect(status().isNotFound());
  }

  private static final String VALID_QUIZ_BODY = """
      {
        "title": "Integration Test Quiz",
        "description": "Created in test",
        "questions": [
          {
            "text": "What is 1+1?",
            "explanation": "Basic math",
            "position": 1,
            "options": [
              {"text": "1", "position": 1},
              {"text": "2", "position": 2}
            ],
            "correctOptionPosition": 2
          }
        ]
      }
      """;

  /** POST /quizzes with a valid payload returns 201 and the quiz is retrievable via GET. */
  @Test
  void createQuiz_validPayload_returns201AndIsRetrievable() throws Exception {
    MvcResult result = mockMvc.perform(post("/quizzes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_QUIZ_BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(header().string("Location", notNullValue()))
        .andReturn();

    String location = result.getResponse().getHeader("Location");
    String path = java.net.URI.create(location).getPath();

    mockMvc.perform(get(path))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Integration Test Quiz"))
        .andExpect(jsonPath("$.questions").isArray())
        .andExpect(jsonPath("$.questions.length()").value(1));
  }

  /** POST /quizzes with missing title returns 400. */
  @Test
  void createQuiz_missingTitle_returns400() throws Exception {
    String body = """
        {
          "description": "no title here",
          "questions": [
            {
              "text": "Q?",
              "explanation": "exp",
              "position": 1,
              "options": [
                {"text": "A", "position": 1},
                {"text": "B", "position": 2}
              ],
              "correctOptionPosition": 1
            }
          ]
        }
        """;

    mockMvc.perform(post("/quizzes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }

  /** POST /quizzes with correctOptionPosition out of range returns 400. */
  @Test
  void createQuiz_correctOptionPositionOutOfRange_returns400() throws Exception {
    String body = """
        {
          "title": "Test Quiz",
          "questions": [
            {
              "text": "Q?",
              "explanation": "exp",
              "position": 1,
              "options": [
                {"text": "A", "position": 1},
                {"text": "B", "position": 2}
              ],
              "correctOptionPosition": 99
            }
          ]
        }
        """;

    mockMvc.perform(post("/quizzes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }

  /** POST /quizzes with empty questions array returns 400. */
  @Test
  void createQuiz_emptyQuestions_returns400() throws Exception {
    String body = """
        {
          "title": "Empty Questions Quiz",
          "questions": []
        }
        """;

    mockMvc.perform(post("/quizzes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }
}
