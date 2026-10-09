package com.serv.common;

public enum CertificationStatus {
    NOT_CERTIFIED,
    PENDING_PHOTO,      // L'utilisateur a cliqué, il doit uploader sa photo (Brun/Doré)
    PENDING_APPROVAL,   // La photo est envoyée, en attente des administrateurs (Gris)
    NEEDS_REVISION,     // L'admin demande une modification/nouvelle photo (Brun/Doré avec alerte)
    CERTIFIED,           // Validé et certifié (Vert / Jaune / Orange / Rouge selon l'expiration)
    REJECTED            // Refusé définitivement
}