package com.cloudys.requirementanalysis.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.cloudys.common.security.JwtTokenProvider;
import com.cloudys.requirementanalysis.entity.LowLevelRequirement;
import com.cloudys.requirementanalysis.entity.LowLevelRequirementLink;
import com.cloudys.requirementanalysis.entity.RequirementL123;
import com.cloudys.requirementanalysis.repository.LowLevelRequirementLinkRepository;
import com.cloudys.requirementanalysis.repository.LowLevelRequirementRepository;
import com.cloudys.requirementanalysis.repository.RequirementL123Repository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequirementExportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private RequirementL123Repository requirementL123Repository;
    @Autowired
    private LowLevelRequirementRepository lowLevelRequirementRepository;
    @Autowired
    private LowLevelRequirementLinkRepository lowLevelRequirementLinkRepository;

    private String token;

    @BeforeEach
    void setUp() {
        lowLevelRequirementLinkRepository.deleteAll();
        lowLevelRequirementRepository.deleteAll();
        requirementL123Repository.deleteAll();
        token = jwtTokenProvider.generateToken("u-admin", "admin", "super_admin", null);

        RequirementL123 top = new RequirementL123();
        top.setReqId("top-1");
        top.setSessionId("session-1");
        top.setLevel("L2");
        top.setText("Top requirement text");
        top.setFingerprint("fp-top-1");
        top.setCreatedAt(Instant.now());
        requirementL123Repository.save(top);

        LowLevelRequirement low = new LowLevelRequirement();
        low.setReqId("low-1");
        low.setSessionId("session-1");
        low.setSourceTopId("top-1");
        low.setSourceTopText("Top requirement text");
        low.setText("Low requirement text");
        low.setComponent("service");
        low.setAcceptanceCriteria("[\"ac1\"]");
        low.setTestMethod("manual");
        low.setMeta("{\"owner\":\"qa\"}");
        low.setCreatedAt(Instant.now());
        lowLevelRequirementRepository.save(low);

        LowLevelRequirementLink link = new LowLevelRequirementLink();
        link.setReqId("low-1");
        link.setSessionId("session-1");
        link.setTopReqId("top-1");
        link.setCreatedAt(Instant.now());
        lowLevelRequirementLinkRepository.save(link);
    }

    @Test
    @DisplayName("export endpoint returns session requirements")
    void exportEndpointReturnsRequirements() throws Exception {
        mockMvc.perform(get("/api/v2/analysis/sessions/session-1/requirements-export")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session_id", is("session-1")))
                .andExpect(jsonPath("$.requirements_l123", hasSize(1)))
                .andExpect(jsonPath("$.low_level_requirements", hasSize(1)))
                .andExpect(jsonPath("$.low_level_requirement_links", hasSize(1)))
                .andExpect(jsonPath("$.low_level_requirements[0].acceptance_criteria[0]", is("ac1")));
    }
}
