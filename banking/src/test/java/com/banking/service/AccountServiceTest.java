package com.banking.service;

import com.banking.dto.CreateAccountRequest;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.UserNotFoundException;
import com.banking.model.Account;
import com.banking.model.AccountType;
import com.banking.model.Role;
import com.banking.model.User;
import com.banking.repository.AccountRepository;
import com.banking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

    private User user;
    private Account account;
    private CreateAccountRequest createAccountRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRole(Role.CUSTOMER);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        account = new Account();
        account.setId(1L);
        account.setAccountNumber("ACC123456789");
        account.setBalance(BigDecimal.valueOf(1000.00));
        account.setAccountType(AccountType.SAVINGS);
        account.setActive(true);
        account.setUser(user);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        createAccountRequest = new CreateAccountRequest();
        createAccountRequest.setUserId(1L);
        createAccountRequest.setAccountType(AccountType.SAVINGS);
    }

    @Test
    void createAccount_Success() {
        // Given
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        // When
        Account result = accountService.createAccount(createAccountRequest);

        // Then
        assertNotNull(result);
        assertEquals(AccountType.SAVINGS, result.getAccountType());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        assertTrue(result.getActive());
        assertEquals(user, result.getUser());
        assertNotNull(result.getAccountNumber());
        assertTrue(result.getAccountNumber().startsWith("ACC"));

        verify(userRepository).findById(1L);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createAccount_UserNotFound() {
        // Given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> accountService.createAccount(createAccountRequest));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(1L);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void getAccountsByUserId_Success() {
        // Given
        List<Account> expectedAccounts = Arrays.asList(account);
        when(accountRepository.findByUserIdAndActiveTrue(anyLong())).thenReturn(expectedAccounts);

        // When
        List<Account> result = accountService.getAccountsByUserId(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(account, result.get(0));

        verify(accountRepository).findByUserIdAndActiveTrue(1L);
    }

    @Test
    void getAccountById_Success() {
        // Given
        when(accountRepository.findById(anyLong())).thenReturn(Optional.of(account));

        // When
        Account result = accountService.getAccountById(1L);

        // Then
        assertNotNull(result);
        assertEquals(account, result);

        verify(accountRepository).findById(1L);
    }

    @Test
    void getAccountById_NotFound() {
        // Given
        when(accountRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class,
                () -> accountService.getAccountById(1L));

        assertEquals("Account not found", exception.getMessage());
        verify(accountRepository).findById(1L);
    }

    @Test
    void updateBalance_Success() {
        // Given
        BigDecimal newBalance = BigDecimal.valueOf(1500.00);
        when(accountRepository.findById(anyLong())).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        // When
        Account result = accountService.updateBalance(1L, newBalance);

        // Then
        assertNotNull(result);
        assertEquals(newBalance, result.getBalance());
        assertNotNull(result.getUpdatedAt());

        verify(accountRepository).findById(1L);
        verify(accountRepository).save(account);
    }

    @Test
    void updateBalance_AccountNotFound() {
        // Given
        BigDecimal newBalance = BigDecimal.valueOf(1500.00);
        when(accountRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class,
                () -> accountService.updateBalance(1L, newBalance));

        assertEquals("Account not found", exception.getMessage());
        verify(accountRepository).findById(1L);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void createAccount_CurrentAccountType() {
        // Given
        createAccountRequest.setAccountType(AccountType.CURRENT);
        Account currentAccount = new Account();
        currentAccount.setAccountType(AccountType.CURRENT);
        currentAccount.setBalance(BigDecimal.ZERO);
        currentAccount.setActive(true);
        currentAccount.setUser(user);

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(accountRepository.save(any(Account.class))).thenReturn(currentAccount);

        // When
        Account result = accountService.createAccount(createAccountRequest);

        // Then
        assertNotNull(result);
        assertEquals(AccountType.CURRENT, result.getAccountType());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        assertTrue(result.getActive());

        verify(userRepository).findById(1L);
        verify(accountRepository).save(any(Account.class));
    }
}