package com.serv.requests.admin;

public record UpdateWorkerDays(String workerId, int newDaysValue, String reason) {
}
