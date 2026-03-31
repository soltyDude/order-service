package com.example.orderservice.infrastructure.persistence.repository;

import com.example.orderservice.infrastructure.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findTop50ByProcessedAtIsNullOrderByCreatedAtAsc();
}