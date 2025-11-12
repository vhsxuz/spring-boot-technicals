package com.example.demo.adapter.persistence.role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, UUID> {

  // keep the id confidential: fetch by key when you need a default
  @Query(value = "select id from public.roles where role_key = :key limit 1", nativeQuery = true)
  Optional<UUID> findIdByRoleKey(@Param("key") String key);

  // list roles of a user (id, role_key, name)
  @Query(value = """
      select r.*
      from public.user_roles ur
      join public.roles r on r.id = ur.role_id
      where ur.user_id = :userId
      order by r.role_key
      """, nativeQuery = true)
  List<RoleEntity> findByUserId(@Param("userId") UUID userId);
}
