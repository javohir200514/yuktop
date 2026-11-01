package uz.onetimecourier.one_time_courier.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
public class ReviewEntity extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "delivery_id", nullable = false)
    private DeliveryEntity delivery;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "target_id", nullable = false)
    private UserEntity target;
    @Column(nullable = false) private int rating;
    @Column(length = 500) private String comment;
}