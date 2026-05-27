---
name: feature-commit-flow
description: Commit a completed feature in story-sized chunks with a consistent, traceable format. Use when preparing commits at the end of a feature or story.
---

Commit a completed feature in story-sized chunks with a consistent, traceable format.

---

## Commit Message Format

```
<type>: <identifier> <slug-description>
```

| Type | Use for |
|---|---|
| `chore` | Build setup, config changes, Liquibase migrations |
| `feat` | Production code implementing a story |
| `test` | Test-only changes with no production code |
| `fix` | Bug fix on an existing story |

**The `<identifier>` depends on your project's tracking system.** Choose one convention and stick to it:

| Tracking system | Identifier format | Example |
|---|---|---|
| Jira (user stories) | `US-N` or `PROJ-N` | `feat: US-1 paginated list endpoint` |
| Jira (tickets) | `TICKET-123` | `feat: TICKET-456 add search filter` |
| GitHub issues | `#123` | `feat: #78 implement user preferences` |
| Linear | `PRO-123` | `feat: PRO-456 oauth integration` |
| None / descriptive | *(omit)* | `feat: add user authentication` |

**Why include an identifier?** Traceability. When someone sees `feat: US-1 quiz list`, they can look up `US-1` in the PRD or issue tracker to understand the original requirements.

---

## Commit Sequence for a Feature

For a feature with multiple stories (e.g., US-1 + US-2 + US-9):

```
1. chore: setup infrastructure and schema migrations
   └── build.gradle changes, application properties, Liquibase changelogs

2. feat: <identifier-1> <endpoint-slug>
   └── entity/repo/dto/service/controller for story 1 + its unit and controller tests

3. feat: <identifier-2> <endpoint-slug>
   └── story 2 production code + its tests

4. feat: <identifier-3> <endpoint-slug>
   └── story 3 production code + its tests

5. test: <identifier-1>/<identifier-2>/<identifier-3> integration tests
   └── integration test class covering all stories' ACs against H2 seed data
```

**Example with user stories:**
```
2. feat: US-1 paginated quiz list
3. feat: US-2 quiz detail endpoint
4. feat: US-9 create quiz with validation
5. test: US-1/US-2/US-9 integration tests
```

**Example with Jira tickets:**
```
2. feat: QUIZ-101 paginated quiz list
3. feat: QUIZ-102 quiz detail endpoint
4. feat: QUIZ-109 create quiz with validation
5. test: QUIZ-101/QUIZ-102/QUIZ-109 integration tests
```

**Example with no ticket system:**
```
2. feat: add paginated quiz list endpoint
3. feat: add quiz detail endpoint
4. feat: add quiz creation with validation
5. test: quiz list/detail/create integration tests
```

Adjust the grouping based on story size. A large story may warrant splitting into a `feat` commit + a `test` commit. A small story can bundle production code and its unit tests in one commit.

---

## Rules

1. **Never mix unrelated stories** in a single commit.
2. **Never mix production code and test-only changes** when there is no associated production code change.
3. **Never include the `.http` test file** in a `feat` commit — include it in a `test` or `chore` commit, or as its own `chore: add HTTP test files` commit.
4. **Never commit broken code.** Run `./gradlew compileJava` before committing. Run `./gradlew build` after the final commit.
5. **Never push to remote** unless explicitly instructed. `git push` is a destructive, shared-state action.
6. **Checkstyle must pass** before any commit. Run `./gradlew checkstyleMain checkstyleTest` — zero warnings.

---

## Pre-commit Checklist

- [ ] `./gradlew compileJava` — zero errors
- [ ] `./gradlew checkstyleMain checkstyleTest` — zero warnings
- [ ] `./gradlew test` — zero failures
- [ ] `git diff --staged` reviewed — no unintended files, no secrets, no debug code
- [ ] Commit message follows the format above

---

## Examples

**With user story identifiers (Jira/PRD):**
```bash
git commit -m "chore: setup project infrastructure and schema migrations"
git commit -m "feat: US-1 paginated resource list endpoint"
git commit -m "feat: US-2 resource detail endpoint"
git commit -m "test: US-1/US-2 unit, controller, and integration tests"
git commit -m "fix: US-3 return 404 instead of 500 on missing resource"
```

**With GitHub issue numbers:**
```bash
git commit -m "chore: setup project infrastructure"
git commit -m "feat: #42 add paginated resource list"
git commit -m "feat: #43 add resource detail endpoint"
git commit -m "test: #42/#43 unit and integration tests"
git commit -m "fix: #67 return 404 on missing resource"
```

**With Jira ticket IDs:**
```bash
git commit -m "chore: setup infrastructure and migrations"
git commit -m "feat: API-101 paginated resource list"
git commit -m "feat: API-102 resource detail endpoint"
git commit -m "test: API-101/API-102 integration tests"
git commit -m "fix: API-205 return 404 on missing resource"
```

**With no ticket system (descriptive only):**
```bash
git commit -m "chore: setup project infrastructure"
git commit -m "feat: add paginated resource list endpoint"
git commit -m "feat: add resource detail endpoint"
git commit -m "test: resource list and detail integration tests"
git commit -m "fix: return 404 on missing resource"
```
