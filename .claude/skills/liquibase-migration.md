---
name: liquibase-migration
description: Generate a Liquibase YAML migration for a given set of entities. Use when adding new tables, columns, or seed data to the database schema.
---

Generate a Liquibase YAML migration for a given set of entities.

---

## File Naming

```
db.changelog-<version>-<short-description>.yaml
```

Examples:
```
db.changelog-1.0-create-schema.yaml
db.changelog-1.1-seed-data.yaml
db.changelog-2.0-add-notifications-table.yaml
```

Every new file must be registered in `db.changelog-master.yaml`:

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/db.changelog-1.0-create-schema.yaml
  - include:
      file: db/changelog/db.changelog-1.1-seed-data.yaml
```

---

## Schema Migration Structure

```yaml
databaseChangeLog:
  - changeSet:
      id: 1.0-create-my-table
      author: <your-name>
      changes:
        - createTable:
            tableName: my_table
            columns:
              - column:
                  name: id
                  type: UUID
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: name
                  type: VARCHAR(255)
                  constraints:
                    nullable: false
              - column:
                  name: description
                  type: VARCHAR(2000)
              - column:
                  name: created_at
                  type: TIMESTAMP
                  defaultValueComputed: CURRENT_TIMESTAMP
```

---

## Column Type Rules

| Java type | Liquibase type | Notes |
|---|---|---|
| `UUID` (PK) | `UUID` | Works in H2 and PostgreSQL |
| `String` (short) | `VARCHAR(255)` | Safe in both |
| `String` (long text) | `VARCHAR(2000)` | Avoid `TEXT` if H2 tests needed |
| `boolean` | `BOOLEAN` | |
| `int` / `Integer` | `INT` | |
| `BigDecimal` | `DECIMAL(p,s)` | |
| `LocalDateTime` | `TIMESTAMP` | |

**Avoid `TEXT`** for columns in projects that use H2 for tests. H2 maps `TEXT` to `CLOB` (`Types#CLOB`), while Hibernate's schema validator expects `VARCHAR` for `String` fields. Use `VARCHAR(2000)` or set `ddl-auto=none` in the test profile.

**Never use `defaultValueComputed`** for UUID primary keys — let the application generate them via `GenerationType.UUID`.

---

## Foreign Keys

```yaml
- column:
    name: parent_id
    type: UUID
    constraints:
      nullable: false
      foreignKeyName: fk_child_parent
      references: parent_table(id)
```

---

## Seed Data Migration Structure

Use `insert` changeSets. Provide explicit UUIDs — predictable IDs make tests and `.http` files self-documenting.

```yaml
databaseChangeLog:
  - changeSet:
      id: 1.1-seed-my-entities
      author: <your-name>
      changes:
        - insert:
            tableName: my_table
            columns:
              - column:
                  name: id
                  value: aaaaaaaa-0000-0000-0000-000000000001
              - column:
                  name: name
                  value: "First Entity"
              - column:
                  name: active
                  valueBoolean: true
```

### UUID Convention for Seed Data

Use a readable prefix + sequential suffix:

```
Entity type  →  prefix
Primary       →  aaaaaaaa-0000-0000-0000-00000000000N
Secondary     →  bbbbbbbb-0000-0000-0000-00000000000N  (parent 1)
              →  bbbbbbbb-0000-0000-0000-00000000001N  (parent 2)
Tertiary      →  cccccccc-0000-0000-0000-NNNNNNNNNNNN  (sequential)
```

---

## Verification

After writing the migration:

1. Start the application: `./gradlew bootRun` — Liquibase must apply cleanly with zero errors.
2. Run tests: `./gradlew test` — H2 must initialize from Liquibase without "already exists" errors.
3. Check that each `changeSet` has a unique `id`. Duplicate IDs cause silent failures.
