package uz.onetimecourier.one_time_courier.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dispute_evidence")
@Getter
@Setter
@NoArgsConstructor
public class DisputeEvidenceEntity extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "dispute_id", nullable = false)
    private DisputeEntity dispute;
    @Column(name = "file_url", nullable = false, length = 500) private String fileUrl;
    @Column(length = 300) private String note;
}