package br.com.papaya.repository;

import br.com.papaya.model.ProductModerationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductModerationEventRepository extends JpaRepository<ProductModerationEvent, Long> {

    List<ProductModerationEvent> findAllByProductIdOrderByOccurredAtAscIdAsc(Long productId);
}
