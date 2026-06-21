package com.cloudys.auth.service;

import com.cloudys.auth.dto.request.UpdateUserScopesRequest;
import com.cloudys.auth.entity.UserProductScope;
import com.cloudys.auth.entity.UserProjectScope;
import com.cloudys.auth.repository.*;
import com.cloudys.common.core.exception.ErrorResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class ScopeService {

    private final UserProductScopeRepository productScopeRepo;
    private final UserProjectScopeRepository projectScopeRepo;
    private final ManageProductRepository productRepo;
    private final ManageProjectRepository projectRepo;

    public ScopeService(UserProductScopeRepository productScopeRepo,
                        UserProjectScopeRepository projectScopeRepo,
                        ManageProductRepository productRepo,
                        ManageProjectRepository projectRepo) {
        this.productScopeRepo = productScopeRepo;
        this.projectScopeRepo = projectScopeRepo;
        this.productRepo = productRepo;
        this.projectRepo = projectRepo;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getScopeOptions() {
        List<Map<String, Object>> products = productRepo.findByStatus("active").stream()
                .map(p -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("product_id", p.getProductId());
                    map.put("name", p.getProductName());
                    map.put("product_name", p.getProductName());
                    return map;
                })
                .toList();

        List<Map<String, Object>> projects = projectRepo.findByStatus("active").stream()
                .map(p -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("project_id", p.getProjectId());
                    map.put("name", p.getProjectName());
                    map.put("project_name", p.getProjectName());
                    map.put("product_id", p.getProductId());
                    return map;
                })
                .toList();

        return Map.of("products", products, "projects", projects);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserScopes(String userId) {
        List<UserProductScope> productScopes = productScopeRepo.findByUserId(userId);
        List<UserProjectScope> projectScopes = projectScopeRepo.findByUserId(userId);

        return Map.of(
                "product_scopes", productScopes.stream().map(s -> Map.of(
                        "product_id", s.getProductId(), "can_edit", s.getCanEdit())).toList(),
                "project_scopes", projectScopes.stream().map(s -> Map.of(
                        "project_id", s.getProjectId(), "can_edit", s.getCanEdit())).toList()
        );
    }

    @Transactional
    public Map<String, Object> replaceUserScopes(String userId, UpdateUserScopesRequest request,
                                                  String grantedBy) {
        // Delete existing scopes
        productScopeRepo.deleteByUserId(userId);
        projectScopeRepo.deleteByUserId(userId);

        Instant now = Instant.now();
        List<Map<String, Object>> newProductScopes = new ArrayList<>();
        List<Map<String, Object>> newProjectScopes = new ArrayList<>();

        // Insert new product scopes
        if (request.productScopes() != null) {
            for (var item : request.productScopes()) {
                UserProductScope scope = new UserProductScope();
                scope.setUserId(userId);
                scope.setProductId(item.id());
                scope.setCanEdit(item.canEdit() != null ? item.canEdit() : false);
                scope.setGrantedBy(grantedBy);
                scope.setGrantedAt(now);
                scope.setUpdatedAt(now);
                productScopeRepo.save(scope);
                newProductScopes.add(Map.of("product_id", item.id(), "can_edit", scope.getCanEdit()));
            }
        }

        // Insert new project scopes
        if (request.projectScopes() != null) {
            for (var item : request.projectScopes()) {
                UserProjectScope scope = new UserProjectScope();
                scope.setUserId(userId);
                scope.setProjectId(item.id());
                scope.setCanEdit(item.canEdit() != null ? item.canEdit() : false);
                scope.setGrantedBy(grantedBy);
                scope.setGrantedAt(now);
                scope.setUpdatedAt(now);
                projectScopeRepo.save(scope);
                newProjectScopes.add(Map.of("project_id", item.id(), "can_edit", scope.getCanEdit()));
            }
        }

        return Map.of(
                "product_scopes", newProductScopes,
                "project_scopes", newProjectScopes
        );
    }
}
