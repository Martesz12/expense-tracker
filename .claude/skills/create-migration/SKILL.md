---
name: create-migration
description: Scaffold the next Flyway migration file with the correct version number and filename convention. Use when adding a new DB migration.
disable-model-invocation: false
---

1. Run `ls backend/src/main/resources/db/migration/ | sort -V | tail -1` to find the highest existing version number.
2. Increment it by 1.
3. Create the file `backend/src/main/resources/db/migration/V{n}__{description}.sql` where `{description}` is snake_case of the user's intent.
4. Populate it with a commented header: `-- Migration: {description}` and an empty transaction block.
5. Tell the user the filename created and remind them to run `mvn flyway:migrate` to apply it.
