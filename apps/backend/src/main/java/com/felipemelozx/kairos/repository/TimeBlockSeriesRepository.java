package com.felipemelozx.kairos.repository;

import com.felipemelozx.kairos.entity.TimeBlockSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeBlockSeriesRepository extends JpaRepository<TimeBlockSeries, UUID> {

    List<TimeBlockSeries> findByUserIdAndDeletedAtIsNullOrderByStartDateAsc(UUID userId);

    Optional<TimeBlockSeries> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    List<TimeBlockSeries> findByEndDateIsNullAndDeletedAtIsNull();

    @Query("select s from TimeBlockSeries s where s.deletedAt is null and (s.endDate is null or s.endDate >= :today)")
    List<TimeBlockSeries> findActiveForRollForward(@Param("today") LocalDate today);

    @Modifying
    @Query("UPDATE TimeBlockSeries s SET s.deletedAt = :deletedAt " +
           "WHERE s.id = :id AND s.userId = :userId AND s.deletedAt IS NULL")
    void softDeleteByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId, @Param("deletedAt") Instant deletedAt);
}
