package com.serv.requests;

public record AccountData(
        String username,
        String email,
        String password,
        String oldPassword
) {}
