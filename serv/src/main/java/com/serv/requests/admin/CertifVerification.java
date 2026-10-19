package com.serv.requests.admin;

public record CertifVerification(String workerId, boolean approved, String rejectionReason) {
}
