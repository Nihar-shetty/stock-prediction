package com.example.demo.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Value("${default.user.username:nihar}")
    private String defaultUsername;

    @Value("${default.user.password:nihar123}")
    private String defaultPassword;

    public DatabaseInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUsername(defaultUsername).isEmpty()) {
            String hashedPassword = hashPassword(defaultPassword);
            if (hashedPassword != null) {
                User user = new User(defaultUsername, hashedPassword);
                userRepository.save(user);
                System.out.println("==================================================");
                System.out.println("Default user initialized successfully!");
                System.out.println("Username: " + defaultUsername);
                System.out.println("Password: " + defaultPassword);
                System.out.println("==================================================");
            }
        } else {
            System.out.println("Default user '" + defaultUsername + "' already exists in database.");
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
