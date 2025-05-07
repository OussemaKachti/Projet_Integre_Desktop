package com.esprit.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.esprit.models.enums.RoleEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @NotBlank(message = "First name cannot be empty")
    @Size(min = 2, message = "First name must be at least 2 characters long")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s'-]+", message = "First name can only contain letters, spaces, hyphens and apostrophes")
    @Column(name = "prenom")
    private String firstName;

    @NotBlank(message = "Last name cannot be empty")
    @Size(min = 2, message = "Last name must be at least 2 characters long")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s'-]+", message = "Last name can only contain letters, spaces, hyphens and apostrophes")
    @Column(name = "nom")
    private String lastName;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Password cannot be empty")
    @Pattern(regexp = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$", message = "Password must include uppercase, lowercase, numbers, and special characters")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private RoleEnum role;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^((\\+|00)216)?([2579][0-9]{7}|(3[012]|4[01]|8[0128])[0-9]{6}|42[16][0-9]{5})$", message = "Invalid Tunisian phone number format")
    @Column(name = "tel", unique = true)
    private String phone;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Column(name = "status")
    private String status = "active";

    @Column(name = "is_verified")
    private boolean isVerified = false;

    @Column(name = "confirmation_token")
    private String confirmationToken;

    @Column(name = "confirmation_token_expires_at")
    private LocalDateTime confirmationTokenExpiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "warning_count")
    private int warningCount = 0;

    @Column(name = "verification_attempts")
    private Integer verificationAttempts = 0;

    @Column(name = "last_code_sent_time")
    private LocalDateTime lastCodeSentTime;

    // Collections with proper JPA annotations
    @OneToMany(mappedBy = "user")
    private List<Sondage> sondages = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Commentaire> commentaires = new ArrayList<>();

    // Constructors
    public User() {
        this.createdAt = LocalDateTime.now();
    }

    public User(String firstName, String lastName, String email, String password, RoleEnum role) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.createdAt = LocalDateTime.now();
        this.status = "active";
        this.isVerified = false;
        this.warningCount = 0;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return getFirstName() + " " + getLastName();
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public RoleEnum getRole() {
        return role;
    }

    public void setRole(RoleEnum role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getConfirmationToken() {
        return confirmationToken;
    }

    public void setConfirmationToken(String confirmationToken) {
        this.confirmationToken = confirmationToken;
    }

    public LocalDateTime getConfirmationTokenExpiresAt() {
        return confirmationTokenExpiresAt;
    }

    public void setConfirmationTokenExpiresAt(LocalDateTime confirmationTokenExpiresAt) {
        this.confirmationTokenExpiresAt = confirmationTokenExpiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    public Integer getVerificationAttempts() {
        return verificationAttempts == null ? 0 : verificationAttempts;
    }

    public void setVerificationAttempts(Integer verificationAttempts) {
        this.verificationAttempts = verificationAttempts;
    }

    public LocalDateTime getLastCodeSentTime() {
        return lastCodeSentTime;
    }

    public void setLastCodeSentTime(LocalDateTime lastCodeSentTime) {
        this.lastCodeSentTime = lastCodeSentTime;
    }

    // Updated collection getters and setters
    public List<Sondage> getSondages() {
        return sondages;
    }

    public void setSondages(List<Sondage> sondages) {
        this.sondages = sondages;
    }

    public List<Commentaire> getCommentaires() {
        return commentaires;
    }

    public void setCommentaires(List<Commentaire> commentaires) {
        this.commentaires = commentaires;
    }

    public boolean isAdmin() {
        return getRole() == RoleEnum.ADMINISTRATEUR;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", status='" + status + '\'' +
                '}';
    }
}