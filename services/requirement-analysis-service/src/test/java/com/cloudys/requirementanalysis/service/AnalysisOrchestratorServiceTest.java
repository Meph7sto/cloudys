package com.cloudys.requirementanalysis.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.cloudys.requirementanalysis.client.InferenceServiceClient;
import com.cloudys.requirementanalysis.client.RequirementServiceClient;
import com.cloudys.requirementanalysis.entity.RequirementL123;
import com.cloudys.requirementanalysis.entity.RequirementsAnalysisRun;
import com.cloudys.requirementanalysis.repository.ClassificationAnalysisRepository;
import com.cloudys.requirementanalysis.repository.ConflictAnalysisRepository;
import com.cloudys.requirementanalysis.repository.LowLevelRequirementLinkRepository;
import com.cloudys.requirementanalysis.repository.LowLevelRequirementRepository;
import com.cloudys.requirementanalysis.repository.RequirementGraphRelationRepository;
import com.cloudys.requirementanalysis.repository.RequirementL123Repository;
import com.cloudys.requirementanalysis.repository.RequirementsAnalysisRunRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AnalysisOrchestratorServiceTest {

    @Mock
    private InferenceServiceClient inferenceServiceClient;
    @Mock
    private RequirementServiceClient requirementServiceClient;
    @Mock
    private ProgressEmitterService progressEmitter;
    private JsonSupport jsonSupport;
    @Mock
    private RequirementsAnalysisRunRepository runRepository;
    @Mock
    private RequirementL123Repository l123Repository;
    @Mock
    private LowLevelRequirementRepository lowLevelRepository;
    @Mock
    private LowLevelRequirementLinkRepository linkRepository;
    @Mock
    private ClassificationAnalysisRepository classificationRepository;
    @Mock
    private ConflictAnalysisRepository conflictRepository;
    @Mock
    private RequirementGraphRelationRepository graphRepository;

    @InjectMocks
    private AnalysisOrchestratorService orchestratorService;

    @BeforeEach
    void setUp() {
        jsonSupport = new JsonSupport(new ObjectMapper());
        orchestratorService = new AnalysisOrchestratorService(
                inferenceServiceClient,
                requirementServiceClient,
                progressEmitter,
                jsonSupport,
                runRepository,
                l123Repository,
                lowLevelRepository,
                linkRepository,
                classificationRepository,
                conflictRepository,
                graphRepository
        );
    }

    @Test
    void executeAnalysisFailsWhenRequirementImportFallbackRespondsWithError() {
        RequirementsAnalysisRun run = new RequirementsAnalysisRun();
        run.setAnalysisRunId("run-1");
        when(runRepository.findById("run-1")).thenReturn(Optional.of(run));
        when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(inferenceServiceClient.extractRequirements(any())).thenReturn(Map.of("requirements", List.of()));
        when(l123Repository.findBySessionIdOrderByCreatedAtAsc("session-1")).thenReturn(List.of());
        when(requirementServiceClient.importRequirements(eq("proj-1"), any()))
                .thenReturn(Map.of("error", "requirement_service_unavailable", "detail", "需求管理服务不可用，无法导入需求"));

        orchestratorService.executeAnalysis("run-1", "session-1", "proj-1", "ctx-1", true, "tree");

        verify(progressEmitter, never()).sendComplete(eq("run-1"), any());
        verify(progressEmitter).sendError(eq("run-1"), org.mockito.ArgumentMatchers.contains("需求管理服务不可用"));
    }

    @Test
    void executeAnalysisSkipsImportWhenAutoImportDisabled() {
        RequirementsAnalysisRun run = new RequirementsAnalysisRun();
        run.setAnalysisRunId("run-2");
        when(runRepository.findById("run-2")).thenReturn(Optional.of(run));
        when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(inferenceServiceClient.extractRequirements(any())).thenReturn(Map.of("requirements", List.of()));
        when(l123Repository.findBySessionIdOrderByCreatedAtAsc("session-1")).thenReturn(List.of());

        orchestratorService.executeAnalysis("run-2", "session-1", "proj-1", "ctx-1", false, "tree");

        verify(requirementServiceClient, never()).importRequirements(eq("proj-1"), any());
        verify(progressEmitter).sendComplete(eq("run-2"), any());
    }
}
