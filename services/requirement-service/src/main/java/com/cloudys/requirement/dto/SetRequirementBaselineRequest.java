package com.cloudys.requirement.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.PositiveOrZero;

public record SetRequirementBaselineRequest(
        @JsonAlias("baseline_id") @PositiveOrZero Long baselineId
) {}
