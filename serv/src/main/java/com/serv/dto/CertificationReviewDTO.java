package com.serv.dto;

public record CertificationReviewDTO (Long requestId, boolean approved, String comment) {
}