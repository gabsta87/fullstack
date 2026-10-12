package com.serv.dto;

public record ReportDTO(Long id,String authorId, String workerId, String reason, String date, String status) {

    public static ReportDTO from(com.serv.database.entities.Report r){
        return new ReportDTO(
                r.getId(),
                r.getAuthor().getId().toString(),
                r.getWorker().getId().toString(),
                r.getReason(),
                r.getCreatedAt().toString(),
                r.getStatus().toString()
        );
    }
}
