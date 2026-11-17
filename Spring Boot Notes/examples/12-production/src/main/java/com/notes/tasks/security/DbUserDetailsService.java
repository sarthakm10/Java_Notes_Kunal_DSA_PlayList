package com.notes.tasks.security;

import com.notes.tasks.model.AppUser;
import com.notes.tasks.repo.AppUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tells Spring Security how to load a user by username: from the app_user table. */
@Service
public class DbUserDetailsService implements UserDetailsService {
    private final AppUserRepository users;

    public DbUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        AppUser u = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user " + username));
        return User.withUsername(u.getUsername())
                .password(u.getPasswordHash())          // the BCrypt hash; Spring compares, we never decode
                .roles(u.getRole())                     // "ADMIN" becomes the authority "ROLE_ADMIN"
                .build();
    }
}
