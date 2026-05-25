# Feature PRD: Quiz Attempt & Submission

## 1. Feature Name

Quiz Attempt & Submission — starting attempts, submitting answers, scoring, and contextual feedback.

## 2. Epic

- Parent PRD: [`docs/planning/prd.md`](/docs/planning/prd.md)
- User stories covered: US-3, US-4, US-5

## 3. Goal

**Problem:**
Students can browse quizzes but have no mechanism to actually take them. Without an attempt system, there is no record of who answered what, no scoring, and no feedback. Students cannot identify their knowledge gaps or validate their understanding.

**Solution:**
Two endpoints: one to start a new attempt (which returns questions without answers, anchoring the session), and one to submit all answers at once and receive immediate scored results with per-question explanations and a contextual feedback message based on score band. Async email notification is triggered as a side effect of successful submission.

**Impact:**
- Students receive immediate, detailed feedback after every submission.
- Attempt records are persisted, enabling the progress-tracking feature.
- The scoring and feedback logic is the core assessment value of the system.

## 4. User Personas

**Alex — AI Developer Student**
Starts an attempt after reviewing quiz questions, prepares all answers offline, then submits in one batch. Wants to know immediately which questions were wrong and why. Expects a submission to be final — no re-submission of the same attempt.

## 5. User Stories

**US-3**: As a student, I want to start a quiz attempt so I have an active session tied to my user ID and the quiz.

**US-4**: As a student, I want to submit all my answers at once and receive a full scored result so I can understand my performance immediately.

**US-5**: As a student, I want the score feedback message to reflect my performance tier so I get appropriate encouragement or motivation.

## 6. Requirements

### Functional Requirements

**POST /api/quizzes/{quizId}/attempts** (US-3)
- Request body: `{ "userId": "<uuid>" }`
- Creates a new `QuizAttempt` record with `startedAt = now()` and `completedAt = null`.
- Response body:
  ```json
  {
    "attemptId": "<uuid>",
    "startedAt": "<ISO 8601>",
    "quiz": {
      "id": "<uuid>",
      "title": "string",
      "questions": [
        {
          "id": "<uuid>",
          "text": "string",
          "position": 1,
          "options": [{ "id": "<uuid>", "text": "string" }]
        }
      ]
    }
  }
  ```
- `isCorrect` and `explanation` are **never** included in the quiz payload returned here.
- Multiple attempts for the same user/quiz are allowed; each call creates a distinct attempt with a new UUID.
- Returns HTTP 404 if `quizId` does not exist.
- Returns HTTP 404 if `userId` does not exist.

**POST /api/attempts/{attemptId}/submit** (US-4, US-5)
- Request body: array of `{ "questionId": "<uuid>", "selectedOptionId": "<uuid>" }` — one entry per question.
- Processing steps (all within a single transaction):
  1. Look up each `selectedOptionId` against the question's options; mark `isCorrect` per answer.
  2. Calculate `score` (count of correct answers) and `percentage` = `(score / totalQuestions) * 100`, rounded to 2 decimal places.
  3. Determine `feedbackMessage` from score band (see table below).
  4. Persist all `AttemptAnswer` rows, update `QuizAttempt` with `score`, `totalQuestions`, `percentage`, `completedAt = now()`.
- After the transaction commits, trigger async email notification (outside the transaction boundary).
- Response body:
  ```json
  {
    "attemptId": "<uuid>",
    "score": "4/5",
    "percentage": 80.00,
    "feedbackMessage": "Great job! You're getting there!",
    "answers": [
      {
        "questionId": "<uuid>",
        "isCorrect": true,
        "explanation": "string"
      }
    ]
  }
  ```
- Returns HTTP 409 Conflict if the attempt has already been submitted (`completedAt` is not null).
- Returns HTTP 404 if `attemptId` does not exist.
- Returns HTTP 400 if `selectedOptionId` does not belong to the given `questionId`.

**Score Feedback Bands** (US-5)

| Percentage | Feedback Message |
|------------|-----------------|
| ≥ 80% | "Great job! You're getting there!" |
| 60–79% | "Good effort! Keep practicing to improve your score." |
| < 60% | "Keep going! Review the material and try again." |

Boundary values: exactly 80% → "Great job..."; exactly 60% → "Good effort..."

### Non-Functional Requirements

