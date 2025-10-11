package edu.kangwon.university.taxicarpool.auth;

import edu.kangwon.university.taxicarpool.auth.authException.TokenExpiredException;
import edu.kangwon.university.taxicarpool.auth.authException.TokenInvalidException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final long ACCESS_EXPIRATION = 1000L * 60 * 60 * 2; // 2시간
    private final long REFRESH_EXPIRATION = 1000L * 60 * 60 * 24 * 7; // 일주일

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    public String generateAccessToken(Long id, int tokenVersion) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + ACCESS_EXPIRATION);

        return Jwts.builder()
            .setSubject(String.valueOf(id))
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim("ver", tokenVersion)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public String generateRefreshToken(Long id) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + REFRESH_EXPIRATION);

        return Jwts.builder()
            .setSubject(String.valueOf(id))
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public Long getIdFromToken(String token) {
        String subject = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
        return Long.parseLong(subject);
    }

    public int getTokenVersionFromToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody()
            .get("ver", Integer.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()// 이 빌더에서 내부적으로 만료 기한도 검사함.
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("Access 토큰이 만료되었습니다.", e);
        } catch (SignatureException e) {
            // 시그니처 불일치 (위조 가능성)
            throw new TokenInvalidException("토큰 서명이 유효하지 않습니다.", e);
        } catch (MalformedJwtException e) {
            // 형식이 잘못된 JWT
            throw new TokenInvalidException("잘못된 토큰 형식입니다.", e);
        } catch (JwtException e) {
            throw new TokenInvalidException("JWT 처리 중 오류가 발생했습니다.", e);
        }
    }

    // 커스텀 클레임(jti)을 넣어 1회용 비번 재설정 토큰 생성
    public String generatePasswordResetToken(Long memberId, String jti, long expirationMillis) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
            .setSubject(String.valueOf(memberId))
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim("jti", jti) // 1회용 식별자
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    // jti포함한 토큰 검증 + Claims 꺼내기
    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException("토큰이 만료되었습니다.", e);
        } catch (JwtException e) {
            throw new TokenInvalidException("유효하지 않은 토큰입니다.", e);
        }
    }

    public static String newJti() {
        return UUID.randomUUID().toString();
    }


}
