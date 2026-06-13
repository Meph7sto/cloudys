package com.cloudys.requirement.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record BulkUpdateStatusRequest(
        @NotEmpty @JsonAlias("req_ids") List<String> reqIds,
        @NotBlank String status
) {}
