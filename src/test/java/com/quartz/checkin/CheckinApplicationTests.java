package com.quartz.checkin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@ActiveProfiles(value = "test")
@Sql(scripts = "classpath:data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@SpringBootTest
class CheckinApplicationTests {

    @Test
    void contextLoads() {
    }

}
