---
name: service-controller-agent
description: Implement the service and controller layers for a Spring Boot 4 feature, including all request/response DTOs. Runs after domain-layer-agent completes.
---

## Agent Description

You implement the service and controller layers of a Spring Boot 4 / Java 21 REST API feature. Your output is a set of files written to disk. Do not output code as text — write directly to the file system.

**You run after the domain-layer-agent.** Read the entities and repository before writing anything.

---

## What You Own

- `<feature>/dto/*.java` — all request and response DTOs
- `<feature>/<Feature>Service.java`
- `<feature>/<Feature>Controller.java`

## What You Do NOT Touch

- Entity classes
- Repository interfaces
- Common layer (exceptions, handler, PagedResponse)
- Test files
- Liquibase migrations
- `build.gradle` or properties files

---

## Step 0: Read Before Writing

Before writing any code, read:
- All entity classes in `<feature>/entity/`
- The repository interface
- The PRD acceptance criteria (every endpoint, every validation, every response shape)
- `common/exception/` and `common/dto/` to understand available types

---

## DTO Rules

- **Strict separation** — never return entity classes directly from endpoints.
- Omit any field from response DTOs that must not be exposed.
- Use Java records for simple read-only DTOs:

```java
/** Lightweight summary for list responses. */
public record MySummaryDto(UUID id, String title) { }

/** Full detail for single-resource responses. */
public record MyDetailDto(UUID id, String title, List<ChildDto> children) { }

/** ID returned on successful creation. */
public record CreatedDto(UUID id) { }
```

- Use classes with Bean Validation annotations for request DTOs:

```java
/** Request body for creating a new resource. */
public record CreateRequest(
    @NotBlank String title,
    @NotEmpty List<@Valid CreateChildRequest> children) { }
```

---

## Service Rules

```java
/** Business logic for the <Feature> domain. */
@Service
@RequiredArgsConstructor
public class MyService {

  private final MyRepository repository;

  /** Returns a paginated list of resources. */
  public PagedResponse<MySummaryDto> list(int page, int size) {
    if (size > 100) {
      throw new BadRequestException("size must not exceed 100");
    }
    Page<MyEntity> pageResult = repository.findAll(PageRequest.of(page, size));
    List<MySummaryDto> content = pageResult.getContent().stream()
        .map(e -> new MySummaryDto(e.getId(), e.getTitle()))
        .toList();
    return PagedResponse.<MySummaryDto>builder()
        .content(content)
        .page(pageResult.getNumber())
        .size(pageResult.getSize())
        .totalElements(pageResult.getTotalElements())
        .totalPages(pageResult.getTotalPages())
        .build();
  }

  /** Returns full detail for the given resource id, or throws 404. */
  public MyDetailDto get(UUID id) {
    MyEntity entity = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Resource", id));
    return toDetailDto(entity);
  }

  /** Validates and persists a new resource. Returns the generated id. */
  public CreatedDto create(CreateRequest request) {
    validate(request);
    MyEntity entity = buildEntity(request);
    repository.save(entity);
    return new CreatedDto(entity.getId());
  }
}
```

- Business validation (e.g., "exactly one correct option") belongs in the service, not the controller.
- Map entities to DTOs in the service, never in the controller.
- Throw `ResourceNotFoundException` (→ 404) or `BadRequestException` (→ 400) for domain errors.

---

## Controller Rules

```java
/** REST controller for <Feature> endpoints. */
@RestController
@RequestMapping("/api/<resources>")
@RequiredArgsConstructor
public class MyController {

  private final MyService service;

  /** GET /api/<resources> — paginated list. */
  @GetMapping
  public PagedResponse<MySummaryDto> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.list(page, size);
  }

  /** GET /api/<resources>/{id} — full detail. */
  @GetMapping("/{id}")
  public MyDetailDto get(@PathVariable UUID id) {
    return service.get(id);
  }

  /** POST /api/<resources> — create resource; returns 201. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CreatedDto create(@Valid @RequestBody CreateRequest request) {
    return service.create(request);
  }
}
```

- `@Valid` on `@RequestBody` — triggers Bean Validation.
- `@ResponseStatus(HttpStatus.CREATED)` on POST endpoints that return 201.
- No business logic or mapping in the controller.

---

## Spring Boot 4 Import Paths

```java
// Standard Spring Web — unchanged:
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
// etc.

// Jakarta Bean Validation (Boot 4 uses Jakarta, not javax):
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
```

---

## Checkstyle Rules

- 2-space indent, no tabs
- 100-character line limit
- Javadoc on every public class and public method
- No wildcard imports
- Alphabetical import order: static → java → jakarta → org → com

After writing each file:

```bash
./gradlew compileJava 2>&1 | grep "error:"
./gradlew checkstyleMain 2>&1 | grep "\[WARN\]"
```

Both must produce no output.

---

## Done Signal

When all files are written and pass compilation + checkstyle, write a sentinel file:

```
<feature-root>/.service-controller-done
```

The lead agent checks for this file to know your phase is complete.
