package com.notes.tasks.demo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/** Runs only with --spring.profiles.active=demo: password hashing and SQL injection, side by side. */
@Component
@Profile("demo")
public class SecurityDemo implements ApplicationRunner {
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;

    public SecurityDemo(PasswordEncoder encoder, JdbcTemplate jdbc) {
        this.encoder = encoder;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("\n=== 1. Password hashing (DelegatingPasswordEncoder -> BCrypt)");
        String h1 = encoder.encode("ravi-secret");
        String h2 = encoder.encode("ravi-secret");
        System.out.println("    hash 1: " + h1);
        System.out.println("    hash 2: " + h2);
        System.out.println("    same password, same hash? " + h1.equals(h2) + " (random salt)");
        System.out.println("    matches(\"ravi-secret\", hash 1): " + encoder.matches("ravi-secret", h1));
        System.out.println("    matches(\"wrong\", hash 1):       " + encoder.matches("wrong", h1));
        String stored = jdbc.queryForObject("select password_hash from app_user where username = 'ravi'", String.class);
        System.out.println("    stored in app_user: " + stored + " -> matches: " + encoder.matches("ravi-secret", stored));
        try {
            encoder.matches("ravi-secret", stored.substring("{bcrypt}".length()));   // a hash without the prefix
        } catch (IllegalArgumentException e) {
            System.out.println("    without the prefix: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        System.out.println("\n=== 2. SQL injection");
        String input = "nobody' OR '1'='1";                 // what an attacker types into a search box
        String unsafeSql = "select username from app_user where username = '" + input + "'";
        System.out.println("    built SQL: " + unsafeSql);
        List<String> leaked = jdbc.queryForList(unsafeSql, String.class);                 // NEVER do this
        System.out.println("    concatenated: " + leaked + "  <- every user");
        List<String> safe = jdbc.queryForList("select username from app_user where username = ?", String.class, input);
        System.out.println("    parameterized: " + safe + "  <- the input is just a strange name");
        System.out.println("\n=== demo finished");
    }
}
