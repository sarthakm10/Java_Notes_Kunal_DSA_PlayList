package com.notes.tasks.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** POST /api/auth/token with HTTP Basic credentials -> a signed JWT valid for app.jwt.lifetime (default 15 minutes). */
@RestController
public class TokenController {
    private final JwtEncoder encoder;
    private final Duration lifetime;

    public TokenController(JwtEncoder encoder, JwtProperties jwt) {
        this.encoder = encoder;
        this.lifetime = jwt.lifetime();
    }

    @PostMapping("/api/auth/token")
    public Map<String, Object> token(Authentication auth) {          // already authenticated by HTTP Basic
        Instant now = Instant.now();                                 // real time: decoders check exp against it
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))                  // skip e.g. FACTOR_PASSWORD (Security 7)
                .map(a -> a.substring("ROLE_".length()))
                .toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("task-tracker")
                .subject(auth.getName())                             // who
                .issuedAt(now)
                .expiresAt(now.plus(lifetime))                       // until when
                .claim("roles", roles)                               // what they may do
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return Map.of("access_token", token, "token_type", "Bearer", "expires_in", lifetime.toSeconds());
    }
}
