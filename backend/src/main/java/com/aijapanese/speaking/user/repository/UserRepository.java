package com.aijapanese.speaking.user.repository;

import com.aijapanese.speaking.user.entity.User;
import com.aijapanese.speaking.user.entity.UserRole;
import com.aijapanese.speaking.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByIdAndOrganization_IdAndRole(Long id, Long organizationId, UserRole role);

    Optional<User> findByIdAndRole(Long id, UserRole role);

    /**
     * keyword는 빈 문자열("")로 "검색어 없음"을 표현한다 (null이 아님).
     * PostgreSQL은 LOWER()/CONCAT() 안에서만 쓰이는 null 파라미터의 타입을 추론하지 못해
     * "function lower(bytea) does not exist" 오류를 던지므로, 항상 타입이 있는 문자열을 바인딩한다.
     */
    @Query("SELECT u FROM User u WHERE u.role = :role "
            + "AND (:organizationId IS NULL OR u.organization.id = :organizationId) "
            + "AND (:status IS NULL OR u.status = :status) "
            + "AND (:keyword = '' OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
            + "ORDER BY u.createdAt DESC")
    List<User> search(
            @Param("role") UserRole role,
            @Param("organizationId") Long organizationId,
            @Param("status") UserStatus status,
            @Param("keyword") String keyword
    );
}
