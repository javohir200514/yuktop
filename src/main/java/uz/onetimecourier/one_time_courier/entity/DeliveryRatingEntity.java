package uz.onetimecourier.one_time_courier.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/** Yetkazilgan yuk bo'yicha bir tomonlama baho: jo'natuvchi kuryerni yoki kuryer jo'natuvchini baholaydi. */
@Entity
@Table(name = "delivery_ratings",
        uniqueConstraints = @UniqueConstraint(name = "uk_rating_delivery_rater",
                columnNames = {"delivery_id", "rater_id"}))
public class DeliveryRatingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    private DeliveryEntity delivery;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "rater_id", nullable = false)
    private UserEntity rater;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ratee_id", nullable = false)
    private UserEntity ratee;

    @Column(nullable = false)
    private int stars;

    @Column(length = 500)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected DeliveryRatingEntity() {
    }

    public DeliveryRatingEntity(DeliveryEntity delivery, UserEntity rater, UserEntity ratee,
                                int stars, String comment) {
        this.delivery = delivery;
        this.rater = rater;
        this.ratee = ratee;
        this.stars = stars;
        this.comment = comment;
    }

    public Long getId() { return id; }
    public DeliveryEntity getDelivery() { return delivery; }
    public UserEntity getRater() { return rater; }
    public UserEntity getRatee() { return ratee; }
    public int getStars() { return stars; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}