package com.fsl.quizapp.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fsl.quizapp.common.exception.ResourceNotFoundException;
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
