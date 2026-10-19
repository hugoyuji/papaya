package br.com.papaya.dto.response;

import br.com.papaya.enums.ModerationAction;

import java.time.Instant;
import java.util.Map;

public record ProductModerationEventResponse(
        Long id,
        Long productId,
        ModerationAction action,
        Long actorUserId,
        String actorDisplayName,
        String reason,
        Map<String, Object> productSnapshot,
        Instant occurredAt
) {
}
