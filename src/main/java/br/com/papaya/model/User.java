package br.com.papaya.model;

import br.com.papaya.enums.ContributorTrustStatus;
import br.com.papaya.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Email
    @NotBlank
    @Size(max = 320)
    @Column(nullable = false, length = 320, unique = true)
    private String email;

    @NotBlank
    @Size(max = 100)
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @NotBlank
    @Size(max = 255)
    @Column(name = "password_hash", nullable = false, length = 255)
    @Getter(AccessLevel.NONE)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "contributor_trust_status", nullable = false, length = 20)
    private ContributorTrustStatus contributorTrustStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public User(String email, String displayName, String passwordHash) {
        this.email = normalizeEmail(email);
        this.displayName = displayName == null ? null : displayName.strip();
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.role = UserRole.USER;
        this.contributorTrustStatus = ContributorTrustStatus.UNVERIFIED;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public void verifyContributor() {
        this.contributorTrustStatus = ContributorTrustStatus.VERIFIED;
    }

    public void revokeContributorVerification() {
        this.contributorTrustStatus = ContributorTrustStatus.UNVERIFIED;
    }

    public void grantAdministratorRole() {
        this.role = UserRole.ADMIN;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    private static String normalizeEmail(String email) {
        return Objects.requireNonNull(email, "email must not be null")
                .strip()
                .toLowerCase(Locale.ROOT);
    }
}
