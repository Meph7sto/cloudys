package com.cloudys.requirementanalysis.service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ProgressEmitterService {

    private static final Logger log = LoggerFactory.getLogger(ProgressEmitterService.class);
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String runId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.computeIfAbsent(runId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(runId, emitter));
        emitter.onTimeout(() -> removeEmitter(runId, emitter));
        emitter.onError(e -> removeEmitter(runId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("run_id", runId, "status", "CONNECTED")));
        } catch (IOException e) {
            removeEmitter(runId, emitter);
        }

        return emitter;
    }

    public void sendProgress(String runId, String stage, int percent, String message) {
        List<SseEmitter> runEmitters = emitters.get(runId);
        if (runEmitters == null) {
            return;
        }

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("run_id", runId);
        event.put("stage", stage);
        event.put("percent", percent);
        event.put("message", message);

        for (SseEmitter emitter : runEmitters) {
            try {
                emitter.send(SseEmitter.event().name("progress").data(event));
            } catch (IOException e) {
                removeEmitter(runId, emitter);
            }
        }
    }

    public void sendComplete(String runId, Map<String, Object> result) {
        List<SseEmitter> runEmitters = emitters.get(runId);
        if (runEmitters == null) {
            return;
        }

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("run_id", runId);
        event.put("status", "COMPLETED");
        event.put("result", result);

        for (SseEmitter emitter : runEmitters) {
            try {
                emitter.send(SseEmitter.event().name("complete").data(event));
                emitter.complete();
            } catch (IOException e) {
                removeEmitter(runId, emitter);
            }
        }
        emitters.remove(runId);
    }

    public void sendError(String runId, String error) {
        List<SseEmitter> runEmitters = emitters.get(runId);
        if (runEmitters == null) {
            return;
        }

        Map<String, Object> event = Map.of("run_id", runId, "status", "FAILED", "error", error);

        for (SseEmitter emitter : runEmitters) {
            try {
                emitter.send(SseEmitter.event().name("error").data(event));
                emitter.complete();
            } catch (IOException e) {
                // ignore
            }
        }
        emitters.remove(runId);
    }

    private void removeEmitter(String runId, SseEmitter emitter) {
        List<SseEmitter> runEmitters = emitters.get(runId);
        if (runEmitters != null) {
            runEmitters.remove(emitter);
            if (runEmitters.isEmpty()) {
                emitters.remove(runId);
            }
        }
    }
}
