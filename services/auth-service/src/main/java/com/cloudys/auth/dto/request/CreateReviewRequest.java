package com.cloudys.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateReviewRequest(
        @NotBlank(message = "评审人 ID 不能为空") String reviewerId,
        Integer seq,
        String comment
) {}
