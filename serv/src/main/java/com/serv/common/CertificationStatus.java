package com.serv.common;

public enum CertificationStatus {
    NOT_CERTIFIED,
    PENDING_PHOTO,      // The worker started a request and must upload the photo
    PENDING_APPROVAL,   // Photo sent, waiting for an admin
    NEEDS_REVISION,     // The admin asks for a new photo
    CERTIFIED,           // Approved and certified (until it expires)
    REJECTED            // Definitively rejected
}