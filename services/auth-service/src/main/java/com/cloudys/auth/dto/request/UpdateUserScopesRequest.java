package com.cloudys.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record UpdateUserScopesRequest(
        @JsonAlias("product_scopes")
        List<ScopeItem> productScopes,
        @JsonAlias("project_scopes")
        List<ScopeItem> projectScopes
) {
    public record ScopeItem(String id, @JsonAlias("can_edit") Boolean canEdit) {}
}
