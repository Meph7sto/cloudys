package com.cloudys.requirement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudys.common.core.exception.ErrorResponse;
import com.cloudys.requirement.client.AuthServiceClient;
import com.cloudys.requirement.client.BaselineServiceClient;
import com.cloudys.requirement.client.ProjectServiceClient;
import com.cloudys.requirement.client.RequirementAnalysisClient;
import com.cloudys.requirement.dto.AnalysisImportResponse;
import com.cloudys.requirement.dto.CreateRequirementRequest;
import com.cloudys.requirement.dto.ImportRequirementsRequest;
import com.cloudys.requirement.entity.ManageRequirement;
import com.cloudys.requirement.repository.DefectRepository;
import com.cloudys.requirement.repository.ManageRequirementRepository;
import com.cloudys.requirement.repository.RequirementTestLinkRepository;

@ExtendWith(MockitoExtension.class)
class RequirementServiceTest {

    @Mock
    private ManageRequirementRepository requirementRepository;
    @Mock
    private DefectRepository defectRepository;
    @Mock
    private RequirementTestLinkRepository requirementTestLinkRepository;
    @Mock
    private AuthServiceClient authServiceClient;
    @Mock
    private ProjectServiceClient projectServiceClient;
    @Mock
    private BaselineServiceClient baselineServiceClient;
    @Mock
    private RequirementAnalysisClient requirementAnalysisClient;

    @InjectMocks
    private RequirementService requirementService;

    @BeforeEach
    void setUp() {
        when(projectServiceClient.getProject("proj-1")).thenReturn(Map.of("project_id", "proj-1"));
        when(authServiceClient.getPermissionContext("proj-1")).thenReturn(Map.of(
                "can_access", true,
                "can_edit", true,
                "permissions", List.of("view_requirement", "edit_requirement", "delete_requirement")
        ));
    }

    @Test
    void createRequirementMapsSourceLevelToRequirementType() {
        CreateRequirementRequest request = new CreateRequirementRequest(
                null,
                "Top requirement",
                "desc",
                null,
                null,
                null,
                "high",
                "u-1",
                List.of("a"),
                null,
                "source-1",
                "L3",
                Map.of("x", "y")
        );
        when(requirementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(requirementTestLinkRepository.findByRequirementIdOrderByCreatedAtDesc(any())).thenReturn(List.of());

        Map<String, Object> created = requirementService.createRequirement("proj-1", request);

        assertThat(created.get("requirement_type")).isEqualTo("top_level");
        assertThat(created.get("source_level")).isEqualTo("L3");
        verify(requirementRepository).save(any(ManageRequirement.class));
    }

    @Test
    void importFromSessionBuildsHierarchyAndStats() {
        AnalysisImportResponse export = new AnalysisImportResponse(
                List.of(new AnalysisImportResponse.AnalysisRequirementRecord("top-1", "L2", "Top text")),
                List.of(new AnalysisImportResponse.AnalysisLowLevelRequirementRecord(
                        "low-1", "top-1", "Top text", "Low text", "api", List.of("ac1"), "manual", Map.of("k", "v"))),
                List.of(new AnalysisImportResponse.AnalysisLowLevelLinkRecord("low-1", "top-1"))
        );
        when(requirementAnalysisClient.exportSessionRequirements("session-1")).thenReturn(export);
        when(requirementRepository.findByProjectIdAndDeletedFalse("proj-1")).thenReturn(List.of());
        when(requirementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> result = requirementService.importFromSession("proj-1", new ImportRequirementsRequest("session-1", "tree"));

        assertThat(result.get("inserted")).isEqualTo(2);
        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) result.get("stats");
        assertThat(stats.get("top_level_total")).isEqualTo(1);
        assertThat(stats.get("low_level_total")).isEqualTo(1);

        ArgumentCaptor<ManageRequirement> captor = ArgumentCaptor.forClass(ManageRequirement.class);
        verify(requirementRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<ManageRequirement> saved = captor.getAllValues();
        assertThat(saved.get(0).getRequirementType()).isEqualTo("top_level");
        assertThat(saved.get(1).getRequirementType()).isEqualTo("low_level");
        assertThat(saved.get(1).getParentId()).isEqualTo(saved.get(0).getReqId());
    }

    @Test
    void setBaselineRejectsBaselineFromAnotherProject() {
        ManageRequirement requirement = new ManageRequirement();
        requirement.setReqId("req-1");
        requirement.setProjectId("proj-1");
        requirement.setRequirementType("top_level");
        requirement.setDeleted(false);
        when(requirementRepository.findByReqIdAndDeletedFalse("req-1")).thenReturn(Optional.of(requirement));
        when(baselineServiceClient.getBaseline(1L)).thenReturn(Map.of("id", 1L, "project_id", "proj-2"));

        assertThatThrownBy(() -> requirementService.setBaseline("req-1", 1L))
                .isInstanceOf(ErrorResponse.class)
                .hasMessageContaining("基线不属于当前项目");
    }
}
