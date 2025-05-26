package com.serv.controller;

import lombok.Data;

public class Requests {

    @Data
    public class RegisterRequest {
        private String pseudo;
        private String email;
        private String password;
    }

    @Data
    public class LoginRequest {
        private String pseudo;
        private String password;
    }

    @Data
    public class ForgotPasswordRequest {
        private String email;
    }
}
