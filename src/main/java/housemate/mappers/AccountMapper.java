/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package housemate.mappers;

import housemate.constants.Role;
import housemate.entities.UserAccount;
import housemate.models.CreateAccountDTO;
import housemate.models.RegisterAccountDTO;
import housemate.models.UpdateAccountDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author Admin
 */

@Component
public class AccountMapper {

    final String DEFAULT_AVATAR = "https://img.freepik.com/free-psd/3d-illustration-person-with-glasses_23-2149436189.jpg";
    final int MAX_PROFICIENCY_SCORE = 100;
    final int DEFAULT_RATING = 0;

    public UserAccount mapToEntity(RegisterAccountDTO registerAccountDTO) {
        UserAccount userAccount = new UserAccount();

        userAccount.setEmailAddress(registerAccountDTO.getEmail());
        userAccount.setFullName(registerAccountDTO.getFullName());
        userAccount.setPhoneNumber(registerAccountDTO.getPhoneNumber());
        userAccount.setRole(Role.CUSTOMER);
        userAccount.setEmailValidationStatus(false);
        userAccount.setPasswordHash(registerAccountDTO.getPassword());
        userAccount.setAvatar(DEFAULT_AVATAR);
        userAccount.setCreatedAt(LocalDateTime.now());

        return userAccount;
    }

    public UserAccount mapToEntity(CreateAccountDTO createAccountDTO) {
        UserAccount userAccount = new UserAccount();

        userAccount.setFullName(createAccountDTO.getFullName());
        userAccount.setDateOfBirth(createAccountDTO.getDateOfBirth());
        userAccount.setGender(createAccountDTO.getGender());
        userAccount.setPhoneNumber(createAccountDTO.getPhone());
        userAccount.setIdentityCard(createAccountDTO.getIdentityCard());
        userAccount.setEmailAddress(createAccountDTO.getEmail());
        userAccount.setAddress(createAccountDTO.getAddress());
        userAccount.setRole(Role.STAFF);
        userAccount.setProficiencyScore(MAX_PROFICIENCY_SCORE);
        userAccount.setAvgRating(DEFAULT_RATING);
        userAccount.setEmailValidationStatus(true);

        return userAccount;
    }

    public UserAccount updateAccount(UserAccount currentAccount, UpdateAccountDTO updatedAccount) {
        currentAccount.setFullName(updatedAccount.getFullName());
        currentAccount.setDateOfBirth(updatedAccount.getDateOfBirth());
        currentAccount.setGender(updatedAccount.getGender());
        currentAccount.setPhoneNumber(updatedAccount.getPhoneNumber());
        currentAccount.setRole(updatedAccount.getRole());
        currentAccount.setIdentityCard(updatedAccount.getIdentityCard());
        currentAccount.setEmailAddress(updatedAccount.getEmail());
        currentAccount.setAddress(updatedAccount.getAddress());

        return currentAccount;
    }
}

