
## QuizApp
Build a **RESTful API** for a Quiz App that allows users to take quizzes on AI development concepts, track their progress, and receive feedback.

The system should focus on backend logic, data persistence, and asynchronous workflows.

### Required API Endpoints

**Quiz Management**

- Retrieve all available quizzes (id, title, description only)
- Get full quiz details, including all questions and options (but NOT correct answers)
- Create a new quiz

**Quiz Attempt & Submission**

- Start a new quiz attempt for a user
- Submit answers and get results with scoring and feedback. Provide contextual performance feedback based on the user's score (e.g., 80%+ encouraging messages, 60-79% motivational feedback, <60% encouragement to improve)
- **Trigger asynchronous email notification** when a quiz is completed, sending the user a results summary

**User Progress**

- Get all quiz attempts for a specific user
- Get detailed results of a particular attempt (Attempt ID, User ID, Quiz ID and title,
- Timestamp, Overall score, and question-by-question breakdown with correctness and explanations)
- Get aggregate statistics for a user (total attempts, average score)

**Persistence & Data** Your API should handle and store:

- Quiz information (title, description, questions)
- Questions with multiple choice options, correct answers, and explanations
- User identifiers and contact information (email)
- Attempt records (which user took which quiz, when, and their score)
- Individual answer submissions for each attempt
- Notification tracking (status, timestamps)

### **Asynchronous Communication Requirement**

When a user completes a quiz, the system must **asynchronously** communicate with an email notification service to send a results summary.

**Requirements:**

- Demonstrate asynchronous communication patterns
- Mock the email service **(you don't actually need to send emails)**
- Submission must succeed even if notification fails
- Track notification status appropriately

**Email notification should include:**

- User's name and email
- Quiz title
- Score (correct answers / total questions)
- Percentage score
- Performance feedback message
- Timestamp of completion

<aside>

**API Usage Example:**

1. A user sends a request to retrieve all available quiz categories from the API
2. The API returns three quiz categories. The user selects "Agent Fundamentals"
3. The user sends a request to start a new quiz attempt for the "Agent Fundamentals" quiz
4. The API responds with the attempt details and all 5 questions with their options (but not the correct answers)
5. The user reviews the questions and prepares answers for all 5 questions
6. The user submits the answers to the API
7. The API processes the answers, calculates a score of 4/5 (80%), and returns:
    - Overall score and percentage
    - Performance feedback: "Good job! You're getting there!"
    - For each question: correct/incorrect status and explanation
    - The API asynchronously triggers a notification to send the user an email with the results
8. The email notification service (mocked) processes the request and simulates sending the email
9. The user sends a request to view quiz attempt history
10. The user decides to retake the quiz and sends another request to start a new attempt for the same quiz
11. The API creates a new attempt record while preserving the previous attempt history
</aside>

## Acceptance Criteria

- All required endpoints are implemented and functional
- Quiz attempts are persisted and retrievable
- Async email notification is implemented (mocked)
- Notification failures do not break quiz submission
- Notification status is tracked
- At least **2 quizzes with 5+ questions each** are preloaded
- Basic unit tests cover:
    - Scoring logic
    - Feedback calculation
    - Async workflow behavior
- Submit a zip file of your project:
    - Remove unnecessary folders (e.g. `node_modules`, `vendor`, `venv`, `.gradle`, `build`)
    - Include all required files (e.g., `.chat-history/log.md`)
