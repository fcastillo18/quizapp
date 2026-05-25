---
name: new-migration
description: Scaffold a new Liquibase migration file and register it in the master changelog. Use when adding or altering DB schema.
disable-model-invocation: false
---

Create a new Liquibase migration for this project.

## Steps

1. Ask the user (if not provided via $ARGUMENTS): what schema change is needed? (e.g., "create quiz table", "add column score to questions")

2. Determine the next version number by reading `src/main/resources/db/changelog/db.changelog-master.yaml` and checking existing includes.

3. Create the migration file at:
   `src/main/resources/db/changelog/db.changelog-<version>-<short-description>.yaml`

   Use this template:
   ```yaml
   databaseChangeLog:
     - changeSet:
         id: <version>-<short-description>
         author: <git user name>
         changes:
           # TODO: add changeSet operations here
   ```

4. Register the new file in `db.changelog-master.yaml` under `databaseChangeLog` as an include:
   ```yaml
   - include:
       file: db/changelog/db.changelog-<version>-<short-description>.yaml
       relativeToChangelogFile: false
   ```

5. Show the user what was created and remind them to fill in the actual `changes` block with the appropriate Liquibase operations.

## Naming convention
- Version: sequential integer (1.0, 1.1, 2.0, etc.) matching the feature scope
- Description: kebab-case, describes the schema change (e.g., `create-quiz-table`, `add-score-column`)
