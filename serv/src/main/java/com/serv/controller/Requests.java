package com.serv.controller;

import com.serv.dto.VenusUserDTO;

public class Requests {

    public record RegisterRequest(String email, String password) { }

    public record ForgotPasswordRequest (String email) { }

    public record LoginRequest(String email, String password) { }
    public record LoginResponse(String token, VenusUserDTO user) { }
}
