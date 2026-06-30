package com.geosun.rmpd.application.dto;

public record CmrPartySuggestionsDto(
        String extractedSenderName,
        String extractedReceiverName,
        PartySuggestionDto senderMatch,
        PartySuggestionDto receiverMatch) {}
