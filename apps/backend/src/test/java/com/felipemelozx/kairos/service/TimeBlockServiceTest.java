package com.felipemelozx.kairos.service;

import com.felipemelozx.kairos.common.AppError;
import com.felipemelozx.kairos.common.Result;
import com.felipemelozx.kairos.dto.request.CreateTimeBlockRequest;
import com.felipemelozx.kairos.dto.request.UpdateTimeBlockRequest;
import com.felipemelozx.kairos.dto.response.TimeBlockResponse;
import com.felipemelozx.kairos.entity.Project;
import com.felipemelozx.kairos.entity.TimeBlock;
import com.felipemelozx.kairos.entity.enums.ProjectStatus;
import com.felipemelozx.kairos.repository.ProjectRepository;
import com.felipemelozx.kairos.repository.TimeBlockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeBlockServiceTest {

    @Mock
    private TimeBlockRepository timeBlockRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private TimeBlockService timeBlockService;

    @Test
    void shouldCreateBlockForCurrentUser() {
        UUID userId = UUID.randomUUID();
        CreateTimeBlockRequest request = new CreateTimeBlockRequest("Deep Work",
                Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T10:00:00Z"), null, null);
        when(timeBlockRepository.save(any(TimeBlock.class))).thenAnswer(invocation -> {
            TimeBlock block = invocation.getArgument(0);
            block.setId(UUID.randomUUID());
            return block;
        });

        TimeBlockResponse result = assertOk(timeBlockService.create(userId, request));

        assertThat(result.title()).isEqualTo("Deep Work");
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.isOverride()).isFalse();
        verify(timeBlockRepository).save(any(TimeBlock.class));
    }

    @Test
    void shouldCreateBlockWithOwnedProject() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = ownedProject(projectId, userId);
        when(projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId))
                .thenReturn(Optional.of(project));
        when(timeBlockRepository.save(any(TimeBlock.class))).thenAnswer(invocation -> {
            TimeBlock block = invocation.getArgument(0);
            block.setId(UUID.randomUUID());
            return block;
        });
        CreateTimeBlockRequest request = new CreateTimeBlockRequest("Study",
                Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T10:00:00Z"), projectId, null);

        TimeBlockResponse result = assertOk(timeBlockService.create(userId, request));

        assertThat(result.projectId()).isEqualTo(projectId);
        verify(timeBlockRepository).save(any(TimeBlock.class));
    }

    @Test
    void shouldRejectBlankTitle() {
        UUID userId = UUID.randomUUID();
        CreateTimeBlockRequest request = new CreateTimeBlockRequest("   ",
                Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T10:00:00Z"), null, null);

        AppError error = assertErr(timeBlockService.create(userId, request));

        assertThat(error.code()).isEqualTo("INVALID_TIME_BLOCK_TITLE");
        verify(timeBlockRepository, never()).save(any(TimeBlock.class));
    }

    @Test
    void shouldRejectEndBeforeStart() {
        UUID userId = UUID.randomUUID();
        CreateTimeBlockRequest request = new CreateTimeBlockRequest("Bad range",
                Instant.parse("2026-10-01T10:00:00Z"), Instant.parse("2026-10-01T09:00:00Z"), null, null);

        AppError error = assertErr(timeBlockService.create(userId, request));

        assertThat(error.code()).isEqualTo("INVALID_TIME_RANGE");
        verify(timeBlockRepository, never()).save(any(TimeBlock.class));
    }

    @Test
    void shouldRejectUnknownProject() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        when(projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userId))
                .thenReturn(Optional.empty());
        CreateTimeBlockRequest request = new CreateTimeBlockRequest("Study",
                Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T10:00:00Z"), projectId, null);

        AppError error = assertErr(timeBlockService.create(userId, request));

        assertThat(error.code()).isEqualTo("NOT_FOUND");
        verify(timeBlockRepository, never()).save(any(TimeBlock.class));
    }

    @Test
    void shouldRejectForeignProject() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        when(projectRepository.findByIdAndUserIdAndDeletedAtIsNull(projectId, userB))
                .thenReturn(Optional.empty());
        CreateTimeBlockRequest request = new CreateTimeBlockRequest("Study",
                Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T10:00:00Z"), projectId, null);

        AppError error = assertErr(timeBlockService.create(userB, request));

        assertThat(error.code()).isEqualTo("NOT_FOUND");
        verify(timeBlockRepository, never()).save(any(TimeBlock.class));
    }

    @Test
    void shouldAllowOverlappingBlocks() {
        UUID userId = UUID.randomUUID();
        when(timeBlockRepository.save(any(TimeBlock.class))).thenAnswer(invocation -> {
            TimeBlock block = invocation.getArgument(0);
            block.setId(UUID.randomUUID());
            return block;
        });
        CreateTimeBlockRequest first = new CreateTimeBlockRequest("First",
                Instant.parse("2026-10-01T09:00:00Z"), Instant.parse("2026-10-01T10:00:00Z"), null, null);
        CreateTimeBlockRequest overlap = new CreateTimeBlockRequest("Overlap",
                Instant.parse("2026-10-01T09:30:00Z"), Instant.parse("2026-10-01T10:30:00Z"), null, null);

        assertOk(timeBlockService.create(userId, first));
        TimeBlockResponse result = assertOk(timeBlockService.create(userId, overlap));

        assertThat(result.title()).isEqualTo("Overlap");
    }

    @Test
    void shouldListOnlyCurrentUserBlocks() {
        UUID userId = UUID.randomUUID();
        TimeBlock block = storedBlock(userId, "Deep Work");
        when(timeBlockRepository.findByUserIdAndDeletedAtIsNullOrderByStartDateTimeAsc(userId))
                .thenReturn(List.of(block));

        List<TimeBlockResponse> result = timeBlockService.listByUser(userId, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Deep Work");
        verify(timeBlockRepository).findByUserIdAndDeletedAtIsNullOrderByStartDateTimeAsc(userId);
    }

    @Test
    void shouldPartiallyUpdateBlock() {
        UUID userId = UUID.randomUUID();
        TimeBlock block = storedBlock(userId, "Original");
        when(timeBlockRepository.findByIdAndUserIdAndDeletedAtIsNull(block.getId(), userId))
                .thenReturn(Optional.of(block));
        when(timeBlockRepository.save(any(TimeBlock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TimeBlockResponse result = assertOk(timeBlockService.update(block.getId(), userId,
                new UpdateTimeBlockRequest("Renamed", null, null, null)));

        assertThat(result.title()).isEqualTo("Renamed");
        assertThat(result.startDateTime()).isEqualTo(block.getStartDateTime());
        verify(timeBlockRepository).save(any(TimeBlock.class));
    }

    @Test
    void shouldSoftDeleteOwnBlock() {
        UUID userId = UUID.randomUUID();
        TimeBlock block = storedBlock(userId, "To Delete");
        when(timeBlockRepository.findByIdAndUserIdAndDeletedAtIsNull(block.getId(), userId))
                .thenReturn(Optional.of(block));

        Result<Void> result = timeBlockService.softDelete(block.getId(), userId);

        assertThat(result.isOk()).isTrue();
        verify(timeBlockRepository).softDeleteByIdAndUserId(any(UUID.class), any(UUID.class), any(Instant.class));
    }

    @Test
    void shouldThrowNotFoundWhenDeletingOtherUsersBlock() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        UUID blockId = UUID.randomUUID();
        when(timeBlockRepository.findByIdAndUserIdAndDeletedAtIsNull(blockId, userB))
                .thenReturn(Optional.empty());

        AppError error = assertErr(timeBlockService.softDelete(blockId, userB));

        assertThat(error.code()).isEqualTo("NOT_FOUND");
        verify(timeBlockRepository, never()).softDeleteByIdAndUserId(any(UUID.class), any(UUID.class), any(Instant.class));
    }

    private TimeBlock storedBlock(UUID userId, String title) {
        TimeBlock block = new TimeBlock();
        block.setId(UUID.randomUUID());
        block.setUserId(userId);
        block.setTitle(title);
        block.setStartDateTime(Instant.parse("2026-10-01T09:00:00Z"));
        block.setEndDateTime(Instant.parse("2026-10-01T10:00:00Z"));
        block.setOverride(false);
        block.setCreatedAt(Instant.now());
        return block;
    }

    private Project ownedProject(UUID projectId, UUID userId) {
        Project project = new Project();
        project.setId(projectId);
        project.setUserId(userId);
        project.setName("Study English");
        project.setColor("#1A2B3C");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedAt(Instant.now());
        return project;
    }

    private static <T> T assertOk(Result<T> result) {
        assertThat(result.isOk()).isTrue();
        return result.fold(value -> value, err -> {
            throw new AssertionError("Expected Ok but was Err: " + err.code());
        });
    }

    private static <T> AppError assertErr(Result<T> result) {
        assertThat(result.isErr()).isTrue();
        return result.fold(value -> {
            throw new AssertionError("Expected Err but was Ok");
        }, err -> err);
    }
}
