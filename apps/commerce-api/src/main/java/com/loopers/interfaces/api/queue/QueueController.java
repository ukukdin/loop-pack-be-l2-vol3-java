package com.loopers.interfaces.api.queue;

import com.loopers.application.queue.EnterQueueUseCase;
import com.loopers.application.queue.QueryPositionUseCase;
import com.loopers.domain.model.queue.QueuePosition;
import com.loopers.domain.model.user.UserId;
import com.loopers.interfaces.api.queue.dto.QueueEnterResponse;
import com.loopers.interfaces.api.queue.dto.QueuePositionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/queue")
public class QueueController {

    private final EnterQueueUseCase enterQueueUseCase;
    private final QueryPositionUseCase queryPositionUseCase;

    public QueueController(EnterQueueUseCase enterQueueUseCase,
                           QueryPositionUseCase queryPositionUseCase) {
        this.enterQueueUseCase = enterQueueUseCase;
        this.queryPositionUseCase = queryPositionUseCase;
    }

    @PostMapping("/enter")
    public ResponseEntity<QueueEnterResponse> enter(
            @RequestAttribute("authenticatedUserId") UserId userId) {
        EnterQueueUseCase.EnterQueueResult result = enterQueueUseCase.enter(userId);
        return ResponseEntity.ok(QueueEnterResponse.from(result));
    }

    @GetMapping("/position")
    public ResponseEntity<QueuePositionResponse> getPosition(
            @RequestAttribute("authenticatedUserId") UserId userId) {
        QueuePosition position = queryPositionUseCase.getPosition(userId);
        return ResponseEntity.ok(QueuePositionResponse.from(position));
    }
}
