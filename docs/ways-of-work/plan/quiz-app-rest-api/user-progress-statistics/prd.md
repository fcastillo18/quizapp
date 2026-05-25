# Feature PRD: User Progress & Statistics

## 1. Feature Name

User Progress & Statistics — attempt history, detailed breakdowns, and aggregate performance metrics.

## 2. Epic

- Parent PRD: [`docs/planning/prd.md`](/docs/planning/prd.md)
- User stories covered: US-6, US-7, US-8

## 3. Goal

**Problem:**
After completing quizzes, students have no way to review their history, revisit incorrect answers, or measure improvement over time. Each quiz attempt exists in isolation. Without a progress-tracking layer, the system provides no longitudinal learning value beyond the immediate submission response.

**Solution:**
Three read-only endpoints: a paginated chronological list of all attempts for a user, a detailed per-attempt breakdown showing correctness and explanation for every question, and an aggregate statistics endpoint returning total attempts and average score.

**Impact:**
- Students can track which quizzes they have taken and how their scores changed across retakes.
- Students can review incorrect answers after the fact to reinforce learning.
- A single stats call provides a snapshot of overall performance without requiring the client to aggregate.

## 4. User Personas

**Alex — AI Developer Student**
Reviews attempt history after a study session. Uses the detailed breakdown to understand which questions they got wrong and read the explanation. Checks aggregate stats to see if their average score is improving across sessions.

## 5. User Stories

**US-6**: As a student, I want to view a paginated list of all my past quiz attempts so I can track my history across quizzes.

**US-7**: As a student, I want to view the full question-by-question breakdown of a specific attempt so I can learn from my mistakes.

**US-8**: As a student, I want to see my overall statistics (total attempts and average score) so I can measure my progress at a glance.

## 6. Requirements

### Functional Requirements

**GET /api/users/{userId}/attempts** (US-6)
- Accepts optional query params: `page` (integer, 0-based, default `0`) and `size` (integer, default `20`, max `100`).
- Returns only **completed** attempts (`completedAt IS NOT NULL`), sorted by `completedAt` descending (most recent first).
- Paginated envelope:
  ```json
  {
    "content": [
      {
        "attemptId": "<uuid>",
        "quizId": "<uuid>",
        "quizTitle": "string",
        "score": "4/5",
        "percentage": 80.00,
        "completedAt": "<ISO 8601>"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1
  }
  ```
- Returns HTTP 200 with empty `content` array if the user has no completed attempts.
- Returns HTTP 404 if `userId` does not exist.
- Returns HTTP 400 if `size > 100`.

**GET /api/attempts/{attemptId}** (US-7)
- Returns the full attempt detail:
  ```json
  {
    "attemptId": "<uuid>",
    "userId": "<uuid>",
    "quizId": "<uuid>",
    "quizTitle": "string",
    "startedAt": "<ISO 8601>",
    "completedAt": "<ISO 8601>",
    "score": "4/5",
    "percentage": 80.00,
    "answers": [
      {
        "questionId": "<uuid>",
        "questionText": "string",
        "selectedOptionId": "<uuid>",
        "selectedOptionText": "string",
        "isCorrect": true,
        "explanation": "string"
      }
    ]
  }
  ```
- `answers` are ordered by question `position` ascending.
- Returns HTTP 404 if `attemptId` does not exist.
- In-progress attempts (not yet submitted) return HTTP 404 — only completed attempts are retrievable via this endpoint.

**GET /api/users/{userId}/stats** (US-8)
- Returns aggregate metrics computed over **completed** attempts only:
  ```json
  {
    "userId": "<uuid>",
    "totalAttempts": 7,
    "averageScore": 72.43
  }
  ```
- `averageScore` is the mean of all `percentage` values for completed attempts, rounded to 2 decimal places. Computed server-side using the stored `percentage` values.
- If the user has no completed attempts: `totalAttempts = 0`, `averageScore = 0.00`.
- Returns HTTP 404 if `userId` does not exist.

### Non-Functional Requirements

- All three endpoints must respond within 300ms for users with up to 500 completed attempts (local environment).
- `averageScore` is computed server-side; the API must never return a floating-point precision artifact (e.g., `72.4285714...`). Use `ROUND(AVG(percentage), 2)` in JPQL or compute in the service layer with `BigDecimal` scale 2.
- In-progress attempts (`completedAt IS NULL`) must not appear in any response from this feature's endpoints.
- Error responses use RFC 7807 Problem Details format.
- No write operations — these endpoints are strictly read-only.

## 7. Acceptance Criteria

### US-6: Paginated attempt history

