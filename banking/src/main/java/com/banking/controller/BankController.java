package com.banking.controller;

import com.banking.dto.CreateAccountRequest;
import com.banking.dto.DepositRequest;
import com.banking.dto.TransferRequest;
import com.banking.dto.WithdrawRequest;
import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.service.AccountService;
import com.banking.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank")
@RequiredArgsConstructor
public class BankController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @PostMapping("/accounts")
    public ResponseEntity<Account> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.createAccount(request);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/accounts/{userId}")
    public ResponseEntity<List<Account>> getAccountsForUser(@PathVariable Long userId) {
        List<Account> accounts = accountService.getAccountsByUserId(userId);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<Account> getAccountById(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.getAccountById(accountId));
    }

    @PostMapping("/deposit")
    public ResponseEntity<Transaction> deposit(@Valid @RequestBody DepositRequest request) {
        Transaction tx = transactionService.deposit(request);
        return ResponseEntity.ok(tx);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Transaction> withdraw(@Valid @RequestBody WithdrawRequest request) {
        Transaction tx = transactionService.withdraw(request);
        return ResponseEntity.ok(tx);
    }

    @PostMapping("/transfer")
    public ResponseEntity<Transaction> transfer(@Valid @RequestBody TransferRequest request) {
        Transaction tx = transactionService.transfer(request);
        return ResponseEntity.ok(tx);
    }

    @GetMapping("/transactions/{accountId}")
    public ResponseEntity<List<Transaction>> getTransactionHistory(@PathVariable Long accountId) {
        List<Transaction> history = transactionService.getTransactionHistory(accountId);
        return ResponseEntity.ok(history);
    }
}
