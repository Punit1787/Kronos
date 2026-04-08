package com.vit.timetable.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Base64;
import java.util.Map;

/**
 * AuthController — Simple endpoint to verify admin password.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Value("${admin.password:admin123}")
    private String adminPassword;

    @PostMapping("/check")
    public ResponseEntity<?> checkPassword(@RequestBody Map<String, String> body) {
        String password = body.get("password");
        if (password != null && password.equals(adminPassword)) {
            String token = Base64.getEncoder().encodeToString(password.getBytes());
            return ResponseEntity.ok(Map.of("success", true, "token", token));
        }
        return ResponseEntity.status(403).body(Map.of("success", false, "error", "Wrong password"));
    }
}
