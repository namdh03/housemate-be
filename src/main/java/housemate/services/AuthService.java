/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package housemate.services;

import housemate.constants.AccountStatus;
import housemate.entities.JwtPayload;
import housemate.entities.UserAccount;
import housemate.mappers.AccountMapper;
import housemate.mappers.JwtPayloadMapper;
import housemate.models.ForgotPasswordDTO;
import housemate.models.LoginAccountDTO;
import housemate.models.RegisterAccountDTO;
import housemate.models.ResetPasswordDTO;
import housemate.repositories.UserRepository;
import housemate.utils.BcryptUtil;
import housemate.utils.JwtUtil;
import java.net.URI;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author hdang09
 */
@Service
public class AuthService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    BcryptUtil bcryptUtil;

    @Autowired
    AccountMapper accountMapper;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${url.client}")
    private String URL_CLIENT;

    public ResponseEntity<List<UserAccount>> getAll() {
        return ResponseEntity.status(HttpStatus.OK).body(userRepository.findAll());
    }

    public ResponseEntity<String> login(LoginAccountDTO loginAccountDTO) {
        UserAccount accountDB = userRepository.findByEmailAddress(loginAccountDTO.getEmail());

        // Check email not in database
        if (accountDB == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("This email haven't been created");
        }

        if (accountDB.getAccountStatus().equals(AccountStatus.BANNED) || accountDB.getAccountStatus().equals(AccountStatus.INACTIVE)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are banned");
        }

        // Check if account logged in with Google
        if (accountDB.getPasswordHash() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Username or password not found");
        }

        // Check correct password
        boolean isCorrect = bcryptUtil.checkpw(loginAccountDTO.getPassword(), accountDB.getPasswordHash());
        if (!isCorrect) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email or password not correct");
        }

        // Generate token
        JwtPayload jwtPayload = new JwtPayloadMapper().mapFromUserAccount(accountDB);
        Map<String, Object> payload = jwtPayload.toMap();
        String token = jwtUtil.generateToken(payload);

        return ResponseEntity.status(HttpStatus.OK).body(token);
    }

    public ResponseEntity<String> register(RegisterAccountDTO registerAccountDTO) {
        UserAccount accountDB = userRepository.findByEmailAddress(registerAccountDTO.getEmail());

        // Check email exists database
        if (accountDB != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("This email have been created before");
        }

         UserAccount accountByPhone = userRepository.findByPhoneNumber(registerAccountDTO.getPhoneNumber());
        if (accountByPhone != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("This phone number has been registered before");
        }

        // Insert to database
        String hash = bcryptUtil.hashPassword(registerAccountDTO.getPassword());
        registerAccountDTO.setPassword(hash);
        UserAccount userAccount = accountMapper.mapToEntity(registerAccountDTO);
        userAccount = userRepository.save(userAccount);

        // Generate token
        JwtPayload jwtPayload = new JwtPayloadMapper().mapFromUserAccount(userAccount);
        Map<String, Object> payload = jwtPayload.toMap();
        String token = jwtUtil.generateToken(payload);

        return ResponseEntity.status(HttpStatus.OK).body(token);
    }

    private void sendEmail(String recipientEmail, String resetPasswordLink) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom("smtp.housemate@gmail.com", "HouseMate Security");
        helper.setTo(recipientEmail);

        String subject = "Here's the link to reset your password";
        String content = "<p>Hello,</p>"
                + "<p>You have requested to reset your password.</p>"
                + "<p>Click the link below to change your password:</p>"
                + "<p><a href=\"" + resetPasswordLink + "\">Change my password</a></p>"
                + "<br>"
                + "<p>Ignore this email if you do remember your password, "
                + "or you have not made the request.</p>";

        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

    private String generateRandomString() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 30);
    }

    public ResponseEntity<String> forgotPassword(ForgotPasswordDTO forgotPasswordDTO) {
        String token = generateRandomString();
        String email = forgotPasswordDTO.getEmail();
        try {
            // Check account in database
            UserAccount account = userRepository.findByEmailAddress(email);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Can't find this email!");
            }

            // Store reset_password_token to account
            account.setResetPasswordToken(token);
            userRepository.save(account);

            // Send email
            String resetPasswordLink = URL_CLIENT + "/set-password?token=" + token;
            sendEmail(email, resetPasswordLink);
            return ResponseEntity.status(HttpStatus.OK).body("We have sent a reset password link to your email. Please check.");
        } catch (UnsupportedEncodingException | MessagingException e) {
            return ResponseEntity.status(HttpStatus.OK).body("Error while sending email");
        }
    }

    public ResponseEntity<String> resetPassword(ResetPasswordDTO resetPasswordDTO) {
        String token = resetPasswordDTO.getToken();
        UserAccount account = userRepository.findByResetPasswordToken(token);

        // Check email not in database
        if (account == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Token is invalid");
        }

        // Set new password
        String password = resetPasswordDTO.getPassword();
        String hash = bcryptUtil.hashPassword(password);
        account.setPasswordHash(hash);
        account.setResetPasswordToken(null);
        userRepository.save(account);
        return ResponseEntity.status(HttpStatus.OK).body("Set new password successfully!");
    }

    public ResponseEntity<String> loginWithGoogle(OAuth2AuthenticationToken oAuth2AuthenticationToken) {
        Map<String, Object> userOAuth = oAuth2AuthenticationToken.getPrincipal().getAttributes();
        String email = (String) userOAuth.get("email");
        String fullName = (String) userOAuth.get("name");
        boolean emailVerified = (boolean) userOAuth.get("email_verified");
        String avatar = (String) userOAuth.get("picture");

        UserAccount userAccount = userRepository.findByEmailAddress(email);

        //if userAccount not exist -> create new user
        if (userAccount == null) {
            UserAccount newUser = new UserAccount(fullName, email, emailVerified, avatar);
            userAccount = userRepository.save(newUser);
        }

        if (userAccount.getAccountStatus().equals(AccountStatus.BANNED) || userAccount.getAccountStatus().equals(AccountStatus.INACTIVE)) {
            //Create uri with token for redirect
            String url = URL_CLIENT + "/" + "?success=false&message=You%20are%20banned";
            URI uri = URI.create(url);

            return ResponseEntity.status(HttpStatus.FOUND).location(uri).build();
        }

        //Create jwt payload
        JwtPayload jwtPayload = new JwtPayload(userAccount.getUserId(), fullName, email, userAccount.getRole().toString());
        Map<String, Object> payload = jwtPayload.toMap();

        //generate token with payload
        String token = jwtUtil.generateToken(payload);

        //Create uri with token for redirect
        String url = URL_CLIENT + "/" + "?success=true&token=" + token;
        URI uri = URI.create(url);

        //REMOVE JSESSIONID
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        return ResponseEntity.status(HttpStatus.FOUND).location(uri).build();
    }
}
