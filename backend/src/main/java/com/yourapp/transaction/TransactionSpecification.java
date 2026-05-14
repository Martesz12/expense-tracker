package com.yourapp.transaction;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

public class TransactionSpecification {

    public static Specification<Transaction> build(
            UUID userId,
            LocalDate from,
            LocalDate to,
            UUID accountId,
            UUID categoryId,
            String type,
            List<UUID> tagIds,
            String search
    ) {
        Specification<Transaction> spec = userEquals(userId);

        if (from != null) {
            OffsetDateTime fromDt = from.atStartOfDay().atOffset(ZoneOffset.UTC);
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("transactionDate"), fromDt));
        }
        if (to != null) {
            OffsetDateTime toDt = to.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("transactionDate"), toDt));
        }
        if (accountId != null) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.equal(root.get("fromAccount").get("id"), accountId),
                    cb.equal(root.get("toAccount").get("id"), accountId)
            ));
        }
        if (categoryId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category").get("id"), categoryId));
        }
        if (type != null && !type.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("type"), type));
        }
        if (tagIds != null && !tagIds.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                return root.join("tags", JoinType.INNER).get("id").in(tagIds);
            });
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("note")), pattern));
        }

        return spec;
    }

    private static Specification<Transaction> userEquals(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }
}
