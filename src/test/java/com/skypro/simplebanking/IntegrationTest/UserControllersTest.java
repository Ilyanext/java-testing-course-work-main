package com.skypro.simplebanking.IntegrationTest;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Testcontainers
public class UserControllersTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    public UserService userService;
    @Autowired
    public UserRepository userRepository;
    @Autowired
    public AccountRepository accountRepository;
    @Autowired
    private DataSource dataSource;

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withUsername("banking")
            .withPassword("super-safe-pass");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void createUsersForRepository() {
        userService.createUser("Ilya", "lolo");
        userService.createUser("Dima", "lili");
        userService.createUser("Lila", "dodo");
    }

    @AfterEach
    void deleteToRepository() {
        userRepository.deleteAll();
        accountRepository.deleteAll();
    }

    private String base64Encoded(String userName, String password) {
        return "Basic " + Base64Utils.encodeToString((userName + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createUser_Test() throws Exception {
        deleteToRepository();
        JSONObject createUserRequest = new JSONObject();
        createUserRequest.put("username", "username");
        createUserRequest.put("password", "password");
        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserRequest.toString()))
                .andExpect(status().isOk());
    }
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createUser_WhenUserIsExistException() throws Exception {
        JSONObject createUserRequest = new JSONObject();
        createUserRequest.put("username", "Ilya");
        createUserRequest.put("password", "lolo");
        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserRequest.toString()))
                .andExpect(status().isBadRequest());
    }
    @Test
    @WithMockUser(username = "user", roles = "USER")
    void getListUsers_Test() throws Exception {
        mockMvc.perform(get("/user/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$.length()").value(3));
    }
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getListUsers_WhenAdminTryToGet() throws Exception {
        mockMvc.perform(get("/user/list"))
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
