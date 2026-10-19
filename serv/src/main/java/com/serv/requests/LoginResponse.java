package com.serv.requests;

import com.serv.dto.VenusUserDTO;

public record LoginResponse(String token, VenusUserDTO user) {
}
