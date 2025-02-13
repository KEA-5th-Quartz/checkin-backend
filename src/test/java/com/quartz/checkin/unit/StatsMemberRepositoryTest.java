package com.quartz.checkin.unit;

import com.quartz.checkin.repository.StatsMemberRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DataJpaTest
class StatsMemberRepositoryTest {

    @Autowired
    private StatsMemberRepository statsMemberRepository;

    @Autowired
    private EntityManager entityManager;

    // 테스트 데이터 초기화 메서드
    private void initTestData() {
        // Member, Category, Ticket 엔티티 생성 및 저장
        // 관계 설정 후 persist
    }

//    @Test
//    void findStatsByCategory_shouldReturnCorrectStats() {
//        // Given
//        initTestData(); // 테스트 데이터 초기화
//
//        // When
//        List<Map<String, Object>> result = statsMemberRepository.findStatsByCategory();
//
//        // Then
//        assertThat(result).hasSize(1);
//
//        Map<String, Object> firstResult = result.get(0);
//        assertThat(firstResult.get("username")).isEqualTo("testUser");
//
//        String stateJson = (String) firstResult.get("state");
//        assertThat(stateJson).contains("categoryName", "ticketCount");
//    }
//
//    @Test
//    void findStatTotalProgress_shouldReturnStatusCounts() {
//        // Given
//        initTestData();
//
//        // When
//        List<Object[]> result = statsMemberRepository.findStatTotalProgress();
//
//        // Then
//        assertThat(result).hasSize(1);
//        Object[] firstResult = result.get(0);
//        assertThat(firstResult[0]).isInstanceOf(Long.class); // overdue count
//        assertThat((String) firstResult[1]).contains("status", "ticketCount");
//    }
}