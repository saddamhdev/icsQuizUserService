package snvnUserService.icsQuizUserService.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;

@Table("users")
public class User {

    @Id
    @Column("id") // primary key
    private Long id;

    @Column("user_id") // your custom logical user ID
    private Long userId;

    @Column("name")
    private String name;

    @Column("code_number")
    private String codeNumber;

    @Column("password")
    private String password;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    public User() {}

    public User(Long userId, String name, String codeNumber, String password) {
        this.userId = userId;
        this.name = name;
        this.codeNumber = codeNumber;
        this.password = password;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

}
