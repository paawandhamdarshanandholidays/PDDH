package com.pddh.places_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pddh.places_service.entity.ContentItem;

public interface ContentItemRepository extends JpaRepository<ContentItem, String> {
    List<ContentItem> findAllByTypeOrderByIdAsc(String type);
    @Query("select i.id as id, i.type as type, i.payload as payload from ContentItem i "
            + "where i.type = :type order by i.id asc")
    List<ContentItemSummary> findSummariesByType(@Param("type") String type);
    Optional<ContentItem> findByIdAndType(String id, String type);

    interface ContentItemSummary {
        String getId();
        String getType();
        String getPayload();
    }
}
