package com.aijapanese.speaking.preference.repository;

import com.aijapanese.speaking.preference.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    Optional<UserPreference> findByUser_Id(Long userId);
}
