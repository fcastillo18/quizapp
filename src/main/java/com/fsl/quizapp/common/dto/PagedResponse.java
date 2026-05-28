package com.fsl.quizapp.common.dto;

import java.util.List;
import lombok.Builder;

/** Generic paginated response envelope. */
@Builder
public record PagedResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages) {
}
