---
name: spring-test
description: Guide for writing Spring Boot tests. Clarifies when to use unit tests (fast, no context) vs integration tests (@SpringBootTest, @WebMvcTest, @DataJpaTest).
---

# Spring Boot Testing Guide

## Unit Tests — no Spring context, mock everything

Use when the class under test has no Spring infrastructure dependency.

```java
@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    QuizRepository quizRepository;

    @InjectMocks
    QuizService quizService;

    @Test
    void shouldReturnQuiz() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(new Quiz()));
        assertThat(quizService.getById(1L)).isNotNull();
    }
}
```

- No `@SpringBootTest` — no application context loaded
- Mockito for mocking (`@Mock`, `@InjectMocks`)
- Fast (~milliseconds per test)

## Slice Tests — partial context, focused scope

### Controller layer — `@WebMvcTest`

```java
@WebMvcTest(QuizController.class)
class QuizControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    QuizService quizService;

    @Test
    void shouldReturn200() throws Exception {
        when(quizService.getById(1L)).thenReturn(new QuizDto());
        mockMvc.perform(get("/quizzes/1"))
               .andExpect(status().isOk());
    }
}
```

- Loads only web layer — no JPA, no service beans
- Use `@MockBean` for service dependencies

### Repository layer — `@DataJpaTest`

```java
@DataJpaTest
class QuizRepositoryTest {

    @Autowired
    QuizRepository quizRepository;

    @Test
    void shouldPersistQuiz() {
        Quiz saved = quizRepository.save(new Quiz("Java Basics"));
        assertThat(saved.getId()).isNotNull();
    }
}
```

- Loads JPA context only, uses an in-memory H2 database by default
- Transactional — rolls back after each test

## Integration Tests — full context, `@SpringBootTest`

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QuizIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void endToEnd() throws Exception {
        mockMvc.perform(post("/quizzes").contentType(APPLICATION_JSON).content("{}"))
               .andExpect(status().isCreated());
    }
}
```

- Loads the full application context (slow — use sparingly)
- Hit a real database (or `@Testcontainers` for a Docker DB)
- Good for critical end-to-end paths

## Decision rule

| Need | Annotation |
|------|-----------|
| Pure logic, no Spring | plain JUnit + Mockito |
| Controller endpoints | `@WebMvcTest` |
| Database queries | `@DataJpaTest` |
| Full stack | `@SpringBootTest` |

Run a single test: `./gradlew test --tests "com.fsl.quizapp.YourTestClass"`
