package com.foodcommunity.controller;

import com.foodcommunity.common.Result;
import com.foodcommunity.dto.ExpertVO;
import com.foodcommunity.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 探店达人榜（公开）：仅 is_expert=1 的用户，
 * 热度 = SUM(审核通过探店浏览量) * 2，降序。
 */
@RestController
@RequestMapping("/api/expert")
@RequiredArgsConstructor
public class ExpertController {

    private final ReservationService reservationService;

    @GetMapping("/list")
    public Result<List<ExpertVO>> list(@RequestParam(required = false) Integer limit) {
        return Result.ok(reservationService.expertRank(limit));
    }
}
