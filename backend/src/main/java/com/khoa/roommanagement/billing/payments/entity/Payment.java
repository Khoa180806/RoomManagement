package com.khoa.roommanagement.billing.payments.entity;

import com.khoa.roommanagement.billing.bills.entity.Bill;
import com.khoa.roommanagement.common.time.BusinessZone;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
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

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToOne(mappedBy = "payment", fetch = FetchType.EAGER)
    private Receipt receipt;

    protected Payment() {
    }

    public static Payment confirm(Bill bill, Instant paidAt, String note, String idempotencyKey) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID();
        payment.bill = bill;
        payment.paidAt = paidAt;
        payment.note = note;
        // Cả ngày đến hạn đều tính là đúng hạn; ranh giới là đầu ngày sau ngày đến hạn.
        Instant dueEndExclusive = bill.getDueDate()
            .plusDays(1)
            .atStartOfDay(BusinessZone.VIETNAM)
            .toInstant();
        payment.onTime = paidAt.isBefore(dueEndExclusive);
        payment.idempotencyKey = idempotencyKey;
        payment.payloadHash = hashPayload(bill.getId(), paidAt, note);
        payment.createdAt = Instant.now();
        return payment;
    }

    /**
     * Hash nội dung yêu cầu thanh toán để phát hiện idempotency key dùng lại
     * cho nội dung khác.
     */
    public static String hashPayload(UUID billId, Instant paidAt, String note) {
        String raw = billId + "|" + paidAt + "|" + (note == null ? "" : note);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 không khả dụng", exception);
        }
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

    public String getPayloadHash() {
        return payloadHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Receipt getReceipt() {
        return receipt;
    }
}
