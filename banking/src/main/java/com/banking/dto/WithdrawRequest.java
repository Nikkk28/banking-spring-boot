package com.banking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class WithdrawRequest {
    @NotNull
    private Long accountId;

    @Positive
    private double amount;
}