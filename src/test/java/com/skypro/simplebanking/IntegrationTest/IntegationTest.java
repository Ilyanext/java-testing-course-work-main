package com.skypro.simplebanking.IntegrationTest;

import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.dto.UserDTO;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import com.skypro.simplebanking.service.AccountService;
import com.skypro.simplebanking.service.UserService;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public abstract class IntegationTest {

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
    public void createUsersForRepository() {
        userService.createUser("Ilya", "lolo");
        userService.createUser("Dima", "lili");
        userService.createUser("Lila", "dodo");
    }

    @BeforeAll
    public static void startContainer() {
        postgres.start();
    }

    @AfterEach
    void deleteToRepository() {
        userRepository.deleteAll();
        accountRepository.deleteAll();
    }
    @AfterAll
    public static void  stopContainer(){
        postgres.stop();
    }

}
