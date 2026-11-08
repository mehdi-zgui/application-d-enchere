package com.example.auction.service;

import com.example.auction.dto.ApiDtos.BidView;
import com.example.auction.model.*;
import com.example.auction.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.time.Instant;
import java.util.List;

@Service
public class BidService {
    private final BidRepository bids;
    private final ProductRepository products;
    private final UserService users;
    public BidService(BidRepository bids, ProductRepository products, UserService users) {
        this.bids = bids; this.products = products; this.users = users;
    }
    @Transactional(readOnly=true)
    public List<BidView> list(Long id) {
        Product product = products.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable"));
        return bids.findByProductOrderByAmountDesc(product).stream().map(BidView::from).toList();
    }
    @Transactional
    public BidView place(Long id, String username) {
        Product product = products.findLockedById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable"));
        if (!product.getEndDate().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Enchère terminée");
        }
        User buyer = users.require(username);
        if (product.getSeller().getId().equals(buyer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Le vendeur ne peut pas enchérir sur son produit");
        }
        BigDecimal current = bids.findFirstByProductOrderByAmountDesc(product)
            .map(Bid::getAmount).orElse(product.getInitialPrice());
        BigDecimal next = current.multiply(new BigDecimal("1.10")).setScale(2, RoundingMode.UP);
        return BidView.from(bids.save(new Bid(buyer, product, next)));
    }
}
