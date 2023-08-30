package com.skypro.simplebanking.IntegrationTest;

import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import com.skypro.simplebanking.service.AccountService;
import com.skypro.simplebanking.service.UserService;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Base64Utils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class AccountControllerTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    public UserService userService;
    @Autowired
    public AccountService accountService;

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

    private JSONObject getBalanceChangeRequest(Long amount) throws JSONException {
        JSONObject balanceChangeRequest = new JSONObject();
        balanceChangeRequest.put("amount", amount);
        return balanceChangeRequest;
    }

    private Long getAccountId(String userName) {
        long userId = userRepository.findByUsername(userName).orElseThrow().getId();
        Collection<Account> account = accountRepository.findByUserId(userId);
        List<Account> accountList = new ArrayList<>(account);
        return accountList.get(0).getId();
    }

    private String base64Encoded(String userName, String password) {
        return "Basic " + Base64Utils.encodeToString((userName + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void getUserAccount_Test() throws Exception {
        mockMvc.perform(get("/account/{id}", getAccountId("Lila"))
                        .header(HttpHeaders.AUTHORIZATION, base64Encoded("Lila", "dodo")))
                .andExpect(status().isOk());
    }

    @Test
    public void getUserAccount_WhenAccountNotFound() throws Exception {
        Long id = 0L;
        mockMvc.perform(get("/account/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, base64Encoded("Lila", "dodo")))
                .andExpect(status().isNotFound());
    }

    @Test
    public void depositToAccount_Test() throws Exception {
        mockMvc.perform(post("/account/deposit/{id}", getAccountId("Dima"))
                        .header(HttpHeaders.AUTHORIZATION, base64Encoded("Dima", "lili"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getBalanceChangeRequest(1000L).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1001L));
    }

    @Test
    public void depositToAccount_WhenAccountNotFound() throws Exception {
        Long id = -1L;
        mockMvc.perform(post("/account/deposit/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, base64Encoded("Dima", "lili"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getBalanceChangeRequest(1L).toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void withdrawToAccount_WhenInsufficienFunds() throws Exception {
        mockMvc.perform(post("/account/withdraw/{id}", 1)
                        .header(HttpHeaders.AUTHORIZATION, base64Encoded("Ilya", "lolo"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getBalanceChangeRequest(1000L).toString()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void withdrawToAccount_WhenAccountNotFound() throws Exception {
        Long id = 0L;
        mockMvc.perform(post("/account/withdraw/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, base64Encoded("Ilya", "lolo"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getBalanceChangeRequest(1L).toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void withdrawToAccount_WhenInvalidAmount() throws Exception {
        mockMvc.perform(post("/account/withdraw/{id}", getAccountId("Ilya"))
                        .header(HttpHeaders.AUTHORIZATION, base64Encoded("Ilya", "lolo"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getBalanceChangeRequest(-1L).toString()))
                .andExpect(status().isBadRequest());
    }
}
