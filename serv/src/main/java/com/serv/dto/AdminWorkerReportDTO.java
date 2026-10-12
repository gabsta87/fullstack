package com.serv.dto;

import com.serv.database.entities.Report;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminWorkerReportDTO(
        UUID workerId,
        String workerUsername,
        String workerEmail,
        long reportCount,
        List<ReportDetailDTO> reports
) {
    public record ReportDetailDTO(
            Long reportId,
            String authorEmail,
            String reportStatus,
            String reason,
            LocalDateTime createdAt
    ) {
        public static ReportDetailDTO from(Report report) {
            return new ReportDetailDTO(
                    report.getId(),
                    report.getAuthor().getEmail().getValue(),
                    report.getStatus().toString(),
                    report.getReason(),
                    report.getCreatedAt()
            );
        }
    }
}
