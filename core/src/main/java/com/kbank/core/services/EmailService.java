package com.kbank.core.services;

import java.util.Map;

public interface EmailService {
    void sendEmail(String to, String subject, Map<String, String> placeholders, String templatePath);
}
