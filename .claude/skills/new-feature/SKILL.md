---
name: new-feature
description: Scaffold a new backend feature package following the project's standard layout (entity, repository, service, controller, dto subpackage). Use when adding a new domain concept.
disable-model-invocation: false
---

Ask the user for the feature name (e.g. "Invoice"). Then create under `backend/src/main/java/com/yourapp/{feature}/`:
- `{Feature}.java` — JPA entity with `@Entity`, UUID id, user FK, and audit timestamps
- `{Feature}Repository.java` — extends JpaRepository, includes `findByIdAndUserId`
- `{Feature}Service.java` — `@Service @Transactional`, first param of every method is `UUID userId`
- `{Feature}Controller.java` — `@RestController @RequestMapping("/api/{features}")` with CRUD stubs
- `dto/{Feature}Request.java` and `dto/{Feature}Response.java`

Follow existing patterns from the `transaction/` package. Do not add Flyway migrations — remind the user to run /create-migration separately.
