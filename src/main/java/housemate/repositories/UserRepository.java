/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package housemate.repositories;

import housemate.constants.Role;
import housemate.entities.UserAccount;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


/**
 *
 * @author ThanhF
 */
@Repository
public interface UserRepository extends JpaRepository<UserAccount, Integer> {

    UserAccount findByEmailAddress(String emailAddress);

    UserAccount findByUserId(int userId);

    @Modifying
    @Transactional
    @Query("UPDATE UserAccount u SET u.role = :role WHERE u.userId = :userId")
    void updateRole(@Param("userId") int userId, @Param("role") Role role);

    UserAccount findByResetPasswordToken(String token);

    List<UserAccount> findByRole(Role role);

    @Query(value = "SELECT COUNT(u) FROM UserAccount u")
    int countAllUser();

    @Query(value = "SELECT u FROM UserAccount u WHERE u.role = :role AND u.createdAt BETWEEN :startDate AND :endDate ORDER BY u.createdAt DESC")
    Page<UserAccount> getAllUserByUserRoleAndStartDateToEndDate(@Param("role") Role role, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    @Query("SELECT u FROM UserAccount u WHERE  u.role = :role AND u.fullName LIKE %:fullName%")
    Page<UserAccount> getAllUserByUserRoleAndFullName(@Param("role") Role role, @Param("fullName") String fullName, Pageable pageable);

    UserAccount findByAddress(String address);

    UserAccount findByIdentityCard(String identityCard);
    
    UserAccount findByPhoneNumber(String phoneNumber);
}
