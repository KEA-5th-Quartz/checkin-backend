package com.quartz.checkin.util;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Map;
import org.hamcrest.Matcher;
import org.springframework.test.web.servlet.ResultMatcher;
public class ApiResponseMatchers {

    public static ResultMatcher apiResponse(int expectedStatus,
                                            Map<String, Matcher<?>> expectedDataFields) {
        return result -> {
            jsonPath("$.status", is(expectedStatus)).match(result);

            if (expectedDataFields != null && !expectedDataFields.isEmpty()) {
                expectedDataFields.forEach((key, matcher) -> {
                    try {
                        jsonPath("$.data." + key, matcher).match(result);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        };
    }

}
