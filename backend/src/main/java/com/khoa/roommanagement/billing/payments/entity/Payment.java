package com.khoa.roommanagement.billing.payments.entity;

import com.khoa.roommanagement.billing.bills.entity.Bill;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @OneToOne
    @JoinColumn(name = "bill_id", nullable = false, unique = true)
    private Bill bill;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "on_time", nullable = false)
    private boolean onTime;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Payment() {
    }

    public static Payment confirm(Bill bill, Instant paidAt, String note, String idempotencyKey) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID();
        payment.bill = bill;
        payment.paidAt = paidAt;
        payment.note = note;
        payment.onTime = !paidAt.isAfter(bill.getDueDate().atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant());
        payment.idempotencyKey = idempotencyKey;
        payment.createdAt = Instant.now();
        return payment;
    }

    public UUID getId() {
        return id;
    }

    public Bill getBill() {
        return bill;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public String getNote() {
        return note;
    }

    public boolean isOnTime() {
        return onTime;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
