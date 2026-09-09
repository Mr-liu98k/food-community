package com.foodcommunity.controller;

import com.foodcommunity.common.PageResult;
import com.foodcommunity.common.Result;
import com.foodcommunity.common.UserContext;
import com.foodcommunity.dto.RejectRequest;
import com.foodcommunity.dto.ReservationCreateRequest;
import com.foodcommunity.entity.Reservation;
import com.foodcommunity.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 探店预约（用户端）：
 * - POST /api/reservation 发起预约（达人主页/榜单的"预约"按钮）
 * - /api/my/reservations 双视角：direction=initiated | received
 * - 同意/拒绝：仅"我接收的"里被预约的达人对待处理记录操作
 */
@RestController
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/api/reservation")
    public Result<Long> create(@Valid @RequestBody ReservationCreateRequest req) {
        return Result.ok(reservationService.create(UserContext.id(), req));
    }

    @GetMapping("/api/my/reservations")
    public Result<PageResult<Reservation>> myList(@RequestParam(defaultValue = "initiated") String direction,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return Result.ok(reservationService.myList(UserContext.id(), direction, page, size));
    }

    /** 达人同意预约 */
    @PostMapping("/api/my/reservations/{id}/agree")
    public Result<Void> agree(@PathVariable Long id) {
        reservationService.agree(UserContext.id(), id);
        return Result.ok();
    }

    /** 达人拒绝预约（理由必填） */
    @PostMapping("/api/my/reservations/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody RejectRequest req) {
        reservationService.reject(UserContext.id(), id, req.getReason());
        return Result.ok();
    }
}
