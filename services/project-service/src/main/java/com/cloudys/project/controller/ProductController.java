package com.cloudys.project.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cloudys.project.dto.BindProjectRequest;
import com.cloudys.project.dto.CreateProductRequest;
import com.cloudys.project.dto.CreateProjectRequest;
import com.cloudys.project.dto.UpdateProductRequest;
import com.cloudys.project.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v2/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/products")
    public ResponseEntity<Map<String, Object>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.ok(productService.create(request));
    }

    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> listProducts(
            @RequestParam(name = "include_archived", defaultValue = "false") boolean includeArchived) {
        return ResponseEntity.ok(productService.list(includeArchived));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable String productId) {
        return ResponseEntity.ok(productService.get(productId));
    }

    @PatchMapping("/products/{productId}")
    public ResponseEntity<Map<String, Object>> updateProduct(@PathVariable String productId,
                                                              @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.update(productId, request));
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable String productId) {
        return ResponseEntity.ok(productService.archive(productId));
    }

    @GetMapping("/products/{productId}/projects")
    public ResponseEntity<Map<String, Object>> listProjectsByProduct(@PathVariable String productId) {
        return ResponseEntity.ok(productService.listProjects(productId));
    }

    @GetMapping("/products/{productId}/overview")
    public ResponseEntity<Map<String, Object>> getProductOverview(@PathVariable String productId) {
        return ResponseEntity.ok(productService.getOverview(productId));
    }

    @GetMapping("/products/{productId}/requirements")
    public ResponseEntity<Map<String, Object>> listRequirementsByProduct(
            @PathVariable String productId,
            @RequestParam(name = "include_deleted", defaultValue = "false") boolean includeDeleted) {
        return ResponseEntity.ok(productService.listRequirements(productId, includeDeleted));
    }

    @GetMapping("/products/{productId}/milestones")
    public ResponseEntity<Map<String, Object>> listMilestonesByProduct(@PathVariable String productId) {
        return ResponseEntity.ok(productService.listMilestones(productId));
    }

    @GetMapping("/products/{productId}/baselines")
    public ResponseEntity<Map<String, Object>> listBaselinesByProduct(@PathVariable String productId) {
        return ResponseEntity.ok(productService.listBaselines(productId));
    }

    @PostMapping("/products/{productId}/projects")
    public ResponseEntity<Map<String, Object>> createProjectUnderProduct(
            @PathVariable String productId,
            @Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.ok(productService.createProjectUnderProduct(productId, request));
    }

    @PostMapping("/projects/{projectId}/bind")
    public ResponseEntity<Map<String, Object>> bindProjectToProduct(@PathVariable String projectId,
                                                                     @RequestBody BindProjectRequest request) {
        return ResponseEntity.ok(productService.bindProject(projectId, request));
    }
}
