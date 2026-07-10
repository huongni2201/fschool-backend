package com.fschool.edu.fschool_backend.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudentTuitionResponse(
        String semester,
        Student student,
        long totalAmount,
        long paidAmount,
        long remainingAmount,
        LocalDate nextDueDate,
        int paymentProgress,
        String statusLabel,
        PaymentInfo paymentInfo,
        List<FeeItem> feeItems,
        List<Transaction> transactions) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Student(String name, String className) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PaymentInfo(
            String bankName,
            String accountNumber,
            String accountName,
            String transferContent,
            String qrCodeUrl) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FeeItem(
            String id,
            String title,
            String description,
            long amount,
            long paidAmount,
            LocalDate dueDate,
            String status,
            String statusLabel) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Transaction(
            String id,
            String title,
            String method,
            long amount,
            Instant date,
            String code) {
    }
}
