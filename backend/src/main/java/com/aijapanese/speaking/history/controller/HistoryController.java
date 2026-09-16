package com.aijapanese.speaking.history.controller;

import com.aijapanese.speaking.history.dto.HistoryDetailResponse;
import com.aijapanese.speaking.history.dto.HistoryListResponse;
import com.aijapanese.speaking.history.service.HistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인한 LEARNER 본인의 학습 기록(My History) 조회 전용 API.
 * AI를 호출하지 않으며, 이미 저장된 데이터만 읽는다.
 */
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public ResponseEntity<HistoryListResponse> getHistory(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(historyService.getHistory(userId));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<HistoryDetailResponse> getHistoryDetail(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(historyService.getHistoryDetail(sessionId, userId));
    }
}
