# QuizApp Story Dependency Graph

---

## Section A — Dependency Table

| Story Key | Title | Phase | Depends On | Required By |
|---|---|---|---|---|
| [QUIZ-01](./QUIZ-01-database-schema-seed-data/prd.md) | Database Schema & Seed Data | 1 | — (none) | QUIZ-02, QUIZ-03, QUIZ-04, QUIZ-05, QUIZ-07, QUIZ-08 |
| [QUIZ-02](./QUIZ-02-list-available-quizzes/prd.md) | List Available Quizzes | 1 | QUIZ-01 | QUIZ-08 |
| [QUIZ-03](./QUIZ-03-get-quiz-details/prd.md) | Get Quiz Details | 1 | QUIZ-01 | QUIZ-08 |
| [QUIZ-04](./QUIZ-04-create-quiz/prd.md) | Create Quiz | 1 | QUIZ-01 | QUIZ-08 |
| [QUIZ-05](./QUIZ-05-start-quiz-attempt/prd.md) | Start Quiz Attempt | 1 | QUIZ-01 | QUIZ-06, QUIZ-08, QUIZ-09, QUIZ-10, QUIZ-11 |
| [QUIZ-06](./QUIZ-06-submit-answers-scoring/prd.md) | Submit Answers with Scoring & Feedback | 1 | QUIZ-01, QUIZ-05, QUIZ-07 | QUIZ-08, QUIZ-09, QUIZ-10, QUIZ-11 |
| [QUIZ-07](./QUIZ-07-async-email-notification/prd.md) | Async Email Notification Service | 1 | QUIZ-01 | QUIZ-06, QUIZ-08 |
| [QUIZ-08](./QUIZ-08-unit-integration-test-suite/prd.md) | Unit & Integration Test Suite | 1 | QUIZ-01 through QUIZ-07 | — (none) |
| [QUIZ-09](./QUIZ-09-user-attempt-history/prd.md) | User Attempt History | 2 | QUIZ-01, QUIZ-05, QUIZ-06 | — (none) |
| [QUIZ-10](./QUIZ-10-detailed-attempt-results/prd.md) | Detailed Attempt Results | 2 | QUIZ-01, QUIZ-05, QUIZ-06 | — (none) |
| [QUIZ-11](./QUIZ-11-user-aggregate-statistics/prd.md) | User Aggregate Statistics | 2 | QUIZ-01, QUIZ-05, QUIZ-06 | — (none) |

---

## Section B — ASCII Dependency Graph

```
QUIZ-01 (DB Schema & Seed Data)
├── QUIZ-02 (List Quizzes)                    ──► QUIZ-08
├── QUIZ-03 (Get Quiz Details)                ──► QUIZ-08
├── QUIZ-04 (Create Quiz)                     ──► QUIZ-08
├── QUIZ-05 (Start Attempt)
│   └── QUIZ-06 (Submit + Scoring)
│       ├── QUIZ-09 (Attempt History)
│       ├── QUIZ-10 (Detailed Results)
│       └── QUIZ-11 (Aggregate Stats)
├── QUIZ-07 (Async Email)
│   └── QUIZ-06 (see above — also depends on QUIZ-05)
└── QUIZ-08 (Test Suite) [depends on all Phase 1: QUIZ-01 through QUIZ-07]
```

**Notes:**
- QUIZ-06 has three direct dependencies: QUIZ-01, QUIZ-05, and QUIZ-07.
- QUIZ-08 is a capstone ticket that depends on every Phase 1 story; it has no downstream dependents.
- QUIZ-09, QUIZ-10, and QUIZ-11 are leaf nodes (Phase 2) — nothing depends on them.

---

## Section C — Implementation Order Recommendation

1. **QUIZ-01 — Database Schema & Seed Data**
   Rationale: All other tickets require the schema and seed data to exist. This must be delivered and verified first before any endpoint work begins.

2. **QUIZ-07 — Async Email Notification Service**
   Rationale: QUIZ-06 (Submit + Scoring) has a hard dependency on QUIZ-07 because it invokes `NotificationService` after submission. Delivering the notification interface and mock early unblocks QUIZ-06 and avoids a last-minute integration.

3. **QUIZ-02 — List Available Quizzes**
   Rationale: Simplest read-only endpoint; good for validating the controller-service-repository stack is wired correctly before tackling more complex stories.

4. **QUIZ-03 — Get Quiz Details**
   Rationale: Shares the `QuizService` / `QuizRepository` stack with QUIZ-02. Delivering these together reduces context-switching and tests the security constraint (no `correctOptionId` in response) early.

5. **QUIZ-04 — Create Quiz**
   Rationale: The write-side complement to QUIZ-02/03. Integration tests for QUIZ-08 use `POST /quizzes` to create controlled test fixtures — having this endpoint early reduces test data setup complexity.

6. **QUIZ-05 — Start Quiz Attempt**
   Rationale: Prerequisite for QUIZ-06. Introduces the `attempts` table and the `AttemptService`. Must be stable before submission logic is built.

7. **QUIZ-06 — Submit Answers with Scoring & Feedback**
   Rationale: Core business value. Depends on QUIZ-01, QUIZ-05, and QUIZ-07 all being complete. The `ScoringService` and feedback logic are unit-tested in QUIZ-08. Delivers the main user outcome (score + per-question feedback).

8. **QUIZ-08 — Unit & Integration Test Suite**
   Rationale: Capstone Phase 1 ticket. By this point all Phase 1 endpoints exist and the full happy-path integration test can be written end-to-end. Coverage target (≥ 80% service layer) is verified here.

9. **QUIZ-09 — User Attempt History**
   Rationale: Phase 2 starts here. Read-only list endpoint against existing data; straightforward to implement once Phase 1 is stable.

10. **QUIZ-10 — Detailed Attempt Results**
    Rationale: Richer read endpoint that joins several tables. Can be developed in parallel with QUIZ-09 and QUIZ-11 once QUIZ-06 data is in place.

11. **QUIZ-11 — User Aggregate Statistics**
    Rationale: Single aggregate query endpoint. Lightweight implementation; good candidate for parallel development alongside QUIZ-09 and QUIZ-10.

---

## Section D — Phase Summary

### Phase 1 — MVP

| Story | One-line Description |
|---|---|
| QUIZ-01 | Create all database tables via Liquibase and seed two quizzes with users. |
| QUIZ-02 | `GET /quizzes` — return a lightweight list of all quizzes (id, title, description). |
| QUIZ-03 | `GET /quizzes/{id}` — return full quiz with questions and options; no correct answers. |
| QUIZ-04 | `POST /quizzes` — create a new quiz with questions, options, and correct answer index. |
| QUIZ-05 | `POST /quizzes/{id}/attempts` — start a quiz session and receive questions for answering. |
| QUIZ-06 | `POST /attempts/{id}/submit` — score answers, return feedback, and trigger async notification. |
| QUIZ-07 | Implement `MockEmailService` with `@Async` + `ThreadPoolTaskExecutor` for decoupled notifications. |
| QUIZ-08 | Layered test suite: unit tests for scoring/feedback/async, integration tests for full API flow. |

### Phase 2 — Extensions

| Story | One-line Description |
|---|---|
| QUIZ-09 | `GET /users/{id}/attempts` — list all past attempts for a user with quiz title and scores. |
| QUIZ-10 | `GET /attempts/{id}` — retrieve full per-question breakdown of a submitted attempt. |
| QUIZ-11 | `GET /users/{id}/stats` — return total completed attempts and average score for a user. |
