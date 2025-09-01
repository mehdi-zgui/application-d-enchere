package com.example.auction.service;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.auction.repository.ProductRepository;

@Component
public class AuctionPurgeService {

    private final ProductRepository productRepository;

    public AuctionPurgeService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void purgeExpiredAuctions() {
        Instant now = Instant.now();
        long deleted = productRepository.deleteByEndDateBefore(now);
        System.out.println("Purge enchères expirées (" + now + ") supprimées : " + deleted);
    }
}
