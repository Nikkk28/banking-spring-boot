package com.banking.service;

import com.banking.dto.DepositRequest;
import com.banking.dto.TransferRequest;
import com.banking.dto.WithdrawRequest;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.BankingException;
import com.banking.exception.InsufficientFundsException;
import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.TransactionStatus;
import com.banking.model.TransactionType;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public Transaction deposit(DepositRequest request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        if (!account.getActive()) {
            throw new BankingException("Account is not active");
        }

        BigDecimal amount = BigDecimal.valueOf(request.getAmount());

        // Create transaction
        Transaction transaction = createTransaction(
                TransactionType.DEPOSIT,
                amount,
                null,
                account,
                "Deposit to account " + account.getAccountNumber()
        );

        try {
            // Update account balance
            account.setBalance(account.getBalance().add(amount));
            account.setUpdatedAt(LocalDateTime.now());
            accountRepository.save(account);

            // Mark transaction as completed
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setProcessedAt(LocalDateTime.now());

            return transactionRepository.save(transaction);
        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new BankingException("Deposit failed: " + e.getMessage());
        }
    }

    @Transactional
    public Transaction withdraw(WithdrawRequest request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        if (!account.getActive()) {
            throw new BankingException("Account is not active");
        }

        BigDecimal amount = BigDecimal.valueOf(request.getAmount());

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        // Create transaction
        Transaction transaction = createTransaction(
                TransactionType.WITHDRAWAL,
                amount,
                account,
                null,
                "Withdrawal from account " + account.getAccountNumber()
        );

        try {
            // Update account balance
            account.setBalance(account.getBalance().subtract(amount));
            account.setUpdatedAt(LocalDateTime.now());
            accountRepository.save(account);

            // Mark transaction as completed
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setProcessedAt(LocalDateTime.now());

            return transactionRepository.save(transaction);
        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new BankingException("Withdrawal failed: " + e.getMessage());
        }
    }

    @Transactional
    public Transaction transfer(TransferRequest request) {
        Account fromAccount = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Source account not found"));

        Account toAccount = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Destination account not found"));

        if (!fromAccount.getActive() || !toAccount.getActive()) {
            throw new BankingException("One or both accounts are not active");
        }

        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new BankingException("Cannot transfer to the same account");
        }

        BigDecimal amount = BigDecimal.valueOf(request.getAmount());

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds in source account");
        }

        // Create transaction
        Transaction transaction = createTransaction(
                TransactionType.TRANSFER,
                amount,
                fromAccount,
                toAccount,
                "Transfer from " + fromAccount.getAccountNumber() + " to " + toAccount.getAccountNumber()
        );

        try {
            // Update account balances
            fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
            toAccount.setBalance(toAccount.getBalance().add(amount));

            LocalDateTime now = LocalDateTime.now();
            fromAccount.setUpdatedAt(now);
            toAccount.setUpdatedAt(now);

            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);

            // Mark transaction as completed
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setProcessedAt(LocalDateTime.now());

            return transactionRepository.save(transaction);
        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            throw new BankingException("Transfer failed: " + e.getMessage());
        }
    }

    public List<Transaction> getTransactionHistory(Long accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    private Transaction createTransaction(TransactionType type, BigDecimal amount,
                                          Account fromAccount, Account toAccount, String description) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(generateTransactionId());
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setDescription(description);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    private String generateTransactionId() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}