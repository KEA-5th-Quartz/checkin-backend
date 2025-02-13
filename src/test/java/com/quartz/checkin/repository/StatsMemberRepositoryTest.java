package com.quartz.checkin.repository;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import com.quartz.checkin.dto.stat.response.StatCategoryRateResponse;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class StatsMemberRepositoryTest {

    @Autowired
    private StatsRepository statsRepository;

    @Autowired
    private EntityManager entityManager;

    // 테스트 데이터 초기화 메서드
    private void initTestData() {
        // Member, Category, Ticket 엔티티 생성 및 저장
        // 관계 설정 후 persist
    }

    @Test
    void findStatsByCategory_shouldReturnCorrectStats() {
        // Given
        initTestData(); // 테스트 데이터 초기화

        // When
        List<StatCategoryRateResponse> result = statsRepository.findStatsByCategory();

        // Then
        assertThat(result).hasSize(1);

        Map<String, Object> firstResult = (Map<String, Object>) result.get(0);
        assertThat(firstResult.get("username")).isEqualTo("testUser");

        String stateJson = (String) firstResult.get("state");
        assertThat(stateJson).contains("categoryName", "ticketCount");
    }

    @Test
    void findStatTotalProgress_shouldReturnStatusCounts() {
        // Given
        initTestData();

        // When
        List<Object[]> result = (List<Object[]>) statsRepository.findStatTotalProgress();

        // Then
        assertThat(result).hasSize(1);
        Object[] firstResult = result.get(0);
        assertThat(firstResult[0]).isInstanceOf(Long.class); // overdue count
        assertThat((String) firstResult[1]).contains("status", "ticketCount");
    }
}