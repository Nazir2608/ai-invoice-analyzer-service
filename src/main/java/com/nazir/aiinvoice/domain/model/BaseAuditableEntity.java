package com.nazir.aiinvoice.domain.model;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseAuditableEntity {

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(updatable = false)
    private String createdBy;

    private LocalDateTime updatedAt;

    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.createdBy = "SYSTEM";
        this.updatedBy = "SYSTEM";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = "SYSTEM";
    }
}
