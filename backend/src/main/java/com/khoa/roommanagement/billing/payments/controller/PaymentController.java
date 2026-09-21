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
import org.springframework.web.bind.annotation.RequestHeader;
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
        @Valid @RequestBody ConfirmPaymentRequest request,
        @RequestHeader(name = "Idempotency-Key") String idempotencyKey
    ) {
        Payment payment = paymentService.confirmPayment(
            id,
            request.paidAt(),
            request.note(),
            idempotencyKey
        );
        return PaymentResponse.from(payment);
    }

    @GetMapping("/payments")
    public Page<PaymentResponse> getPayments(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) Integer pageSize,
        @RequestParam(required = false) Boolean onTime
    ) {
        int requestedSize = pageSize != null ? pageSize : size != null ? size : 12;
        int boundedSize = Math.min(Math.max(requestedSize, 1), 100);
        return paymentService.getPayments(onTime, PageRequest.of(Math.max(page, 0), boundedSize))
            .map(PaymentResponse::from);
    }

    @GetMapping("/payments/{id}")
    public PaymentResponse getPayment(@PathVariable UUID id) {
        return PaymentResponse.from(paymentService.getPaymentById(id));
    }
}
