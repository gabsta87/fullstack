package com.serv.dto;

import java.util.UUID;

public record CertificationRevocationDTO(UUID workerId, String reason) { }
