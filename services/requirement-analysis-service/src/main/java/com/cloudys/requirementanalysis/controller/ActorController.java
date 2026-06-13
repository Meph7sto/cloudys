package com.cloudys.requirementanalysis.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudys.requirementanalysis.dto.CreateActorRequest;
import com.cloudys.requirementanalysis.dto.UpdateActorRequest;
import com.cloudys.requirementanalysis.service.ActorService;

@RestController
@RequestMapping("/api/v2/actor")
public class ActorController {

    private final ActorService actorService;

    public ActorController(ActorService actorService) {
        this.actorService = actorService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody CreateActorRequest request) {
        return ResponseEntity.ok(actorService.createActor(request));
    }

    @GetMapping("/requirement/{requirementId}")
    public ResponseEntity<Map<String, Object>> listByRequirement(@PathVariable String requirementId) {
        return ResponseEntity.ok(actorService.listActors(requirementId));
    }

    @PatchMapping("/{actorId}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable String actorId,
                                                       @RequestBody UpdateActorRequest request) {
        return ResponseEntity.ok(actorService.updateActor(actorId, request));
    }

    @DeleteMapping("/{actorId}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String actorId) {
        return ResponseEntity.ok(actorService.deleteActor(actorId));
    }
}
