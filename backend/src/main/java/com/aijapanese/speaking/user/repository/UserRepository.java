package com.aijapanese.speaking.user.repository;

import com.aijapanese.speaking.user.entity.OAuthProvider;
import com.aijapanese.speaking.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    Optional<User> findByEmail(String email);
}
