---
name: verify
description: Run the full build and test suite and report any failures. Use before committing or after making significant changes.
---

Run `./gradlew clean build` and report the results.

1. Run: `./gradlew clean build`
2. If the build succeeds, confirm: "Build and tests passed."
3. If it fails, show the relevant error output — test names, compilation errors, or Checkstyle violations — with file paths and line numbers when available.
4. Do not proceed with other work until failures are resolved unless the user explicitly says to.
