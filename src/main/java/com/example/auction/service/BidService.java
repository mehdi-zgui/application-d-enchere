package com.example.auction.service;

import com.example.auction.model.Bid;
import com.example.auction.model.Product;
import com.example.auction.repository.BidRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BidService {
    private final BidRepository repo;
    public BidService(BidRepository repo) { this.repo = repo; }

    public Bid save(Bid b) { return repo.save(b); }
    public List<Bid> findByProduct(Product p) { return repo.findByProductOrderByAmountDesc(p); }
}

