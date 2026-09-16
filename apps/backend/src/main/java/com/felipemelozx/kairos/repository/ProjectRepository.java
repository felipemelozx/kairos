package com.felipemelozx.kairos.repository;

import com.felipemelozx.kairos.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByUserIdAndDeletedAtIsNull(UUID userId);

    Optional<Project> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    @Modifying
    @Query("UPDATE Project p SET p.deletedAt = :deletedAt " +
           "WHERE p.id = :id AND p.userId = :userId AND p.deletedAt IS NULL")
    void softDeleteByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId, @Param("deletedAt") Instant deletedAt);
}