**AC-6.1 — Default pagination with results**
- Given: User `userId` has 3 completed attempts; most recent is "Attempt C"
- When: `GET /api/users/{userId}/attempts`
- Then: HTTP 200; `content` has 3 items ordered by `completedAt` desc (C first); each item has `attemptId`, `quizId`, `quizTitle`, `score`, `percentage`, `completedAt`; `totalElements=3`, `page=0`, `size=20`

**AC-6.2 — Custom page and size**
- Given: User has 25 completed attempts
- When: `GET /api/users/{userId}/attempts?page=1&size=10`
- Then: HTTP 200; `content` has 10 items (attempts 11–20 by recency); `totalElements=25`, `totalPages=3`

**AC-6.3 — No completed attempts**
- Given: User exists but has never submitted a quiz (or only has in-progress attempts)
- When: `GET /api/users/{userId}/attempts`
- Then: HTTP 200; `content` is empty array; `totalElements=0`

**AC-6.4 — In-progress attempts excluded**
- Given: User has 2 completed attempts and 1 in-progress attempt (no `completedAt`)
- When: `GET /api/users/{userId}/attempts`
- Then: HTTP 200; `content` has 2 items only; in-progress attempt is not included

**AC-6.5 — Unknown user**
- When: `GET /api/users/00000000-0000-0000-0000-000000000000/attempts`
- Then: HTTP 404; RFC 7807 error body

**AC-6.6 — Size exceeds maximum**
- When: `GET /api/users/{userId}/attempts?size=101`
- Then: HTTP 400; RFC 7807 error body

### US-7: Detailed attempt breakdown

**AC-7.1 — Full breakdown structure**
- Given: A completed attempt for a 5-question quiz where 4 were answered correctly
- When: `GET /api/attempts/{attemptId}`
- Then: HTTP 200; response has `attemptId`, `userId`, `quizId`, `quizTitle`, `startedAt`, `completedAt`, `score = "4/5"`, `percentage = 80.00`; `answers` array has 5 items, each with `questionId`, `questionText`, `selectedOptionId`, `selectedOptionText`, `isCorrect`, `explanation`

**AC-7.2 — Answers ordered by question position**
- When: `GET /api/attempts/{attemptId}` for a quiz with questions at positions 1, 2, 3, 4, 5
- Then: `answers[0]` corresponds to position 1, `answers[4]` to position 5

**AC-7.3 — Incorrect answer included**
- Given: Attempt where question 3 was answered incorrectly
- When: `GET /api/attempts/{attemptId}`
- Then: `answers[2].isCorrect = false`; `explanation` is still present (showing what the correct answer was about)

**AC-7.4 — In-progress attempt not retrievable**
- Given: An attempt that has been started but not yet submitted
- When: `GET /api/attempts/{attemptId}`
- Then: HTTP 404

**AC-7.5 — Unknown attempt**
- When: `GET /api/attempts/00000000-0000-0000-0000-000000000000`
- Then: HTTP 404

### US-8: Aggregate statistics

**AC-8.1 — Correct average with multiple attempts**
- Given: User has 3 completed attempts with percentages 80.00, 60.00, 100.00
- When: `GET /api/users/{userId}/stats`
- Then: HTTP 200; `totalAttempts = 3`, `averageScore = 80.00`

**AC-8.2 — Non-round average**
- Given: User has 3 attempts with percentages 80.00, 60.00, 77.00
- When: `GET /api/users/{userId}/stats`
- Then: `averageScore = 72.33` (not `72.333333...`)

**AC-8.3 — No completed attempts**
- Given: User exists with no completed attempts
- When: `GET /api/users/{userId}/stats`
- Then: HTTP 200; `totalAttempts = 0`, `averageScore = 0.00`

**AC-8.4 — In-progress attempts excluded from stats**
- Given: User has 2 completed attempts (avg 70.00) and 1 in-progress attempt
- When: `GET /api/users/{userId}/stats`
- Then: `totalAttempts = 2`, `averageScore = 70.00` (in-progress not counted)

**AC-8.5 — Unknown user**
- When: `GET /api/users/00000000-0000-0000-0000-000000000000/stats`
- Then: HTTP 404

## 8. Out of Scope

- Filtering attempt history by quiz ID, date range, or score threshold.
- Sorting attempt history by anything other than `completedAt` descending.
- Per-quiz statistics (e.g., average score on "Agent Fundamentals" specifically) — deferred to v1.1.
- Leaderboard or comparison with other users — deferred to v2.0.
- Attempt deletion or history clearing.
- Progress over time graphs or trend data — client responsibility to compute from the paginated list.
- Retrieving in-progress (not-yet-submitted) attempts.
