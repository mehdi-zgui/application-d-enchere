package com.example.auction.repository;

import com.example.auction.model.Product;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    java.util.Optional<Product> findLockedById(@Param("id") Long id);

    // Recherche combinée titre partiel + catégorie exacte
    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%',:keyword,'%'))) AND " +
           "(:category IS NULL OR LOWER(p.category)=LOWER(:category))")
    List<Product> search(@Param("keyword") String keyword,
                         @Param("category") String category);

    @org.springframework.transaction.annotation.Transactional
    long deleteByEndDateBefore(Instant cutoff);
}
