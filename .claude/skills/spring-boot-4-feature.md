---
name: spring-boot-4-feature
description: Implement a single Spring Boot 4 / Java 21 feature end-to-end — entities, repository, service, controller. Use when starting implementation of a new feature or PRD story.
---

Implement a single Spring Boot 4 / Java 21 feature end-to-end: entities, repository, service, controller.

---

## Pre-flight

Before writing any code:
1. Read the PRD acceptance criteria — understand what endpoints return, what they reject, and what they must NOT expose.
2. Read existing entities and repositories to understand naming conventions already in place.
3. Confirm the base package (e.g., `com.example.myapp`). All new code lives under `<base-package>.<feature>/`.

---

## Package Structure

Use feature-based layout, not layer-based:

```
<base-package>/
  <feature>/
    entity/         JPA entities
    repository/     Spring Data repos
    dto/            Request and Response DTOs (strict separation — never reuse entity classes)
    <Feature>Service.java
    <Feature>Controller.java
  common/
    exception/      ResourceNotFoundException, BadRequestException
    web/            GlobalExceptionHandler
    dto/            PagedResponse<T>
```

---

## Entity Rules

```java
@Entity
@Table(name = "table_name")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"bidirectional_field_1", "bidirectional_field_2"})
public class MyEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  // Use @Builder.Default on List fields:
  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private List<Child> children = new ArrayList<>();
}
```

- **Never use `@Data`** on JPA entities.
- **Never use `@EqualsAndHashCode`** on entities.
- `GenerationType.UUID` works in both PostgreSQL and H2.

---

## DTO Rules

- Separate request DTOs from response DTOs. Never return entity classes directly.
- Omit any field from response DTOs that must not be exposed (e.g., `isCorrect`, passwords, internal flags).
- Use Java records for DTOs when they are read-only or simple:

```java
public record MyResponseDto(UUID id, String title) { }
```

- Use `@NotBlank`, `@NotEmpty`, `@Size` on request DTOs for Bean Validation.

---

## Service Rules

- `@Service` + `@RequiredArgsConstructor` (Lombok injects via constructor).
- Validate business rules (e.g., "exactly one correct option") in the service, not the controller.
- Throw `ResourceNotFoundException` for 404 cases, `BadRequestException` for 400 cases.
- Map entities to DTOs in the service — never in the controller.
- Paginated lists: accept `int page, int size`, validate `size <= 100`, delegate to `PageRequest.of(page, size)`.

---

## Controller Rules

- `@RestController` + `@RequestMapping("/api/<resource>")` + `@RequiredArgsConstructor`.
- Use `@ResponseStatus(HttpStatus.CREATED)` on POST endpoints that return 201.
- Use `@Valid` on `@RequestBody` parameters to trigger Bean Validation.
- Never put business logic or mapping logic in the controller.

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public CreatedDto create(@Valid @RequestBody CreateRequest request) {
  return service.create(request);
}
```

---

## Spring Boot 4 Import Paths

```java
// Test annotations — Boot 4 moved these:
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

// These are unchanged from Boot 3:
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
```

---

## Verification Checklist

After writing each class:

- [ ] Run `./gradlew compileJava 2>&1 | grep "error:"` — must be empty
- [ ] Run `./gradlew checkstyleMain 2>&1 | grep "\[WARN\]"` — must be empty
- [ ] No entity class appears in a response DTO
- [ ] No `@Data` on any `@Entity` class
- [ ] All public types and methods have Javadoc
- [ ] Lines ≤ 100 characters, 2-space indent, no tabs
