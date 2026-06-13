package com.cloudys.project.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record BindProjectRequest(
        @JsonAlias("product_id") String productId
) {}
