package com.bloodbridge;

import com.bloodbridge.domain.*;
import com.bloodbridge.repo.InvitationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired InvitationRepository invitations;

    private String login(String email, String password) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private String validRequestBody() throws Exception {
        return mapper.writeValueAsString(java.util.Map.of(
                "bloodGroup", "O_POS", "component", "RED_CELLS", "unitsRequired", 1,
                "hospital", "Demo Hospital", "city", "Chandigarh",
                "latitude", 30.7333, "longitude", 76.7794,
                "urgency", "URGENT", "requiredBy", Instant.now().plus(6, ChronoUnit.HOURS).toString()));
    }

    @Test
    void rbacDonorCannotCreateRequestsAndRequesterCannotUseStaffEndpoints() throws Exception {
        String donor = login("donor01@demo.bloodbridge.local", "Demo@12345");
        mvc.perform(post("/api/requests").header("Authorization", "Bearer " + donor)
                .contentType(MediaType.APPLICATION_JSON).content(validRequestBody()))
                .andExpect(status().isForbidden());
        String requester = login("requester@demo.bloodbridge.local", "Demo@12345");
        mvc.perform(get("/api/admin/queue").header("Authorization", "Bearer " + requester))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/donor/invitations").header("Authorization", "Bearer " + requester))
                .andExpect(status().isForbidden());
    }

    @Test
    void rbacCoordinatorCanReviewButNotAdminister() throws Exception {
        String t = login("coordinator@demo.bloodbridge.local", "Demo@12345");
        String h = "Bearer " + t;
        mvc.perform(get("/api/admin/analytics").header("Authorization", h)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/queue").header("Authorization", h)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/users").header("Authorization", h)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/audit").header("Authorization", h)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/permissions").header("Authorization", h)).andExpect(status().isForbidden());
        mvc.perform(post("/api/requests").header("Authorization", h)
                .contentType(MediaType.APPLICATION_JSON).content(validRequestBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanEditPermissionMatrixAndChangeTakesEffect() throws Exception {
        String admin = "Bearer " + login("admin@bloodbridge.local", "Admin@12345");
        String coordinator = "Bearer " + login("coordinator@demo.bloodbridge.local", "Demo@12345");
        String reduced = mapper.writeValueAsString(java.util.Map.of("permissions",
                List.of("REQUEST_VIEW_ALL", "ANALYTICS_VIEW")));
        String defaults = mapper.writeValueAsString(java.util.Map.of("permissions",
                List.of("REQUEST_VIEW_ALL", "QUEUE_VIEW", "URGENCY_REVIEW", "DONOR_VERIFY", "ANALYTICS_VIEW",
                        "DELIVERY_RETRY", "INVENTORY_VIEW", "INVENTORY_MANAGE", "DONATION_RECORD",
                        "DRIVE_MANAGE", "REPORT_EXPORT", "USER_VIEW")));
        try {
            mvc.perform(put("/api/admin/permissions/MODERATOR").header("Authorization", admin)
                    .contentType(MediaType.APPLICATION_JSON).content(reduced)).andExpect(status().isOk());
            mvc.perform(get("/api/admin/queue").header("Authorization", coordinator)).andExpect(status().isForbidden());
            mvc.perform(get("/api/admin/analytics").header("Authorization", coordinator)).andExpect(status().isOk());
            mvc.perform(put("/api/admin/permissions/ADMIN").header("Authorization", admin)
                    .contentType(MediaType.APPLICATION_JSON).content(reduced)).andExpect(status().isBadRequest());
        } finally {
            mvc.perform(put("/api/admin/permissions/MODERATOR").header("Authorization", admin)
                    .contentType(MediaType.APPLICATION_JSON).content(defaults)).andExpect(status().isOk());
        }
        mvc.perform(get("/api/admin/queue").header("Authorization", coordinator)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/audit").header("Authorization", admin)).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointsRejectAnonymousAndWrongRole() throws Exception {
        mvc.perform(get("/api/requests")).andExpect(status().isUnauthorized());
        String donorToken = login("donor01@demo.bloodbridge.local", "Demo@12345");
        mvc.perform(get("/api/admin/analytics").header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestIsMatchedAndDuplicateResponseIsRejected() throws Exception {
        String requesterToken = login("requester@demo.bloodbridge.local", "Demo@12345");
        String body = mapper.writeValueAsString(java.util.Map.of(
                "bloodGroup", "O_POS", "component", "RED_CELLS", "unitsRequired", 1,
                "hospital", "Demo Hospital", "city", "Chandigarh",
                "latitude", 30.7333, "longitude", 76.7794,
                "urgency", "EMERGENCY", "requiredBy", Instant.now().plus(6, ChronoUnit.HOURS).toString()));

        MvcResult created = mvc.perform(post("/api/requests").header("Authorization", "Bearer " + requesterToken)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn();
        JsonNode detail = mapper.readTree(created.getResponse().getContentAsString());

        assertEquals("MATCHING", detail.get("request").get("status").asText());
        assertTrue(detail.get("candidates").size() > 0, "expected ranked candidates");
        double first = detail.get("candidates").get(0).get("score").asDouble();
        double last = detail.get("candidates").get(detail.get("candidates").size() - 1).get("score").asDouble();
        assertTrue(first >= last, "candidates must be sorted by score");
        assertTrue(detail.get("candidates").get(0).get("reasons").size() > 0, "reasons must be present");

        long requestId = detail.get("request").get("id").asLong();
        List<Invitation> invs = invitations.findAll().stream()
                .filter(i -> i.getRequest().getId() == requestId).toList();
        Invitation inv = invs.get(0);
        String donorToken = login(inv.getDonor().getEmail(), "Demo@12345");
        String accept = mapper.writeValueAsString(java.util.Map.of("accept", true));

        mvc.perform(post("/api/invitations/" + inv.getId() + "/respond").header("Authorization", "Bearer " + donorToken)
                .contentType(MediaType.APPLICATION_JSON).content(accept)).andExpect(status().isOk());
        mvc.perform(post("/api/invitations/" + inv.getId() + "/respond").header("Authorization", "Bearer " + donorToken)
                .contentType(MediaType.APPLICATION_JSON).content(accept)).andExpect(status().isConflict());

        String adminToken = login("admin@bloodbridge.local", "Admin@12345");
        mvc.perform(get("/api/admin/analytics").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // A coordinator records the completed donation; the same invitation cannot be recorded twice.
        String coordinator = "Bearer " + login("coordinator@demo.bloodbridge.local", "Demo@12345");
        String donation = mapper.writeValueAsString(java.util.Map.of("invitationId", inv.getId(), "units", 1));
        mvc.perform(post("/api/donations").header("Authorization", coordinator)
                .contentType(MediaType.APPLICATION_JSON).content(donation)).andExpect(status().isOk());
        mvc.perform(post("/api/donations").header("Authorization", coordinator)
                .contentType(MediaType.APPLICATION_JSON).content(donation)).andExpect(status().isConflict());
        mvc.perform(post("/api/donations").header("Authorization", "Bearer " + donorToken)
                .contentType(MediaType.APPLICATION_JSON).content(donation)).andExpect(status().isForbidden());
    }

    @Test
    void stockDrivesAndReportsFollowPermissions() throws Exception {
        String coordinator = "Bearer " + login("coordinator@demo.bloodbridge.local", "Demo@12345");
        String donor = "Bearer " + login("donor02@demo.bloodbridge.local", "Demo@12345");
        String requester = "Bearer " + login("requester@demo.bloodbridge.local", "Demo@12345");

        MvcResult stock = mvc.perform(get("/api/inventory").header("Authorization", coordinator))
                .andExpect(status().isOk()).andReturn();
        JsonNode first = mapper.readTree(stock.getResponse().getContentAsString()).get(0);
        mvc.perform(get("/api/inventory").header("Authorization", requester)).andExpect(status().isForbidden());
        String tooMuch = mapper.writeValueAsString(java.util.Map.of("delta", -500, "reason", "test"));
        mvc.perform(patch("/api/inventory/" + first.get("id").asLong() + "/adjust").header("Authorization", coordinator)
                .contentType(MediaType.APPLICATION_JSON).content(tooMuch)).andExpect(status().isBadRequest());

        mvc.perform(get("/api/reports/requests.csv").header("Authorization", coordinator)).andExpect(status().isOk());
        mvc.perform(get("/api/reports/requests.csv").header("Authorization", donor)).andExpect(status().isForbidden());

        MvcResult drives = mvc.perform(get("/api/drives").header("Authorization", donor))
                .andExpect(status().isOk()).andReturn();
        long driveId = mapper.readTree(drives.getResponse().getContentAsString()).get(0).get("id").asLong();
        mvc.perform(post("/api/drives/" + driveId + "/register").header("Authorization", donor))
                .andExpect(status().isOk());
        mvc.perform(post("/api/drives/" + driveId + "/register").header("Authorization", donor))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/drives/" + driveId + "/register").header("Authorization", coordinator))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/donor/impact").header("Authorization", donor)).andExpect(status().isOk());
    }

    @Test
    void repeatedFailedLoginsAreLocked() throws Exception {
        String body = mapper.writeValueAsString(java.util.Map.of("email", "nobody@example.com", "password", "wrong-password"));
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests());
    }
}
