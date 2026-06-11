package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.domain.Equipement;
import com.example.demo.domain.RoleType;
import com.example.demo.domain.StatutEquipement;
import com.example.demo.domain.Utilisateur;
import com.example.demo.repository.EmployeRepository;
import com.example.demo.repository.EquipementRepository;
import com.example.demo.repository.UtilisateurRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class DemoApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private EquipementRepository equipementRepository;

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void contextLoads() {
    }

    @Test
    void loginReturnsJwtForValidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"admin\",\"motDePasse\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.login").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"admin\",\"motDePasse\":\"wrong\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Login ou mot de passe invalide"));
    }

    @Test
    void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboardAcceptsAdminJwt() throws Exception {
        String token = login("admin", "admin");

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEquipements").exists());
    }

    @Test
    void stockAcceptsAdminAndTechnicien() throws Exception {
        mockMvc.perform(get("/api/stock/pieces")
                        .header("Authorization", "Bearer " + login("admin", "admin")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/stock/pieces")
                        .header("Authorization", "Bearer " + login("tech", "tech")))
                .andExpect(status().isOk());
    }

    @Test
    void stockRejectsEmploye() throws Exception {
        mockMvc.perform(get("/api/stock/pieces")
                        .header("Authorization", "Bearer " + login("employe", "employe")))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginBlocksAccountAfterFiveInvalidAttempts() throws Exception {
        String login = "blocked_" + UUID.randomUUID();
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setLogin(login);
        utilisateur.setEmail(login + "@example.com");
        utilisateur.setRole(RoleType.EMPLOYE);
        utilisateur.setMotDePasse(passwordEncoder.encode("secret"));
        utilisateurRepository.save(utilisateur);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"login\":\"" + login + "\",\"motDePasse\":\"wrong\"}"))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + login + "\",\"motDePasse\":\"secret\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pretFollowsDemandValidationAndReturnWorkflow() throws Exception {
        String adminToken = login("admin", "admin");
        Equipement equipement = new Equipement();
        equipement.setNumSerie("TEST-" + UUID.randomUUID());
        equipement.setType("PC portable");
        equipement.setStatut(StatutEquipement.DISPONIBLE);
        equipement = equipementRepository.save(equipement);
        Long employeId = employeRepository.findAll().get(0).getId();

        MvcResult createResult = mockMvc.perform(post("/api/prets")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipementId\":" + equipement.getId()
                                + ",\"employeId\":" + employeId
                                + ",\"dateRetourPrevue\":\"2026-12-31\",\"motif\":\"Test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
                .andReturn();
        Long pretId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/prets/" + pretId + "/valider")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("VALIDE"));

        mockMvc.perform(put("/api/prets/" + pretId + "/cloturer")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CLOTURE"));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + username + "\",\"motDePasse\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }
}
