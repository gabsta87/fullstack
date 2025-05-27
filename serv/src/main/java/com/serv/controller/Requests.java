package com.serv.controller;

import lombok.Data;

public class Requests {

    @Data
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
    }

    @Data
    public static class LoginRequest {
        private String pseudo;
        private String password;
        private String redirectTo;
    }

    @Data
    public static class ForgotPasswordRequest {
        private String email;
    }
}
