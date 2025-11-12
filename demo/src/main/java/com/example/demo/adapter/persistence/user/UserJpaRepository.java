// adapter/persistence/user/UserJpaRepository.java
package com.example.demo.adapter.persistence.user;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    // Get role ids for a user (handy for updates/deletes)
  @Query(
      value = "select ur.role_id from public.user_roles ur where ur.user_id = :userId",
      nativeQuery = true
  )
  Set<UUID> findRoleIdsByUserId(@Param("userId") UUID userId);

    // Get role keys (e.g., SUPER_ADMIN, EDITOR, CONTRIBUTOR, VIEWER) for Security
  @Query(
      value = """
        select r.role_key
        from public.user_roles ur
        join public.roles r on r.id = ur.role_id
        where ur.user_id = :userId
      """,
      nativeQuery = true
  )
  Collection<String> findRoleKeysByUserId(@Param("userId") UUID userId);
  
  // --- exists (already working) ---
  @Query(value = "select exists (select 1 from public.users where email = :email)", nativeQuery = true)
  boolean existsByEmail(@Param("email") String email);

  @Query(value = "select exists (select 1 from public.users where username = :username)", nativeQuery = true)
  boolean existsByUsername(@Param("username") String username);

  // --- finders for login (email or username) ---
  @Query(value = "select * from public.users where email = :email limit 1", nativeQuery = true)
  Optional<UserEntity> findByEmail(@Param("email") String email);

  @Query(value = "select * from public.users where username = :username limit 1", nativeQuery = true)
  Optional<UserEntity> findByUsername(@Param("username") String username);
  // --- listing (your existing pagination) ---
  @Query(
      value = """
        select
          u.id,
          u.fullname,
          u.email::text    as email,
          u.username::text as username,
          u.password_hash,
          u.is_email_verified,
          u.blocked_until,
          u.created_at,
          u.updated_at
        from public.users u
        where (:emailLike    is null or u.email::text    ilike concat('%', :emailLike, '%'))
          and (:usernameLike is null or u.username::text ilike concat('%', :usernameLike, '%'))
        order by u.created_at desc
      """,
      countQuery = """
        select count(*)
        from public.users u
        where (:emailLike    is null or u.email::text    ilike concat('%', :emailLike, '%'))
          and (:usernameLike is null or u.username::text ilike concat('%', :usernameLike, '%'))
      """,
      nativeQuery = true
  )
  Page<UserEntity> findFiltered(
      @Param("emailLike") String emailLike,
      @Param("usernameLike") String usernameLike,
      Pageable pageable
  );
}