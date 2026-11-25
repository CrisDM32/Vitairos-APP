package com.vitairos.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/v1")
class Sessions {
    record Profile(String email, String name, String role) {}
    record Login(String email, String password) {}
    record Session(String accessToken, Instant expiresAt, Profile user) {}
    private static final Profile OPERATOR = new Profile("operador@vitairos.demo", "Camila Rojas (demo)", "OPERADOR");
    private static final Profile SUPERVISOR = new Profile("supervisor@vitairos.demo", "Andrés Soto (demo)", "SUPERVISOR");
    private final Map<String, Profile> profiles = Map.of(OPERATOR.email(), OPERATOR, SUPERVISOR.email(), SUPERVISOR);
    private final Map<String, String> passwords;
    private final JdbcTemplate db;
    private final SecureRandom random = new SecureRandom();

    Sessions(JdbcTemplate db, @Value("${account.operator-password}") String operator,
             @Value("${account.supervisor-password}") String supervisor) {
        this.db = db;
        passwords = Map.of(OPERATOR.email(), operator, SUPERVISOR.email(), supervisor);
    }

    @PostMapping("/sessions")
    @ResponseStatus(CREATED)
    Session login(@RequestBody Login login) {
        String email = login.email() == null ? "" : login.email().strip().toLowerCase(Locale.ROOT);
        String expected = passwords.get(email);
        if (expected == null || expected.isBlank() || login.password() == null ||
                !MessageDigest.isEqual(hash(expected), hash(login.password()))) {
            throw new ResponseStatusException(UNAUTHORIZED, "Credenciales inválidas");
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expires = Instant.now().plusSeconds(28800);
        db.update("DELETE FROM sessions WHERE expires_at <= ?", Instant.now().getEpochSecond());
        db.update("INSERT INTO sessions VALUES (?, ?, ?)", digest(token), email, expires.getEpochSecond());
        return new Session(token, expires, profiles.get(email));
    }

    @GetMapping("/me")
    Profile me(@RequestHeader(value="Authorization", required=false) String header) {
        return db.query("SELECT email FROM sessions WHERE digest=? AND expires_at>?",
                (rs, row) -> profiles.get(rs.getString(1)), bearerDigest(header), Instant.now().getEpochSecond())
                .stream().findFirst().orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
    }

    @DeleteMapping("/sessions/current")
    @ResponseStatus(NO_CONTENT)
    void logout(@RequestHeader(value="Authorization", required=false) String header) {
        db.update("DELETE FROM sessions WHERE digest=?", bearerDigest(header));
    }

    private String bearerDigest(String header) {
        if (header == null || !header.startsWith("Bearer ")) throw new ResponseStatusException(UNAUTHORIZED);
        return digest(header.substring(7));
    }

    private static String digest(String token) {
        return HexFormat.of().formatHex(hash(token));
    }

    private static byte[] hash(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
