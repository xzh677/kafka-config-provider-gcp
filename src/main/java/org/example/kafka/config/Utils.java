package org.example.kafka.config;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.BiFunction;

public final class Utils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Utils() {}

    public static byte[] fromBase64(String key) {
        return Base64.getDecoder().decode(key);
    }

    public static String toBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static void sleep(long sleepMs) {
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static <T> T fromJsonString(String str, Class<T> type) {
        try {
            return objectMapper.readValue(str, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse Json response", e);
        }
    }

    public static <T> T runWithRetry(int maxAttempts, long retryIntervalMs, BiFunction<Integer, String, Optional<T>> fun) {
        int counter = 1;
        String uniqueId = Utils.uuid();
        while (counter <= maxAttempts) {
            Optional<T> result = fun.apply(counter, uniqueId);
            if (result.isEmpty()) {
                counter++;
                sleep(retryIntervalMs);
                continue;
            }
            return result.get();
        }
        throw new RuntimeException("Attempt exhausted (" + counter + "/" + maxAttempts + ") on " + uniqueId);
    }

    public static String toCsv(Object[] rows, String csvSeparator, String csvQuote) {
        StringJoiner sj = new StringJoiner(csvSeparator);
        for (Object obj : rows) {
            String strValue = "";
            if (obj != null) {
                strValue = obj.toString();
            }
            if (strValue.contains(csvSeparator) || strValue.contains(csvQuote)) {
                strValue = "\"" + strValue + "\"";
            }
            sj.add(strValue);
        }
        return sj.toString();
    }
}
