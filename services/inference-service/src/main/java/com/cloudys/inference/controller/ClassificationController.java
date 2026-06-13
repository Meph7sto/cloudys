package com.cloudys.inference.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.cloudys.inference.dto.classification.ClassificationRequest;
import com.cloudys.inference.service.InferenceOrchestratorService;

import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping({"/api/v2/inference/classification", "/inference/classification"})
public class ClassificationController {

    private final InferenceOrchestratorService orchestrator;

    public ClassificationController(InferenceOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    /** 文本分类 */
    @PostMapping("/predict-texts")
    public Mono<ResponseEntity<Map<String, Object>>> predictTexts(@Valid @RequestBody ClassificationRequest request) {
        return orchestrator.classifyTexts(request)
                .map(ResponseEntity::ok);
    }

    /** CSV 文件分类（multipart） */
    @PostMapping(value = "/predict-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<byte[]>> predictCsv(@RequestPart("file") FilePart file) {
        return orchestrator.classifyCsv(file)
                .map(data -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                        .body(data));
    }
}
