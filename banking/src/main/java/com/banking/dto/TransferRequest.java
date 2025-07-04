package com.banking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TransferRequest {
    @NotNull
    private Long fromAccountId;

    @NotNull
    private Long toAccountId;

    @Positive
    private double amount;
}