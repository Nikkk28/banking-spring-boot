package com.banking.service;

import com.banking.dto.DepositRequest;
import com.banking.dto.TransferRequest;
import com.banking.dto.WithdrawRequest;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.BankingException;
import com.banking.exception.InsufficientFundsException;
import com.banking.model.*;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
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
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account account;
    private Account fromAccount;
    private Account toAccount;
    private Transaction transaction;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRole(Role.CUSTOMER);

        account = new Account();
        account.setId(1L);
        account.setAccountNumber("ACC123456789");
        account.setBalance(BigDecimal.valueOf(1000.00));
        account.setAccountType(AccountType.SAVINGS);
        account.setActive(true);
        account.setUser(user);

        fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setAccountNumber("ACC123456789");
        fromAccount.setBalance(BigDecimal.valueOf(1000.00));
        fromAccount.setAccountType(AccountType.SAVINGS);
        fromAccount.setActive(true);
        fromAccount.setUser(user);

        toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setAccountNumber("ACC987654321");
        toAccount.setBalance(BigDecimal.valueOf(500.00));
        toAccount.setAccountType(AccountType.CURRENT);
        toAccount.setActive(true);
        toAccount.setUser(user);

        transaction = new Transaction();
        transaction.setId(1L);
        transaction.setTransactionId("TXN123456789");
        transaction.setAmount(BigDecimal.valueOf(100.00));
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void deposit_Success() {
        // Given
        DepositRequest request = new DepositRequest();
        request.setAccountId(1L);
        request.setAmount(100.00);

        when(accountRepository.findById(anyLong())).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        // When
        Transaction result = transactionService.deposit(request);

        // Then
        assertNotNull(result);
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
        assertEquals(BigDecimal.valueOf(1100.00), account.getBalance());

        verify(accountRepository).findById(1L);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(accountRepository).save(account);
    }

    @Test
    void deposit_AccountNotFound() {
        // Given
        DepositRequest request = new DepositRequest();
        request.setAccountId(1L);
        request.setAmount(100.00);

        when(accountRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class,
                () -> transactionService.deposit(request));

        assertEquals("Account not found", exception.getMessage());
        verify(accountRepository).findById(1L);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void deposit_InactiveAccount() {
        // Given
        DepositRequest request = new DepositRequest();
        request.setAccountId(1L);
        request.setAmount(100.00);

        account.setActive(false);
        when(accountRepository.findById(anyLong())).thenReturn(Optional.of(account));

        // When & Then
        BankingException exception = assertThrows(BankingException.class,
                () -> transactionService.deposit(request));

        assertEquals("Account is not active", exception.getMessage());
        verify(accountRepository).findById(1L);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void withdraw_Success() {
        // Given
        WithdrawRequest request = new WithdrawRequest();
        request.setAccountId(1L);
        request.setAmount(100.00);

        when(accountRepository.findById(anyLong())).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        // When
        Transaction result = transactionService.withdraw(request);

        // Then
        assertNotNull(result);
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
        assertEquals(BigDecimal.valueOf(900.00), account.getBalance());

        verify(accountRepository).findById(1L);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(accountRepository).save(account);
    }

    @Test
    void withdraw_InsufficientFunds() {
        // Given
        WithdrawRequest request = new WithdrawRequest();
        request.setAccountId(1L);
        request.setAmount(1500.00);

        when(accountRepository.findById(anyLong())).thenReturn(Optional.of(account));

        // When & Then
        InsufficientFundsException exception = assertThrows(InsufficientFundsException.class,
                () -> transactionService.withdraw(request));

        assertEquals("Insufficient funds", exception.getMessage());
        verify(accountRepository).findById(1L);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void withdraw_AccountNotFound() {
        // Given
        WithdrawRequest request = new WithdrawRequest();
        request.setAccountId(1L);
        request.setAmount(100.00);

        when(accountRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class,
                () -> transactionService.withdraw(request));

        assertEquals("Account not found", exception.getMessage());
        verify(accountRepository).findById(1L);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_Success() {
        // Given
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(200.00);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(accountRepository.save(any(Account.class))).thenReturn(fromAccount, toAccount);

        // When
        Transaction result = transactionService.transfer(request);

        // Then
        assertNotNull(result);
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
        assertEquals(BigDecimal.valueOf(800.00), fromAccount.getBalance());
        assertEquals(BigDecimal.valueOf(700.00), toAccount.getBalance());

        verify(accountRepository).findById(1L);
        verify(accountRepository).findById(2L);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    void transfer_SameAccount() {
        // Given
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(1L);
        request.setAmount(200.00);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));

        // When & Then
        BankingException exception = assertThrows(BankingException.class,
                () -> transactionService.transfer(request));

        assertEquals("Cannot transfer to the same account", exception.getMessage());
        verify(accountRepository, times(2)).findById(1L);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_InsufficientFunds() {
        // Given
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(1500.00);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        // When & Then
        InsufficientFundsException exception = assertThrows(InsufficientFundsException.class,
                () -> transactionService.transfer(request));

        assertEquals("Insufficient funds in source account", exception.getMessage());
        verify(accountRepository).findById(1L);
        verify(accountRepository).findById(2L);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_FromAccountNotFound() {
        // Given
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(200.00);

        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class,
                () -> transactionService.transfer(request));

        assertEquals("Source account not found", exception.getMessage());
        verify(accountRepository).findById(1L);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_ToAccountNotFound() {
        // Given
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(200.00);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.empty());

        // When & Then
        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class,
                () -> transactionService.transfer(request));

        assertEquals("Destination account not found", exception.getMessage());
        verify(accountRepository).findById(1L);
        verify(accountRepository).findById(2L);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void getTransactionHistory_Success() {
        // Given
        List<Transaction> expectedTransactions = Arrays.asList(transaction);
        when(transactionRepository.findByAccountId(anyLong())).thenReturn(expectedTransactions);

        // When
        List<Transaction> result = transactionService.getTransactionHistory(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(transaction, result.get(0));

        verify(transactionRepository).findByAccountId(1L);
    }

    @Test
    void transfer_InactiveFromAccount() {
        // Given
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(200.00);

        fromAccount.setActive(false);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        // When & Then
        BankingException exception = assertThrows(BankingException.class,
                () -> transactionService.transfer(request));

        assertEquals("One or both accounts are not active", exception.getMessage());
        verify(accountRepository).findById(1L);
        verify(accountRepository).findById(2L);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_InactiveToAccount() {
        // Given
        TransferRequest request = new TransferRequest();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(200.00);

        toAccount.setActive(false);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(toAccount));

        // When & Then
        BankingException exception = assertThrows(BankingException.class,
                () -> transactionService.transfer(request));

        assertEquals("One or both accounts are not active", exception.getMessage());
        verify(accountRepository).findById(1L);
        verify(accountRepository).findById(2L);
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}