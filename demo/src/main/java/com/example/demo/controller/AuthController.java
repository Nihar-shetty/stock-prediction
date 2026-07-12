package com.example.demo.controller;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ================= REGISTER =================
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            response.put("error", "Username and password cannot be empty");
            return ResponseEntity.badRequest().body(response);
        }

        if (userRepository.findByUsername(username).isPresent()) {
            response.put("error", "Username already exists");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        String hashedPassword = hashPassword(password);
        if (hashedPassword == null) {
            response.put("error", "Failed to hash password");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        User user = new User(username, hashedPassword);
        userRepository.save(user);

        response.put("message", "User registered successfully");
        response.put("username", username);
        return ResponseEntity.ok(response);
    }

    // ================= LOGIN =================
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            response.put("error", "Username and password are required");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            response.put("error", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        User user = userOpt.get();
        String hashedPassword = hashPassword(password);

        if (hashedPassword == null || !hashedPassword.equals(user.getPassword())) {
            response.put("error", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        // Store user in session
        session.setAttribute("user", user.getUsername());

        response.put("message", "Logged in successfully");
        response.put("username", user.getUsername());
        response.put("watchlist", user.getWatchlist());
        return ResponseEntity.ok(response);
    }

    // ================= LOGOUT =================
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        session.invalidate();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Logged out successfully");
        return ResponseEntity.ok(response);
    }

    // ================= STATUS =================
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String username = (String) session.getAttribute("user");

        if (username == null) {
            response.put("authenticated", false);
            return ResponseEntity.ok(response);
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            session.invalidate();
            response.put("authenticated", false);
            return ResponseEntity.ok(response);
        }

        User user = userOpt.get();
        response.put("authenticated", true);
        response.put("username", user.getUsername());
        response.put("watchlist", user.getWatchlist());
        return ResponseEntity.ok(response);
    }

    // ================= WATCHLIST: GET =================
    @GetMapping("/watchlist")
    public ResponseEntity<Map<String, Object>> getWatchlist(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String username = (String) session.getAttribute("user");

        if (username == null) {
            response.put("error", "Unauthorized access. Please log in first.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            response.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        response.put("watchlist", userOpt.get().getWatchlist());
        return ResponseEntity.ok(response);
    }

    // ================= WATCHLIST: ADD =================
    @PostMapping("/watchlist/add/{stock}")
    public ResponseEntity<Map<String, Object>> addToWatchlist(@PathVariable String stock, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String username = (String) session.getAttribute("user");

        if (username == null) {
            response.put("error", "Unauthorized. Please log in first.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            response.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        User user = userOpt.get();
        user.getWatchlist().add(stock);
        userRepository.save(user);

        response.put("message", "Stock added to watchlist successfully");
        response.put("watchlist", user.getWatchlist());
        return ResponseEntity.ok(response);
    }

    // ================= WATCHLIST: REMOVE =================
    @PostMapping("/watchlist/remove/{stock}")
    public ResponseEntity<Map<String, Object>> removeFromWatchlist(@PathVariable String stock, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String username = (String) session.getAttribute("user");

        if (username == null) {
            response.put("error", "Unauthorized. Please log in first.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            response.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        User user = userOpt.get();
        user.getWatchlist().remove(stock);
        userRepository.save(user);

        response.put("message", "Stock removed from watchlist successfully");
        response.put("watchlist", user.getWatchlist());
        return ResponseEntity.ok(response);
    }

    // ================= HELPERS =================
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
