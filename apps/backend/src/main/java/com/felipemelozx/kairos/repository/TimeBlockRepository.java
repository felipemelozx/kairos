package com.felipemelozx.kairos.repository;

import com.felipemelozx.kairos.entity.TimeBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeBlockRepository extends JpaRepository<TimeBlock, UUID> {

    List<TimeBlock> findByUserIdAndDeletedAtIsNullOrderByStartDateTimeAsc(UUID userId);

    List<TimeBlock> findByUserIdAndStartDateTimeGreaterThanEqualAndStartDateTimeLessThanAndDeletedAtIsNullOrderByStartDateTimeAsc(
            UUID userId, Instant from, Instant to);

    Optional<TimeBlock> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    List<TimeBlock> findBySeriesIdAndUserIdAndDeletedAtIsNullOrderByStartDateTimeAsc(UUID seriesId, UUID userId);

    @Modifying
    @Query("UPDATE TimeBlock t SET t.deletedAt = :deletedAt " +
           "WHERE t.id = :id AND t.userId = :userId AND t.deletedAt IS NULL")
    void softDeleteByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId, @Param("deletedAt") Instant deletedAt);

    @Modifying
    @Query("UPDATE TimeBlock t SET t.deletedAt = :deletedAt " +
           "WHERE t.seriesId = :seriesId AND t.userId = :userId AND t.deletedAt IS NULL AND t.startDateTime >= :from")
    void softDeleteFutureBySeriesId(@Param("seriesId") UUID seriesId, @Param("userId") UUID userId,
            @Param("from") Instant from, @Param("deletedAt") Instant deletedAt);
}
