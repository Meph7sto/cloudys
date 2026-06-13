package com.cloudys.requirementanalysis.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudys.common.core.exception.ErrorResponse;
import com.cloudys.requirementanalysis.dto.CreateActorRequest;
import com.cloudys.requirementanalysis.dto.UpdateActorRequest;
import com.cloudys.requirementanalysis.entity.RequirementActor;
import com.cloudys.requirementanalysis.repository.RequirementActorRepository;

@Service
public class ActorService {

    private final RequirementActorRepository actorRepository;
    private final JsonSupport jsonSupport;

    public ActorService(RequirementActorRepository actorRepository, JsonSupport jsonSupport) {
        this.actorRepository = actorRepository;
        this.jsonSupport = jsonSupport;
    }

    @Transactional
    public Map<String, Object> createActor(CreateActorRequest request) {
        RequirementActor actor = new RequirementActor();
        actor.setActorId(UUID.randomUUID().toString());
        actor.setRequirementId(request.requirementId());
        actor.setActorType(request.actorType());
        actor.setActorName(request.actorName());
        actor.setStatus(request.status() != null ? request.status() : "idle");
        actor.setConfigJson(jsonSupport.toJson(request.config()));
        actor.setCreatedAt(Instant.now());
        actor.setUpdatedAt(Instant.now());
        actorRepository.save(actor);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("actor_id", actor.getActorId());
        result.put("created_at", actor.getCreatedAt());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listActors(String requirementId) {
        List<RequirementActor> actors = actorRepository.findByRequirementId(requirementId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requirement_id", requirementId);
        result.put("actors", actors.stream().map(this::toActorMap).toList());
        return result;
    }

    @Transactional
    public Map<String, Object> updateActor(String actorId, UpdateActorRequest request) {
        RequirementActor actor = actorRepository.findById(actorId)
                .orElseThrow(() -> new ErrorResponse("actor 不存在: " + actorId, 404));

        if (request.actorName() != null) {
            actor.setActorName(request.actorName());
        }
        if (request.actorType() != null) {
            actor.setActorType(request.actorType());
        }
        if (request.status() != null) {
            actor.setStatus(request.status());
        }
        if (request.config() != null) {
            actor.setConfigJson(jsonSupport.toJson(request.config()));
        }
        actor.setUpdatedAt(Instant.now());
        actorRepository.save(actor);

        return toActorMap(actor);
    }

    @Transactional
    public Map<String, Object> deleteActor(String actorId) {
        RequirementActor actor = actorRepository.findById(actorId)
                .orElseThrow(() -> new ErrorResponse("actor 不存在: " + actorId, 404));
        actorRepository.delete(actor);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("actor_id", actorId);
        result.put("deleted", true);
        return result;
    }

    private Map<String, Object> toActorMap(RequirementActor actor) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("actor_id", actor.getActorId());
        map.put("requirement_id", actor.getRequirementId());
        map.put("actor_type", actor.getActorType());
        map.put("actor_name", actor.getActorName());
        map.put("status", actor.getStatus());
        map.put("config", jsonSupport.toMap(actor.getConfigJson()));
        map.put("created_at", actor.getCreatedAt());
        map.put("updated_at", actor.getUpdatedAt());
        return map;
    }
}
