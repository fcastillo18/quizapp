---
name: spring-boot-4-tests
description: Write unit, controller, and integration tests for a completed Spring Boot 4 feature. Use after implementing a feature to ensure full test coverage of all acceptance criteria.
---

Write unit, controller, and integration tests for a completed Spring Boot 4 feature.

---

## Three Test Types

| Type | Annotation | Context loaded | Use for |
|---|---|---|---|
| Unit | `@ExtendWith(MockitoExtension.class)` | None | Service logic, business rules |
| Controller | `@WebMvcTest(MyController.class)` | MVC slice only | HTTP layer, request/response shape |
| Integration | `@SpringBootTest` + `@AutoConfigureMockMvc` | Full context + H2 | End-to-end AC validation |

---

## Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {

  @Mock
  private MyRepository repository;

  @InjectMocks
  private MyService service;

  @Test
  void methodName_conditionDescription_expectedBehavior() {
    // arrange, act, assert
  }
}
```

- No `@SpringBootTest` — no application context.
- Test each business rule independently (validation, mapping, edge cases).
- Mock all dependencies via `@Mock` / `@InjectMocks`.

---

## Controller Tests

```java
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;  // Boot 4 path

@WebMvcTest(MyController.class)
class MyControllerTest {

  @Autowired
  private MockMvc mockMvc;

  // NOT @Autowired — ObjectMapper is not a bean in @WebMvcTest slices
  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean  // org.springframework.test.context.bean.override.mockito.MockitoBean
  private MyService service;

  @Test
  void getResource_validId_returns200() throws Exception {
    given(service.get(any())).willReturn(someDto());

    mockMvc.perform(get("/api/resources/{id}", UUID.randomUUID()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").isString());
  }
}
```

Key rules:
- Import `@WebMvcTest` from `org.springframework.boot.webmvc.test.autoconfigure` (Boot 4).
- `ObjectMapper objectMapper = new ObjectMapper()` — never `@Autowired`.
- `@MockitoBean` (not `@MockBean`) to stub the service layer.

---

## Integration Tests

```java
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;  // Boot 4 path

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional  // rolls back all DB writes after each test method
class MyIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();
}
```

### Critical Rules

1. **Identical annotations across all `@SpringBootTest` classes.** If any class uses `@AutoConfigureMockMvc`, all must. Spring caches contexts by annotation fingerprint — mismatches cause a second context to start, which causes Liquibase to fail with "Table DATABASECHANGELOG already exists".

2. **`@Transactional` at class level.** Prevents test data from one test bleeding into assertions of another.

3. **`ObjectMapper objectMapper = new ObjectMapper()`** — never `@Autowired`.

4. **Test against seed data.** Use fixed, predictable UUIDs in seed data so integration tests can assert specific values without creating data.

5. Cover all acceptance criteria — every AC in the PRD should map to at least one test method.

---

## Test Naming Convention

```
methodName_conditionDescription_expectedBehavior
```

Examples:
```java
void listQuizzes_defaultPagination_returnsSeedData()
void listQuizzes_sizeOver100_returns400()
void getQuiz_unknownId_returns404()
void createQuiz_blankTitle_returns400()
void createQuiz_validRequest_persistsAndRetrievable()
```

---

## Verification Checklist

- [ ] Unit tests cover all service-layer business rules
- [ ] Controller tests verify HTTP status codes, request validation, and response shape
- [ ] Integration tests cover every PRD acceptance criterion
- [ ] No `@Autowired ObjectMapper` anywhere
- [ ] All `@SpringBootTest` classes have identical annotations
- [ ] `@Transactional` on all integration test classes that write to the DB
- [ ] Run `./gradlew test` — all tests pass, zero failures
- [ ] Run `./gradlew checkstyleTest` — zero warnings
