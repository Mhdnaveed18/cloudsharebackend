package com.cloudshareoriginal.utils;

public final class EmailUtils {
    private EmailUtils() {}

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
