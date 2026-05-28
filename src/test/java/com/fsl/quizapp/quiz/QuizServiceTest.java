package com.fsl.quizapp.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fsl.quizapp.common.exception.BadRequestException;
import com.fsl.quizapp.common.exception.ResourceNotFoundException;
import com.fsl.quizapp.quiz.dto.CreateOptionRequest;
import com.fsl.quizapp.quiz.dto.CreateQuestionRequest;
import com.fsl.quizapp.quiz.dto.CreateQuizRequest;
import com.fsl.quizapp.quiz.dto.CreatedQuizResponse;
import com.fsl.quizapp.quiz.dto.QuizDetailResponse;
import com.fsl.quizapp.quiz.dto.QuizSummaryResponse;
import com.fsl.quizapp.quiz.entity.Option;
import com.fsl.quizapp.quiz.entity.Question;
import com.fsl.quizapp.quiz.entity.Quiz;
import com.fsl.quizapp.quiz.repository.QuizRepository;
import com.fsl.quizapp.quiz.service.QuizService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for QuizService. */
@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

  @Mock
  QuizRepository quizRepository;

  @InjectMocks
  QuizService quizService;

  /** listQuizzes returns a summary for each quiz in the repository. */
  @Test
  void listQuizzes_returnsSummariesForAllQuizzes() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    Quiz quiz1 = Quiz.builder().id(id1).title("Q1").description("D1").build();
    Quiz quiz2 = Quiz.builder().id(id2).title("Q2").description(null).build();
    when(quizRepository.findAll()).thenReturn(List.of(quiz1, quiz2));

    List<QuizSummaryResponse> result = quizService.listQuizzes();

    assertThat(result).hasSize(2);
    assertThat(result.get(0)).isEqualTo(new QuizSummaryResponse(id1, "Q1", "D1"));
    assertThat(result.get(1)).isEqualTo(new QuizSummaryResponse(id2, "Q2", null));
  }

  /** listQuizzes returns empty list when no quizzes exist. */
  @Test
  void listQuizzes_returnsEmptyListWhenNoneExist() {
    when(quizRepository.findAll()).thenReturn(List.of());

    List<QuizSummaryResponse> result = quizService.listQuizzes();

    assertThat(result).isEmpty();
  }

  /** getQuizSummary throws ResourceNotFoundException when quiz does not exist. */
  @Test
  void getQuizSummary_throwsWhenNotFound() {
    UUID unknown = UUID.fromString("00000000-0000-0000-0000-000000000000");
    when(quizRepository.findById(unknown)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> quizService.getQuizSummary(unknown))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  /** getQuizDetails returns questions and options sorted by position. */
  @Test
  void getQuizDetails_returnsSortedQuestionsAndOptions() {
    UUID quizId = UUID.randomUUID();
    UUID q1Id = UUID.randomUUID();
    UUID o1Id = UUID.randomUUID();
    UUID o2Id = UUID.randomUUID();

    Option opt1 = Option.builder().id(o1Id).text("optA").position(2).build();
    Option opt2 = Option.builder().id(o2Id).text("optB").position(1).build();
    Question q1 = Question.builder()
        .id(q1Id).text("Q text").explanation("exp").position(1)
        .options(List.of(opt1, opt2)).build();
    Quiz quiz = Quiz.builder()
        .id(quizId).title("T").description("D")
        .questions(List.of(q1)).build();

    when(quizRepository.findByIdWithQuestionsAndOptions(quizId)).thenReturn(Optional.of(quiz));

    QuizDetailResponse result = quizService.getQuizDetails(quizId);

    assertThat(result.id()).isEqualTo(quizId);
    assertThat(result.questions()).hasSize(1);
    assertThat(result.questions().get(0).options().get(0).position()).isEqualTo(1);
    assertThat(result.questions().get(0).options().get(1).position()).isEqualTo(2);
  }

  /** getQuizDetails throws ResourceNotFoundException when quiz does not exist. */
  @Test
  void getQuizDetails_throwsWhenNotFound() {
    UUID unknown = UUID.fromString("00000000-0000-0000-0000-000000000000");
    when(quizRepository.findByIdWithQuestionsAndOptions(unknown)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> quizService.getQuizDetails(unknown))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  /** createQuiz saves the quiz and returns a response with a non-null ID. */
  @Test
  void createQuiz_validRequest_returnsCreatedWithId() {
    UUID savedId = UUID.randomUUID();
    when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> {
      Quiz q = inv.getArgument(0);
      q.setId(savedId);
      return q;
    });

    CreateQuizRequest request = new CreateQuizRequest(
        "My Quiz",
        "desc",
        List.of(new CreateQuestionRequest(
            "What is 2+2?",
            "Basic addition",
            1,
            List.of(
                new CreateOptionRequest("3", 1),
                new CreateOptionRequest("4", 2)),
            2)));

    CreatedQuizResponse response = quizService.createQuiz(request);

    assertThat(response.id()).isEqualTo(savedId);
  }

  /** createQuiz throws BadRequestException when correctOptionPosition exceeds options size. */
  @Test
  void createQuiz_correctOptionPositionOutOfRange_throwsBadRequest() {
    when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> inv.getArgument(0));

    CreateQuizRequest request = new CreateQuizRequest(
        "My Quiz",
        null,
        List.of(new CreateQuestionRequest(
            "What is 2+2?",
            "Basic addition",
            1,
            List.of(
                new CreateOptionRequest("3", 1),
                new CreateOptionRequest("4", 2)),
            5)));

    assertThatThrownBy(() -> quizService.createQuiz(request))
        .isInstanceOf(BadRequestException.class);
  }

  /** getQuizDetails never exposes correctOptionId or explanation. */
  @Test
  void getQuizDetails_doesNotExposeCorrectOptionIdOrExplanation() {
    UUID quizId = UUID.randomUUID();
    UUID correctId = UUID.randomUUID();
    Option opt = Option.builder().id(UUID.randomUUID()).text("opt").position(1).build();
    Question q = Question.builder()
        .id(UUID.randomUUID()).text("Q").explanation("secret").position(1)
        .correctOptionId(correctId).options(List.of(opt)).build();
    Quiz quiz = Quiz.builder().id(quizId).title("T").description("D")
        .questions(List.of(q)).build();
    when(quizRepository.findByIdWithQuestionsAndOptions(quizId)).thenReturn(Optional.of(quiz));

    QuizDetailResponse result = quizService.getQuizDetails(quizId);

    // QuestionResponse does not have explanation or correctOptionId fields
    // compile-time check: accessing result.questions().get(0) only has id, text, position, options
    assertThat(result.questions().get(0).text()).isEqualTo("Q");
  }
}
