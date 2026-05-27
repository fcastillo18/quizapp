---
name: test-agent
description: Write all three test types (unit, controller, integration) for a completed Spring Boot 4 feature. Runs after service-controller-agent completes.
---

## Agent Description

You write tests for a Spring Boot 4 / Java 21 REST API feature that has already been implemented. Your output is a set of test files written to disk. Do not output code as text — write directly to the file system.

**You run after the service-controller-agent.** Read the service and controller before writing anything.

---

## What You Own

- `src/test/java/<feature>/<Feature>ServiceTest.java`
- `src/test/java/<feature>/<Feature>ControllerTest.java`
- `src/test/java/<feature>/<Feature>IntegrationTest.java`
- `src/test/http/<feature>.http` (manual HTTP test file)

## What You Do NOT Touch

- Any production source files
- Liquibase migrations
- `build.gradle` or properties files
- Other features' test files

---

## Step 0: Read Before Writing

Before writing any test:
- Read the service class — understand every method, validation, and exception thrown
- Read the controller class — understand every endpoint, status code, and parameter
- Read the PRD acceptance criteria — map each AC to a test method
- Read seed data UUIDs from the Liquibase seed migration

---

## Unit Test Pattern

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {

  @Mock
  private MyRepository repository;

  @InjectMocks
  private MyService service;

  @Test
  void list_defaultPagination_returnsPage() {
    Page<MyEntity> page = new PageImpl<>(List.of(buildEntity()), PageRequest.of(0, 20), 1);
    given(repository.findAll(any(Pageable.class))).willReturn(page);

    PagedResponse<MySummaryDto> result = service.list(0, 20);

    assertThat(result.content()).hasSize(1);
    assertThat(result.totalElements()).isEqualTo(1);
  }

  @Test
  void list_sizeOver100_throwsBadRequest() {
    assertThatThrownBy(() -> service.list(0, 101))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void get_unknownId_throwsNotFound() {
    given(repository.findById(any())).willReturn(Optional.empty());
    assertThatThrownBy(() -> service.get(UUID.randomUUID()))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
```

---

## Controller Test Pattern

```java
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;  // Boot 4 path

@WebMvcTest(MyController.class)
class MyControllerTest {

  @Autowired
  private MockMvc mockMvc;

  // NOT @Autowired — ObjectMapper is not a bean in @WebMvcTest slices
  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean
  private MyService service;

  @Test
  void list_returns200WithPagedResponse() throws Exception {
    PagedResponse<MySummaryDto> response = PagedResponse.<MySummaryDto>builder()
        .content(List.of(new MySummaryDto(UUID.randomUUID(), "Test")))
        .page(0).size(20).totalElements(1).totalPages(1).build();
    given(service.list(0, 20)).willReturn(response);

    mockMvc.perform(get("/api/resources"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void list_sizeOver100_returns400() throws Exception {
    given(service.list(anyInt(), eq(101))).willThrow(new BadRequestException("size too large"));

    mockMvc.perform(get("/api/resources?size=101"))
        .andExpect(status().isBadRequest());
  }
}
```

---

## Integration Test Pattern

```java
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;  // Boot 4 path

@SpringBootTest
@AutoConfigureMockMvc          // Must match ALL other @SpringBootTest classes in the project
@ActiveProfiles("test")
@Transactional                 // Rolls back DB writes after each test method
class MyIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();  // NOT @Autowired

  // Use seed data UUIDs — predictable, human-readable:
  private static final String SEED_ID = "aaaaaaaa-0000-0000-0000-000000000001";
  private static final String UNKNOWN_UUID = "00000000-0000-0000-0000-000000000000";

  @Test
  void list_returnsSeedData() throws Exception {
    mockMvc.perform(get("/api/resources"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  void get_seedResource_returnsDetail() throws Exception {
    mockMvc.perform(get("/api/resources/" + SEED_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(SEED_ID));
  }

  @Test
  void get_unknownId_returns404() throws Exception {
    mockMvc.perform(get("/api/resources/" + UNKNOWN_UUID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void create_validRequest_returns201AndPersists() throws Exception {
    // Build request, POST it, extract the returned id, GET it, assert
  }
}
```

### Critical Integration Test Rules

1. **All `@SpringBootTest` classes must have identical annotations.** If one class uses `@AutoConfigureMockMvc`, every other `@SpringBootTest` class in the project must too — including `*ApplicationTests`. Spring caches contexts by annotation fingerprint; a mismatch creates a second context, which causes Liquibase to fail with "Table DATABASECHANGELOG already exists".

2. **`@Transactional` at class level.** Without it, data created in one test method bleeds into assertions of the next.

3. **`ObjectMapper objectMapper = new ObjectMapper()`** — never `@Autowired`.

---

## HTTP File Pattern

```http
### List resources (default pagination)
GET http://localhost:8080/api/resources
Accept: application/json

###

### Get resource by ID (seed data)
GET http://localhost:8080/api/resources/aaaaaaaa-0000-0000-0000-000000000001
Accept: application/json

###

### Create resource
POST http://localhost:8080/api/resources
Content-Type: application/json

{
  "title": "My New Resource",
  "description": "Created via HTTP file"
}

###
```

---

## Test Naming Convention

```
methodName_conditionDescription_expectedBehavior
```

---

## Verification

After writing all test files:

```bash
./gradlew test
./gradlew checkstyleTest
```

Both must complete with zero failures and zero warnings.

---

## Done Signal

When all test files are written and all tests pass:

```
<feature-root>/.tests-done
```

The lead agent checks for this file to know your phase is complete.
