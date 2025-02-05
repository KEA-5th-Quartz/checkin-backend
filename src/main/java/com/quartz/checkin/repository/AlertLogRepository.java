package com.quartz.checkin.repository;

import com.quartz.checkin.entity.AlertLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {
}
