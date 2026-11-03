package com.aijapanese.speaking.auth;

import com.aijapanese.speaking.auth.security.JwtTokenProvider;
import com.aijapanese.speaking.support.TestUsers;
import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /api/auth/me를 Spring Security 필터 체인까지 포함해 MockMvc로 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthMeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    void me_returnsProvider() throws Exception {
        User user = TestUsers.learner(userRepository, "auth-me");
        String token = jwtTokenProvider.generateToken(user.getId(), user.getRole());

        String body = mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.provider").value("google"))
                .andExpect(jsonPath("$.role").value("LEARNER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // 계약(User 스키마)의 필드만 있어야 한다. 예전 기관·부서 필드가 남아 있으면 실패한다.
        Set<String> fieldNames = new HashSet<>();
        objectMapper.readTree(body).fieldNames().forEachRemaining(fieldNames::add);
        assertThat(fieldNames).containsExactlyInAnyOrder("id", "name", "email", "provider", "role", "status");
    }

    @Test
    void me_withoutToken_is401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // TC-11 백엔드 부분: 서명이 틀린 토큰과 만료된 토큰은 모두 401이다.
    @Test
    void me_withInvalidToken_is401() throws Exception {
        User user = TestUsers.learner(userRepository, "auth-me-invalid");
        Instant now = Instant.now();

        String wrongSignature = Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(
                        "different-test-signing-key-0123456789abcdef".getBytes(StandardCharsets.UTF_8)))
                .compact();

        String expired = Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now.minusSeconds(7200)))
                .expiration(Date.from(now.minusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + wrongSignature))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }
}
