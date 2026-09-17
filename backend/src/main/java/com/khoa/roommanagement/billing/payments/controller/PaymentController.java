package com.khoa.roommanagement.billing.payments.controller;

import com.khoa.roommanagement.billing.payments.dto.ConfirmPaymentRequest;
import com.khoa.roommanagement.billing.payments.dto.PaymentResponse;
import com.khoa.roommanagement.billing.payments.entity.Payment;
import com.khoa.roommanagement.billing.payments.service.PaymentService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/bills/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse confirmPayment(
        @PathVariable UUID id,
        @Valid @RequestBody ConfirmPaymentRequest request
    ) {
        Payment payment = paymentService.confirmPayment(
            id,
            request.paidAt(),
            request.note(),
            request.idempotencyKey()
        );
        return PaymentResponse.from(payment);
    }

    @GetMapping("/payments")
    public Page<PaymentResponse> getPayments(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "12") int size,
        @RequestParam(required = false) Boolean onTime
    ) {
        return paymentService.getPayments(onTime, PageRequest.of(page, size))
            .map(PaymentResponse::from);
    }

    @GetMapping("/payments/{id}")
    public PaymentResponse getPayment(@PathVariable UUID id) {
        return PaymentResponse.from(paymentService.getPaymentById(id));
    }
}
