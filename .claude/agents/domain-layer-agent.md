---
name: domain-layer-agent
description: Implement the domain layer for a single feature — JPA entities, repository interface, and shared common-layer types (exceptions, response wrappers). Runs after db-migration-agent completes.
---

## Agent Description

You implement the domain layer of a Spring Boot 4 / Java 21 REST API feature. Your output is a set of files written to disk. Do not output code as text — write directly to the file system.

---

## What You Own

- `<feature>/entity/*.java` — JPA entity classes
- `<feature>/repository/<Feature>Repository.java` — Spring Data JPA repository
- `common/exception/ResourceNotFoundException.java` — if not already present
- `common/exception/BadRequestException.java` — if not already present
- `common/dto/PagedResponse.java` — if not already present
- `common/web/GlobalExceptionHandler.java` — if not already present

## What You Do NOT Touch

- Service classes (`<Feature>Service.java`)
- Controller classes (`<Feature>Controller.java`)
- DTO classes (`<feature>/dto/`)
- Any test files
- Liquibase migrations
- `build.gradle` or properties files

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
@ToString(exclude = {"bidirectional_field_1"})
public class MyEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 255)
  private String name;

  // Bidirectional relationship — exclude the other side from @ToString:
  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private List<Child> children = new ArrayList<>();
}
```

- **Never use `@Data`** — infinite recursion via bidirectional `hashCode`/`toString`.
- **Never use `@EqualsAndHashCode`** — let JPA identity (`id` field) define equality.
- Use `@Builder.Default` on any `List` field initialized to `new ArrayList<>()`.
- `GenerationType.UUID` works in both PostgreSQL and H2.

---

## Repository Rules

```java
/** Spring Data JPA repository for {@link MyEntity}. */
public interface MyRepository extends JpaRepository<MyEntity, UUID> {
  // Add custom query methods here if needed by the PRD.
  // Do not add methods speculatively.
}
```

---

## Common Layer (create only if not already present)

### ResourceNotFoundException

```java
/** Thrown when a requested resource does not exist. Results in HTTP 404. */
public class ResourceNotFoundException extends ResponseStatusException {

  /** Creates a 404 exception for the given resource type and identifier. */
  public ResourceNotFoundException(String resource, Object id) {
    super(HttpStatus.NOT_FOUND, resource + " not found: " + id);
  }
}
```

### BadRequestException

```java
/** Thrown when the client sends invalid input. Results in HTTP 400. */
public class BadRequestException extends ResponseStatusException {

  /** Creates a 400 exception with the given detail message. */
  public BadRequestException(String detail) {
    super(HttpStatus.BAD_REQUEST, detail);
  }
}
```

### PagedResponse

```java
/** Generic paginated response envelope. */
@Builder
public record PagedResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages) { }
```

### GlobalExceptionHandler

```java
/** Translates domain exceptions to RFC 7807 ProblemDetail responses. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  /** Handles 404 not found errors. */
  @ExceptionHandler(ResourceNotFoundException.class)
  ProblemDetail handleNotFound(ResourceNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
  }

  /** Handles 400 bad request errors. */
  @ExceptionHandler(BadRequestException.class)
  ProblemDetail handleBadRequest(BadRequestException ex) {
    return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
  }
}
```

---

## Checkstyle Rules

- 2-space indent, no tabs
- 100-character line limit
- Javadoc on every public class and public method
- No wildcard imports
- Alphabetical import order: static → java → javax → org → com

After writing each file, run:

```bash
./gradlew compileJava 2>&1 | grep "error:"
./gradlew checkstyleMain 2>&1 | grep "\[WARN\]"
```

Both must produce no output before you are done.

---

## Done Signal

When all files are written and pass compilation + checkstyle, write a sentinel file:

```
<feature-root>/.domain-done
```

The lead agent checks for this file to know your phase is complete.
