package jetpackcodesolution.infrastructure.persistence.specification;

import jakarta.persistence.criteria.Predicate;
import jetpackcodesolution.domain.model.BookingSearchCriteria;
import jetpackcodesolution.infrastructure.persistence.entity.BookingEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class BookingSpecifications {

    private BookingSpecifications() {
    }

    public static Specification<BookingEntity> matching(BookingSearchCriteria criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.roomId() != null) {
                predicates.add(builder.equal(root.get("room").get("id"), criteria.roomId()));
            }

            if (criteria.dayStart() != null && criteria.dayEnd() != null) {
                predicates.add(builder.lessThan(root.get("startTime"), criteria.dayEnd()));
                predicates.add(builder.greaterThan(root.get("endTime"), criteria.dayStart()));
            }

            if (!criteria.includeCancelled()) {
                predicates.add(builder.isNull(root.get("cancelledAt")));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
