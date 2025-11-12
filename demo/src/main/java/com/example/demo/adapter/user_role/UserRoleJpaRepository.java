package com.example.demo.adapter.user_role;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleJpaRepository extends JpaRepository<UserRoleEntity, UserRoleId> {

  @Modifying
  @Query(value = "delete from public.user_roles where user_id = :userId", nativeQuery = true)
  void deleteByUserId(@Param("userId") UUID userId);

  @Query("SELECT CAST(ur.id.roleId AS string) FROM UserRoleEntity ur WHERE ur.id.userId = :userId")
  Set<String> findRoleIdsByUserId(@Param("userId") UUID userId);

  @Query("SELECT r.roleKey FROM UserRoleEntity ur JOIN RoleEntity r ON ur.id.roleId = r.id WHERE ur.id.userId = :userId")
  Set<String> findRoleKeysByUserId(@Param("userId") UUID userId);
  
}
