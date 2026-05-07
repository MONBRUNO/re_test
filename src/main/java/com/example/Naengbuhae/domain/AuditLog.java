package com.example.Naengbuhae.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String adminName;    // 행위를 한 관리자 ID
    private String action;       // 행위 종류 (예: DELETE_RECIPE, BAN_USER)
    private String targetType;   // 대상 도메인 (RECIPE, USER 등)
    private Long targetId;       // 대상의 PK ID

    @Column(length = 1000)
    private String description;  // 상세 내용 (예: "부적절한 이미지 포함으로 삭제")

    private LocalDateTime timestamp;
    private String ipAddress;    // (선택) 보안을 위해 접속 IP까지 기록하면 베스트!

    public AuditLog(String adminName, String action, String targetType, Long targetId, String description, String ipAddress) {
        this.adminName = adminName;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.description = description;
        this.timestamp = LocalDateTime.now();
        this.ipAddress = ipAddress;
    }
}