package com.skypro.simplebanking.IntegrationTest;

import com.skypro.simplebanking.dto.BankingUserDetails;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import javax.transaction.Transactional;

import com.skypro.simplebanking.SimpleBankingApplication;
import com.skypro.simplebanking.dto.UserDTO;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import com.skypro.simplebanking.service.AccountService;
import com.skypro.simplebanking.service.UserService;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Base64Utils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import java.nio.charset.StandardCharsets;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



public class UserControllersTest extends IntegationTest{

    private String base64Encoded(String userName, String password) {
        return "Basic " + Base64Utils.encodeToString((userName + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void createUser_Ok() throws Exception {
        JSONObject createUserRequest = new JSONObject();
        createUserRequest.put("username", "username");
        createUserRequest.put("password", "password");
        mockMvc.perform(post("/user")
                        .header("X-SECURITY-ADMIN-KEY", "SUPER_SECRET_KEY_FROM_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserRequest.toString()))
                .andExpect(status().isOk());
    }
    @Test
    void createUser_WhenUserIsExistException() throws Exception {
        JSONObject createUserRequest = new JSONObject();
        createUserRequest.put("username", "Ilya");
        createUserRequest.put("password", "lolo");
        mockMvc.perform(post("/user")
                        .header("X-SECURITY-ADMIN-KEY", "SUPER_SECRET_KEY_FROM_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserRequest.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getListUsers_Test() throws Exception {
        mockMvc.perform(get("/user/list")
                        .header(HttpHeaders.AUTHORIZATION, base64Encoded("Ilya", "lolo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void getListUsers_WhenAdminTryToGet() throws Exception {
        JSONObject createUserRequest = new JSONObject();
        createUserRequest.put("username", "admin");
        createUserRequest.put("password", "****");
        mockMvc.perform(get("/user/list")
                .header("X-SECURITY-ADMIN-KEY", "SUPER_SECRET_KEY_FROM_ADMIN"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyProfile_Test() throws Exception {
        mockMvc.perform(get("/user/me")
                        .header(HttpHeaders.AUTHORIZATION, base64Encoded("Ilya", "lolo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Ilya"))
                .andExpect(jsonPath("$.accounts.length()").value(3));

    }

}
