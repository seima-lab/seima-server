package vn.fpt.seima.seimaserver.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.fpt.seima.seimaserver.dto.request.user.UserInGoogleReponseDto;
import vn.fpt.seima.seimaserver.entity.User;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretString;

    @Value("${jwt.access.token.expiration}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh.token.expiration}")
    private long refreshTokenExpirationMs;

    private Key secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secretString.getBytes());
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String generateAccessToken(UserInGoogleReponseDto user) {
        Map<String, Object> claims = new HashMap<>();
        // You can add more claims here if needed, e.g., roles
        return createToken(claims, user.getEmail(), accessTokenExpirationMs);
    }

    public String generateRefreshToken(UserInGoogleReponseDto user) {
        return createToken(new HashMap<>(), user.getEmail(), refreshTokenExpirationMs);
    }

    private String createToken(Map<String, Object> claims, String subject, long expirationTimeMs) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTimeMs))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, User userDetails) {
        final String email = extractEmail(token);
        return (email.equals(userDetails.getUserEmail()) && !isTokenExpired(token));
    }

    public Boolean validateToken(String token) { // Simpler validation if you don't have userDetails yet
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            // Log different exceptions: MalformedJwtException, ExpiredJwtException, UnsupportedJwtException, IllegalArgumentException
            return false;
        }
    }
}