- The full submit response (scoring + DB write + response serialization) must complete within 500ms for quizzes with up to 20 questions (local environment).
- The submit operation is atomic: all `AttemptAnswer` rows and the `QuizAttempt` update succeed together or the transaction rolls back. No partial state is persisted.
- Correct answers (`isCorrect` on options, full option list) are resolved server-side only; they must not appear in any response body.
- The async email trigger must not be inside the database transaction — it fires after the commit to avoid holding the connection during email processing.
- Input is validated with Bean Validation. Errors use RFC 7807 Problem Details format.

## 7. Acceptance Criteria

### US-3: Start attempt

**AC-3.1 — Successful attempt creation**
- Given: Quiz `quizId` with 5 questions exists; user `userId` exists
- When: `POST /api/quizzes/{quizId}/attempts` with `{ "userId": "..." }`
- Then: HTTP 200; response contains `attemptId` (UUID), `startedAt` (ISO 8601), and `quiz` with 5 questions and their options; no `isCorrect` or `explanation` present

**AC-3.2 — Multiple attempts allowed**
- When: `POST /api/quizzes/{quizId}/attempts` is called twice with the same `userId`
- Then: Both return HTTP 200 with different `attemptId` values; both records exist in the database

**AC-3.3 — Unknown quiz**
- When: `POST /api/quizzes/00000000-0000-0000-0000-000000000000/attempts`
- Then: HTTP 404; RFC 7807 error body

**AC-3.4 — Unknown user**
- When: `POST /api/quizzes/{quizId}/attempts` with a `userId` that does not exist
- Then: HTTP 404; RFC 7807 error body

### US-4 & US-5: Submit answers and scoring

**AC-4.1 — Full correct submission (100%)**
- Given: An active attempt for a 5-question quiz; all correct options are known
- When: `POST /api/attempts/{attemptId}/submit` with all 5 correct `selectedOptionId` values
- Then: HTTP 200; `score = "5/5"`, `percentage = 100.00`, `feedbackMessage = "Great job! You're getting there!"`; all `answers[n].isCorrect = true`; each answer includes `explanation`

**AC-4.2 — 4/5 correct (80%)**
- When: Submit with 4 correct and 1 incorrect answer
- Then: `score = "4/5"`, `percentage = 80.00`, `feedbackMessage = "Great job! You're getting there!"`; incorrect answer shows `isCorrect = false` with its explanation

**AC-4.3 — 3/5 correct (60%)**
- When: Submit with 3 correct and 2 incorrect
- Then: `percentage = 60.00`, `feedbackMessage = "Good effort! Keep practicing to improve your score."`

**AC-4.4 — 2/5 correct (40%)**
- When: Submit with 2 correct answers
- Then: `percentage = 40.00`, `feedbackMessage = "Keep going! Review the material and try again."`

**AC-4.5 — Zero correct (0%)**
- When: Submit all incorrect answers
- Then: `score = "0/5"`, `percentage = 0.00`, `feedbackMessage = "Keep going! Review the material and try again."`

**AC-4.6 — Duplicate submission rejected**
- Given: An attempt that has already been submitted (`completedAt` is not null)
- When: `POST /api/attempts/{attemptId}/submit` again
- Then: HTTP 409 Conflict; RFC 7807 error body; database state unchanged

**AC-4.7 — Unknown attempt**
- When: `POST /api/attempts/00000000-0000-0000-0000-000000000000/submit`
- Then: HTTP 404

**AC-4.8 — Invalid option for question**
- When: `selectedOptionId` does not belong to the given `questionId`
- Then: HTTP 400; RFC 7807 error body identifying the invalid pair

**AC-4.9 — Async notification triggered**
- When: A submission succeeds
- Then: A `NotificationLog` row exists with `status = PENDING` or `SENT` for this `attemptId` within 5 seconds of the response

**AC-4.10 — Correct answers not in response**
- When: Any submission response is examined
- Then: No option's `isCorrect` field appears in the response; only per-answer `isCorrect` (whether the student's *selection* was correct) is present

## 8. Out of Scope

- Partial answer submission — all answers must be submitted in one request.
- Editing or re-submitting answers after an attempt is completed.
- Time limits or attempt expiration.
- Submitting answers for only a subset of questions — all questions must have an answer in the payload. (Behavior for missing questions: HTTP 400.)
- Authentication — any caller can submit for any `attemptId`.
- Real-time scoring feedback during the quiz (question by question) — results are provided only upon full submission.
