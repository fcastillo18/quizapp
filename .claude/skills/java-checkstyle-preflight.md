---
name: java-checkstyle-preflight
description: Fix all Google Java Style violations before committing. Run after writing or modifying any Java file. Use before every commit.
---

Fix all Google Java Style violations before committing. Run this checklist after writing or modifying any Java file.

---

## Run First

```bash
./gradlew checkstyleMain checkstyleTest
```

Must output: `0 violations`. Any `[WARN]` is a blocker — fix before proceeding.

To target only files you touched:

```bash
./gradlew checkstyleMain 2>&1 | grep "\[WARN\]"
./gradlew checkstyleTest 2>&1 | grep "\[WARN\]"
```

---

## Checklist

### Indentation
- [ ] 2-space indent, no tabs
- [ ] Continuation lines indented +4 spaces from the opening statement

### Line Length
- [ ] No line exceeds 100 characters
- [ ] Break long method chains at `.` — indent continuation lines by +4
- [ ] Break long string concatenations at `+` — avoid them; prefer multi-line String or a local variable

### Javadoc
- [ ] Every `public` class has a Javadoc comment
- [ ] Every `public` method has a Javadoc comment
- [ ] First sentence ends with a period
- [ ] No `@param` or `@return` tags needed for obvious parameters, but add them when non-trivial

```java
/** Returns all active users sorted by creation date. */
public List<UserDto> listUsers() { ... }

/**
 * Creates a new quiz with the given title and questions.
 *
 * @param request the validated create request
 * @return the generated quiz id
 */
public CreatedQuizDto createQuiz(CreateQuizRequest request) { ... }
```

### Imports
- [ ] No wildcard imports (`import java.util.*` is a violation)
- [ ] Import order: static imports first, then `java.*`, then `javax.*`, then `org.*`, then third-party (`com.*`, etc.)
- [ ] Each import group separated by one blank line
- [ ] Alphabetical within each group

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
```

### Braces
- [ ] Opening brace on the same line (`if (x) {` not `if (x)\n{`)
- [ ] Always use braces for `if`/`for`/`while` — even single-line bodies

### Blank Lines
- [ ] One blank line between methods
- [ ] No trailing blank lines at end of file

### Naming
- [ ] Classes: `UpperCamelCase`
- [ ] Methods and variables: `lowerCamelCase`
- [ ] Constants: `UPPER_SNAKE_CASE`
- [ ] Packages: all lowercase, no underscores

---

## Common Violations to Watch

| Violation | Fix |
|---|---|
| Line too long in Javadoc | Break at 100 chars with `*` continuation |
| Missing Javadoc on public record | Add `/** Description. */` above the record |
| Wrong import order | Reorder: static → java → javax → org → com |
| Wildcard import | Expand to explicit imports |
| Tab indentation | Replace tabs with 2 spaces |
| Brace on new line | Move `{` to end of previous line |

---

## Final Gate

After fixing all violations, run the full build:

```bash
./gradlew build
```

Zero test failures + zero checkstyle warnings = ready to commit.
