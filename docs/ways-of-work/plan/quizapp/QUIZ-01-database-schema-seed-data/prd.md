# QUIZ-01 — Database Schema & Seed Data Setup

## Epic
[QuizApp REST API PRD](../../../prd.md)

**Depends on:** — (none)  
**Required by:** [QUIZ-02](../QUIZ-02-list-available-quizzes/prd.md), [QUIZ-03](../QUIZ-03-get-quiz-details/prd.md), [QUIZ-04](../QUIZ-04-create-quiz/prd.md), [QUIZ-05](../QUIZ-05-start-quiz-attempt/prd.md), [QUIZ-07](../QUIZ-07-async-email-notification/prd.md), [QUIZ-08](../QUIZ-08-unit-integration-test-suite/prd.md)

---

## Goal

**Problem:** The application has no persistent data layer. Without a defined schema and seed data, no endpoint can be developed or tested against real records.

**Solution:** Deliver Liquibase changesets that create all seven tables required by the domain model, plus a data changeset that seeds two quizzes (5+ questions each) and two users. This ticket is the foundation every other QUIZ-XX ticket depends on.

**Impact:** Unblocks all feature development. Enables integration tests to run against realistic data from day one.

---

## User Personas

| Persona | Relevance |
|---|---|
| **Developer** | Needs a working, repeatable schema to develop against locally and in CI. |
| **Integration Test** | Needs seed data in the `test` profile (H2) to assert real query results. |

---

## User Stories

- As a developer, I want Liquibase to create all tables on startup so that I never have to run DDL manually.
- As a developer, I want the database to be pre-populated with quiz and user data so that I can test endpoints immediately without manual inserts.
- As an integration test, I want the H2 in-memory database to have the same schema and seed data as PostgreSQL so that tests are meaningful.

---

## Requirements

### Functional Requirements

- Create a Liquibase master changelog at `src/main/resources/db/changelog/db.changelog-master.yaml` that includes all changeset files in order.
- Schema changeset (`db.changelog-1.0-initial-schema.yaml`) must create:
  - `users` — `id` (UUID PK), `name` (VARCHAR 255 NOT NULL), `email` (VARCHAR 255 NOT NULL UNIQUE), `created_at` (TIMESTAMPTZ NOT NULL DEFAULT now())
  - `quizzes` — `id` (UUID PK), `title` (VARCHAR 255 NOT NULL), `description` (TEXT), `created_at` (TIMESTAMPTZ NOT NULL DEFAULT now())
  - `questions` — `id` (UUID PK), `quiz_id` (UUID FK → quizzes), `text` (TEXT NOT NULL), `explanation` (TEXT NOT NULL), `position` (INT NOT NULL), `correct_option_id` (UUID nullable initially — updated after options are inserted)
  - `options` — `id` (UUID PK), `question_id` (UUID FK → questions), `text` (VARCHAR 500 NOT NULL), `position` (INT NOT NULL)
  - Add FK constraint `questions.correct_option_id → options.id` after both tables exist.
  - `attempts` — `id` (UUID PK), `user_id` (UUID FK → users), `quiz_id` (UUID FK → quizzes), `started_at` (TIMESTAMPTZ NOT NULL DEFAULT now()), `submitted_at` (TIMESTAMPTZ nullable), `score` (INT nullable), `percentage` (NUMERIC(5,2) nullable)
  - `answers` — `id` (UUID PK), `attempt_id` (UUID FK → attempts), `question_id` (UUID FK → questions), `selected_option_id` (UUID FK → options), `correct` (BOOLEAN NOT NULL)
  - `notifications` — `id` (UUID PK), `attempt_id` (UUID FK → attempts), `status` (VARCHAR 20 NOT NULL — values: `PENDING`, `SENT`, `FAILED`), `created_at` (TIMESTAMPTZ NOT NULL DEFAULT now()), `updated_at` (TIMESTAMPTZ NOT NULL DEFAULT now())
- Seed data changeset (`db.changelog-1.1-seed-data.yaml`) must insert:
  - 2 users with `name` and `email`.
  - 2 quizzes — e.g., "LLM Fundamentals" and "Agent Design Patterns".
  - 5+ questions per quiz, each with 4 options and one designated correct option.
  - Seed changeset must be idempotent (use `onFail: MARK_RAN` or `runOnChange: false` guards).
- `spring.jpa.hibernate.ddl-auto` must be set to `validate` in all profiles — Hibernate must not manage schema.

### Non-Functional Requirements

- All changesets use standard ANSI SQL — no PostgreSQL-specific types (e.g., use `TIMESTAMPTZ` / `TIMESTAMP WITH TIME ZONE`, which H2 also supports in compatibility mode).
- H2 dialect compatibility: `application-test.properties` must set `spring.datasource.url` with `MODE=PostgreSQL` and `DEFAULT_NULL_ORDERING=HIGH`.
- Changeset IDs follow format: `<version>-<short-description>` (e.g., `1.0-create-users-table`).

---

## Acceptance Criteria

- [ ] `./gradlew bootRun` starts without Liquibase errors against a fresh PostgreSQL container.
- [ ] `./gradlew test` starts without schema errors against H2.
- [ ] `SELECT COUNT(*) FROM quizzes` returns 2 after startup.
- [ ] Each quiz has at least 5 questions; each question has exactly 4 options with one marked correct.
- [ ] `SELECT COUNT(*) FROM users` returns 2 after startup.
- [ ] Re-running migrations on a populated database does not duplicate seed data.
- [ ] `spring.jpa.hibernate.ddl-auto=validate` passes on startup (schema matches entities).

---

## Out of Scope

- Indexes beyond PKs and FKs.
- Data archival or soft-delete columns.
- Any migration beyond the initial schema + seed data (future migrations are other tickets).
