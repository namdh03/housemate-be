/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package housemate.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import housemate.constants.AccountStatus;
import housemate.constants.Gender;
import housemate.constants.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 *
 * @author ThanhF
 */
@Entity
@Table(name = "user_account")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private int userId;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Role role = Role.CUSTOMER;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_number", nullable = true, unique = true)
    private String phoneNumber;

    @JsonIgnore
    @Column(name = "password_hash", nullable = true)
    private String passwordHash;

    @Column(name = "email_address", unique = true)
    private String emailAddress;

    @Column(name = "email_validation_status")
    private boolean emailValidationStatus;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "address")
    private String address;

    @JsonIgnore
    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @Column(name = "proficiency_score")
    private int proficiencyScore;

    @Column(name = "avg_rating")
    private int avgRating;

    @Column(name = "account_status")
    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus = AccountStatus.ACTIVE; // Default: active

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "identity_card")
    private String identityCard;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    public UserAccount(String fullName, String emailAddress, boolean emailValidationStatus, String avatar) {
        this.fullName = fullName;
        this.emailAddress = emailAddress;
        this.emailValidationStatus = emailValidationStatus;
        this.avatar = avatar;
    }

}