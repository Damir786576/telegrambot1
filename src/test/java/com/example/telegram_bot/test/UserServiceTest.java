package com.example.telegram_bot.test;

import com.example.telegram_bot.dto.UserDto;
import com.example.telegram_bot.jpa.Role;
import com.example.telegram_bot.jpa.UserEntity;
import com.example.telegram_bot.repository.UserRepository;
import com.example.telegram_bot.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository repo;
    @InjectMocks private UserService service;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);
        user.setChatId(123L);
        user.setName("Иван");
        user.setRole(Role.ROLE_USER);
    }

    @Test
    void testGetAllUsersDto() {
        when(repo.findAll()).thenReturn(Arrays.asList(user));

        List<UserDto> result = service.getAllUsersDto();

        assertEquals(1, result.size());
        assertEquals("Иван", result.get(0).getName());
        verify(repo).findAll();
    }

    @Test
    void testGetUserByChatIdDto() {
        when(repo.findByChatId(123L)).thenReturn(user);

        UserDto result = service.getUserByChatIdDto(123L);

        assertNotNull(result);
        assertEquals(123L, result.getChatId());
        assertEquals("ROLE_USER", result.getRole());
        verify(repo).findByChatId(123L);
    }

    @Test
    void testIsAdmin() {
        when(repo.findByChatId(123L)).thenReturn(user);
        user.setRole(Role.ROLE_ADMIN);

        boolean result = service.isAdmin(123L);

        assertTrue(result);
        verify(repo).findByChatId(123L);
    }
}