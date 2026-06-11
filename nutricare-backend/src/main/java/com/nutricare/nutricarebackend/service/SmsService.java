package com.nutricare.nutricarebackend.service;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsService {

    @Value("${twilio.account.sid:${twilio.account-sid:${TWILIO_ACCOUNT_SID:}}}")
    private String accountSid;

    @Value("${twilio.auth.token:${twilio.auth-token:${TWILIO_AUTH_TOKEN:}}}")
    private String authToken;

    @Value("${twilio.phone.number:${twilio.sms-from:${TWILIO_PHONE_NUMBER:${TWILIO_SMS_FROM:}}}}")
    private String fromPhoneNumber;

    @Value("${twilio.enabled:${TWILIO_ENABLED:false}}")
    private boolean twilioEnabled;

    public void sendSms(String toPhoneNumber, String message) {
        if (!twilioEnabled) {
            log.info("SMS skipped because Twilio is disabled");
            return;
        }
        if (!hasText(toPhoneNumber)) {
            log.info("SMS skipped because recipient phone number is missing");
            return;
        }
        if (!isConfigured()) {
            log.warn("SMS skipped because Twilio SMS configuration is incomplete");
            return;
        }

        try {
            Twilio.init(accountSid.trim(), authToken.trim());
            Message sentMessage = Message.creator(
                    new PhoneNumber(normalizePhone(toPhoneNumber)),
                    new PhoneNumber(normalizePhone(fromPhoneNumber)),
                    message
            ).create();
            log.info("SMS sent successfully to={}, sid={}", maskPhone(toPhoneNumber), sentMessage.getSid());
        } catch (ApiException ex) {
            log.warn("SMS delivery failed to={}, status={}, code={}", maskPhone(toPhoneNumber), ex.getStatusCode(), ex.getCode());
        } catch (RuntimeException ex) {
            log.warn("SMS delivery failed to={}, error={}", maskPhone(toPhoneNumber), ex.getClass().getSimpleName());
        }
    }

    private boolean isConfigured() {
        return hasText(accountSid) && hasText(authToken) && hasText(fromPhoneNumber);
    }

    private String normalizePhone(String phone) {
        return phone.trim().replace(" ", "");
    }

    private String maskPhone(String phone) {
        if (!hasText(phone)) {
            return "-";
        }
        String normalized = normalizePhone(phone);
        if (normalized.length() <= 4) {
            return "****";
        }
        return "****" + normalized.substring(normalized.length() - 4);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
