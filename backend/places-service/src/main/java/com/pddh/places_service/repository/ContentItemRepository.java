package com.pddh.places_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pddh.places_service.entity.ContentItem;

public interface ContentItemRepository extends JpaRepository<ContentItem, String> {
    List<ContentItem> findAllByTypeOrderByIdAsc(String type);
    Optional<ContentItem> findByIdAndType(String id, String type);
}
