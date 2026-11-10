package com.example.socket_netty_server.myNetty;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtVerifier {

    private final SecretKey accessTokenKey;
    private final String issuer;
    private final String audience;

    private static final Logger logger = LoggerFactory.getLogger(JwtVerifier.class);

    public JwtVerifier(@Value("${jwt.access-token-key}") String accessTokenKey,
            @Value("${jwt.issuer}") String issuer, @Value("${jwt.audience}") String audience) {

        logger.info("issuer :: {}", issuer);

        // byte[] keyBytes = Decoders.BASE64.decode(accessTokenKeyBase64);

        byte[] keyBytes = accessTokenKey.getBytes();
        this.accessTokenKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = issuer;
        this.audience = audience;
    }

    public Claims verify(String jwt) {

        logger.info(jwt);

        return Jwts.parser()
                .verifyWith(accessTokenKey)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }
}
