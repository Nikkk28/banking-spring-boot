package com.banking.dto;

import com.banking.model.AccountType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAccountRequest {
    @NotNull
    private Long userId;

    @NotNull
    private AccountType accountType;
}