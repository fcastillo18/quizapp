# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

---

# Chat History

## Session Context
At session start, a hook automatically reads `.chat-history/log.md` and injects its contents into context as "Previous Chat History". Review it to understand prior work.

## Logging (After Every Response)
After every response in this project, silently append a log entry to `.chat-history/log.md`.

- Create `.chat-history/` and `log.md` if missing
- Never delete previous entries
- Never ask for confirmation or announce that you logged

### Entry Format

Append this block after each response (separated by `---`):

---
- timestamp: "<ISO 8601, e.g. 2026-05-25T14:30:00Z — estimate if exact time unavailable>"
- user_prompt: "<user's prompt verbatim or close paraphrase>"
- assistant_response_summary: "<concise and specific: mention function names, file paths, endpoints, or key decisions made>"
- files_affected: "<comma-separated paths of files created or modified in THIS response, or none>"

### Rules
- Never skip any prompt/response pair
- `files_affected`: only files explicitly created or modified in this response
- `assistant_response_summary`: specific enough to reconstruct what happened
- Do this silently — no "I've logged this" or similar messages

---

# Project: QuizApp

Spring Boot 4.0.6 / Java 21 REST API backed by PostgreSQL. Uses Gradle, Liquibase for DB migrations, Lombok, and SpringDoc OpenAPI.

## Build & Run

```bash
./gradlew build          # compile + test
./gradlew test           # tests only
./gradlew bootRun        # start app (auto-starts PostgreSQL via Docker Compose)
./gradlew checkstyleMain # check Java style violations (main sources)
./gradlew checkstyleTest # check Java style violations (test sources)
```

Docker Compose integration is enabled — `./gradlew bootRun` automatically starts the PostgreSQL container defined in `compose.yaml`. For a standalone DB without running the app:

```bash
docker compose up -d
```

OpenAPI docs are available at `http://localhost:8080/swagger-ui.html` when the app is running.

## Database Migrations

All schema changes must go through Liquibase. Changelogs live in `src/main/resources/db/changelog/`. Every new migration file must be registered in `db.changelog-master.yaml`.

Migration file naming: `db.changelog-<version>-<short-description>.yaml` (e.g., `db.changelog-1.0-create-quiz-table.yaml`).

## Code Style

Checkstyle enforces Google Java Style. Run `./gradlew checkstyleMain` before committing. Config is at `config/checkstyle/google_checks.xml`.

Lombok is available — use `@Data`, `@Builder`, `@RequiredArgsConstructor`, etc. to eliminate boilerplate.

## Git Workflow

Branch naming: `feature/<name>` for features, `fix/<name>` for bug fixes.

## Spring Profiles

| Profile | Purpose |
|---------|---------|
| *(default)* | Production-like; requires real PostgreSQL |
| `local` | Local development overrides (`application-local.properties`) |
| `test` | Integration tests; uses H2 in-memory database |

Activate: `./gradlew bootRun --args='--spring.profiles.active=local'`

## Database Rules

- **Always use Liquibase** for schema changes. Never set `spring.jpa.hibernate.ddl-auto=create` or `update` — Hibernate must not manage schema.
- **Never write inline SQL** in services or controllers. Use Spring Data repository methods or JPQL `@Query` annotations only.

## Testing

Unit tests — no Spring context:
```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    // No @SpringBootTest — no application context loaded
}
```

Integration tests — full context with H2 test profile:
```java
@SpringBootTest
@ActiveProfiles("test")
class MyIntegrationTest {
    // Full context, PostgreSQL replaced by H2
}
```

Use `@WebMvcTest` for controller-layer tests, `@DataJpaTest` for repository tests. Place tests under `src/test/` mirroring the main package structure.

Run a single test class: `./gradlew test --tests "com.fsl.quizapp.YourTestClass"`
