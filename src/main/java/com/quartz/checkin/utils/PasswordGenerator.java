package com.quartz.checkin.utils;

import java.security.SecureRandom;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PasswordGenerator {

    private static final String NUMBERS = "0123456789";
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String SPECIAL_CHARACTERS = "!@#&()–[{}]:;',?/*~$^+=<>";
    private static final String ALL_CHARACTERS = NUMBERS + LETTERS + SPECIAL_CHARACTERS;

    public static String generateRandomPassword() {

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        password.append(NUMBERS.charAt(random.nextInt(NUMBERS.length())));
        password.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        password.append(SPECIAL_CHARACTERS.charAt(random.nextInt(SPECIAL_CHARACTERS.length())));

        for (int i = 0; i < 5; i++) {
            password.append(ALL_CHARACTERS.charAt(random.nextInt(SPECIAL_CHARACTERS.length())));
        }

        log.info("랜덤 비밀번호을 생성합니다. {}", password);
        return password.toString();
    }
}
