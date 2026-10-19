package com.serv.requests.admin;

public record UpdateWorkerStatus(
        Boolean locked,
        Boolean banned) {
}
