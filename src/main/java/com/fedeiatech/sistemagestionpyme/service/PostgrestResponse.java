package com.fedeiatech.sistemagestionpyme.service;

public record PostgrestResponse(int statusCode, String body) {

    public boolean esExitosa() {
        return statusCode >= 200 && statusCode < 300;
    }
}
