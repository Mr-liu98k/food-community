package com.foodcommunity.service;

import com.foodcommunity.common.BusinessException;
import com.foodcommunity.common.PageResult;
import com.foodcommunity.common.UserContext;
import com.foodcommunity.dto.ExpertVO;
import com.foodcommunity.dto.ReservationCreateRequest;
import com.foodcommunity.entity.Reservation;
import com.foodcommunity.entity.User;
import com.foodcommunity.mapper.ExpertMapper;
import com.foodcommunity.mapper.ReservationMapper;
import com.foodcommunity.mapper.UserMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 探店达人 + 探店预约模块。
 * 预约双主体：同一条记录对发起人（initiator_id）与被预约达人（expert_id）各是一种视角；
 * 同意/拒绝仅被预约达人可操作（拒绝理由必填）；管理员只读维护。
 */
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationMapper reservationMapper;
    private final ExpertMapper expertMapper;
    private final UserMapper userMapper;

    // ---- 达人榜 ----

    public List<ExpertVO> expertRank(Integer limit) {
        return expertMapper.selectRank(limit);
    }

    // ---- 发起预约 ----

    public Long create(Long initiatorId, ReservationCreateRequest req) {
        if (expertMapper.countExpert(req.getExpertId()) == 0) {
            throw new BusinessException("预约对象不是探店达人");
        }
        if (initiatorId.equals(req.getExpertId())) {
            throw new BusinessException("不能预约自己");
        }
        Reservation rv = new Reservation();
        rv.setInitiatorId(initiatorId);
        rv.setExpertId(req.getExpertId());
        rv.setStoreName(req.getStoreName().trim());
        rv.setStoreIntro(req.getStoreIntro());
        rv.setStoreAddress(req.getStoreAddress());
        rv.setPhone(req.getPhone());
        rv.setRemark(req.getRemark());
        reservationMapper.insert(rv);
        return rv.getId();
    }

    /** 我的预约：direction = initiated | received */
    public PageResult<Reservation> myList(Long userId, String direction, int page, int size) {
        Long initiatorId = null;
        Long expertId = null;
        if ("received".equalsIgnoreCase(direction)) {
            expertId = userId;
        } else {
            initiatorId = userId; // 默认 initiated
        }
        PageHelper.startPage(page, size);
        PageInfo<Reservation> pageInfo = new PageInfo<>(reservationMapper.selectMyList(initiatorId, expertId));
        return PageResult.of(pageInfo);
    }

    // ---- 达人处理 ----

    /** 同意：仅被预约达人本人可操作，且只能处理待审核记录 */
    public void agree(Long userId, Long id) {
        Reservation rv = requireOwnReceived(userId, id);
        if (rv.getStatus() != 0) {
            throw new BusinessException("该预约已处理，请勿重复操作");
        }
        reservationMapper.updateStatus(id, 1, null);
    }

    /** 拒绝：理由必填 */
    public void reject(Long userId, Long id, String reason) {
        Reservation rv = requireOwnReceived(userId, id);
        if (rv.getStatus() != 0) {
            throw new BusinessException("该预约已处理，请勿重复操作");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("拒绝时必须填写拒绝理由");
        }
        reservationMapper.updateStatus(id, 2, reason.trim());
    }

    // ---- 管理端 ----

    public PageResult<Reservation> adminPage(int page, int size, String keyword) {
        PageHelper.startPage(page, size);
        PageInfo<Reservation> pageInfo = new PageInfo<>(
                reservationMapper.selectAdminPage(keyword == null || keyword.isBlank() ? null : keyword.trim()));
        return PageResult.of(pageInfo);
    }

    public void adminDelete(Long id) {
        requireReservation(id);
        reservationMapper.deleteById(id);
    }

    // ---- 私有 ----

    private Reservation requireOwnReceived(Long userId, Long id) {
        Reservation rv = requireReservation(id);
        if (!rv.getExpertId().equals(userId)) {
            throw new BusinessException("只有被预约的达人才可以处理该预约");
        }
        return rv;
    }

    private Reservation requireReservation(Long id) {
        Reservation rv = reservationMapper.selectDetail(id);
        if (rv == null) {
            throw new BusinessException("预约记录不存在");
        }
        return rv;
    }
}
