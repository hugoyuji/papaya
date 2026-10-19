package br.com.papaya.model;

import br.com.papaya.enums.ModerationAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "product_moderation_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductModerationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_id_snapshot", nullable = false, updatable = false)
    private Long productIdSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @Column(name = "actor_user_id_snapshot", updatable = false)
    private Long actorUserIdSnapshot;

    @Column(name = "actor_display_name_snapshot", length = 100, updatable = false)
    private String actorDisplayNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private ModerationAction action;

    @Column(length = 1000, updatable = false)
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "product_snapshot", nullable = false, columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> productSnapshot;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    public ProductModerationEvent(Product product, User actor, ModerationAction action,
                                  String reason, Map<String, Object> productSnapshot) {
        this.product = product;
        this.productIdSnapshot = product.getId();
        this.actor = actor;
        this.actorUserIdSnapshot = actor == null ? null : actor.getId();
        this.actorDisplayNameSnapshot = actor == null ? null : actor.getDisplayName();
        this.action = action;
        this.reason = reason;
        this.productSnapshot = Map.copyOf(productSnapshot);
    }

    @PrePersist
    protected void onCreate() {
        this.occurredAt = Instant.now();
    }
}
