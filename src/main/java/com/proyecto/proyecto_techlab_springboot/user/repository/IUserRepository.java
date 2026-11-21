package com.proyecto.proyecto_techlab_springboot.user.repository;

import com.proyecto.proyecto_techlab_springboot.user.model.Users;
import com.proyecto.proyecto_techlab_springboot.common.model.RowStatus;
import com.proyecto.proyecto_techlab_springboot.user.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IUserRepository extends JpaRepository<Users, UUID> {
    Optional<Users> findByUserEmailIgnoreCase(String email);

    @Query("""
            SELECT u
            FROM Users u
            WHERE u.userRole IN :allowedRoles
              AND (:roleFilter IS NULL OR u.userRole = :roleFilter)
              AND (:rowStatus IS NULL OR u.rowStatus = :rowStatus)
              AND (:search IS NULL OR LOWER(u.userName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.userEmail) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(u.companyName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(COALESCE(u.taxIdNumber, '')) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY u.userName ASC
            """)
    Page<Users> searchUsersAdmin(
            @Param("allowedRoles") List<UserRole> allowedRoles,
            @Param("roleFilter") UserRole roleFilter,
            @Param("rowStatus") RowStatus rowStatus,
            @Param("search") String search,
            Pageable pageable
    );
}
