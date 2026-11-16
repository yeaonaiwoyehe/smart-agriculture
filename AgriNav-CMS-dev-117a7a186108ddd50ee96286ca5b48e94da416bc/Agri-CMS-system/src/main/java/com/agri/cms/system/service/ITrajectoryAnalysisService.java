package com.agri.cms.system.service;

import com.agri.cms.system.domain.dto.TrajectoryAnalysisDTO;
import com.agri.cms.system.domain.dto.TrajectoryQualityDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
public interface ITrajectoryAnalysisService {
    TrajectoryQualityDTO analyzeTrajectoryQuality(Long plotId);

    TrajectoryAnalysisDTO analyzeTrajectoryByTimeRange(LocalDateTime startTime, LocalDateTime endTime);
}
