package snvnUserService.icsQuizUserService.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;

@Table("staging_users")
public class StagingUser {

    @Id
    private Long id; // Primary key (auto-increment)

    @Column("user_id")
    private Long userId; // Optional user ID from source system or CSV

    @Column("name")
    private String name;

    @Column("code_number")
    private String codeNumber;

    @Column("password_plain")
    private String passwordPlain;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    public StagingUser() {}

    public StagingUser(Long userId, String name, String codeNumber, String passwordPlain) {
        this.userId = userId;
        this.name = name;
        this.codeNumber = codeNumber;
        this.passwordPlain = passwordPlain;
        this.createdAt = LocalDateTime.now();
    }

    // ✅ Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCodeNumber() { return codeNumber; }
    public void setCodeNumber(String codeNumber) { this.codeNumber = codeNumber; }

    public String getPasswordPlain() { return passwordPlain; }
    public void setPasswordPlain(String passwordPlain) { this.passwordPlain = passwordPlain; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "StagingUser{" +
                "id=" + id +
                ", userId=" + userId +
                ", name='" + name + '\'' +
                ", codeNumber='" + codeNumber + '\'' +
                ", passwordPlain='" + passwordPlain + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
