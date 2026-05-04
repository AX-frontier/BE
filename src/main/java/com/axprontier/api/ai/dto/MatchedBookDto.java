package com.axprontier.api.ai.dto;

public record MatchedBookDto(
        Long id,
        String bibNo,
        String regNo,
        String title,
        String author,
        String publisher,
        Integer publishYear,
        String holdingCallNo,
        String materialType,
        String locationSymbol,
        String stackLocation,
        String stackShelf
) {
}
