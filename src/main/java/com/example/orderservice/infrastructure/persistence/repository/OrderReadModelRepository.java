package com.example.orderservice.infrastructure.persistence.repository;

import com.example.orderservice.infrastructure.persistence.entity.OrderReadModelEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface OrderReadModelRepository extends JpaRepository<OrderReadModelEntity, UUID> {

    @Query("""
            SELECT r FROM OrderReadModelEntity r
            WHERE r.userId = :userId
            AND (:status IS NULL OR r.status = :status)
            ORDER BY r.createdAt DESC
            """)
    Page<OrderReadModelEntity> findByUserIdAndOptionalStatus(UUID userId, String status, Pageable pageable);
}
