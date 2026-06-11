package com.nutricare.nutricarebackend.mongo.document;

import com.nutricare.nutricarebackend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "report_snapshots")
@CompoundIndex(name = "idx_report_type_created", def = "{'reportType': 1, 'createdAt': -1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSnapshotDocument {

    @Id
    private String id;

    private String reportType;

    @Indexed
    private Long userId;

    @Indexed
    private Long dieticianId;

    @Indexed
    private Long generatedBy;

    private Role generatedByRole;
    private Object data;
    private LocalDateTime createdAt;
}
