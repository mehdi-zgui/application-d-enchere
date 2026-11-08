package com.example.auction.controller;

import com.example.auction.dto.ApiDtos.BidView;
import com.example.auction.service.BidService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/bids")
public class BidController {
    private final BidService bids;
    public BidController(BidService bids) { this.bids = bids; }

    @GetMapping
    public List<BidView> list(@PathVariable Long productId) { return bids.list(productId); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BidView place(@PathVariable Long productId, Authentication auth) {
        return bids.place(productId, auth.getName());
    }
}
