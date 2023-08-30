package com.skypro.simplebanking;

import com.skypro.simplebanking.dto.UserDTO;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.UserRepository;
import com.skypro.simplebanking.service.AccountService;
import com.skypro.simplebanking.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private  UserRepository userRepository;
    @Mock
    private AccountService accountService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    public static final User user1 = new User();

    @Test
    private  void createUser_TestAddUser (){
        user1.setUsername("Ilya");
        user1.setPassword("lolo");

        when(userRepository.findByUsername(user1.getUsername()));
        UserDTO userDTO = userService.createUser("Ilya", "lolo");
        assertEquals(user1.getUsername(), userDTO.getUsername());

    }
}
