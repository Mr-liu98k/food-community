package com.foodcommunity.service;

import cn.hutool.core.util.StrUtil;
import com.foodcommunity.common.BusinessException;
import com.foodcommunity.common.PageResult;
import com.foodcommunity.dto.AuditRequest;
import com.foodcommunity.dto.StoreVisitQuery;
import com.foodcommunity.dto.StoreVisitSaveRequest;
import com.foodcommunity.entity.StoreVisit;
import com.foodcommunity.mapper.StoreVisitMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 探店信息模块（与菜谱相同的审核状态机：0 待审 / 1 通过 / 2 拒绝）。
 */
@Service
@RequiredArgsConstructor
public class StoreVisitService {

    private final StoreVisitMapper storeVisitMapper;

    // ---- 我的探店 ----

    public PageResult<StoreVisit> myPage(Long userId, int page, int size, Integer status) {
        StoreVisitQuery query = new StoreVisitQuery();
        query.setUserId(userId);
        query.setStatus(status);
        return doPage(page, size, query);
    }

    /** 发布/编辑（编辑重置待审核） */
    public Long save(Long userId, Long id, StoreVisitSaveRequest req) {
        StoreVisit sv = new StoreVisit();
        sv.setUserId(userId);
        sv.setTitle(req.getTitle().trim());
        sv.setCover(req.getCover() == null ? "" : req.getCover());
        sv.setContent(req.getContent().trim());
        sv.setAddress(req.getAddress() == null ? "" : req.getAddress().trim());

        if (id == null) {
            sv.setStatus(0);
            storeVisitMapper.insert(sv);
        } else {
            StoreVisit exist = requireStoreVisit(id);
            if (!exist.getUserId().equals(userId)) {
                throw new BusinessException("无权操作他人的探店信息");
            }
            sv.setId(id);
            sv.setStatus(0);
            sv.setRejectReason(null);
            storeVisitMapper.updateContent(sv);
        }
        return sv.getId();
    }

    public void delete(Long userId, Long id) {
        StoreVisit exist = requireStoreVisit(id);
        if (!exist.getUserId().equals(userId)) {
            throw new BusinessException("无权操作他人的探店信息");
        }
        storeVisitMapper.deleteById(id);
    }

    /** 我的探店详情（编辑回填） */
    public StoreVisit myDetail(Long userId, Long id) {
        StoreVisit sv = requireStoreVisit(id);
        if (!sv.getUserId().equals(userId)) {
            throw new BusinessException("无权查看他人的探店信息");
        }
        return sv;
    }

    // ---- 前台 ----

    public PageResult<StoreVisit> publicPage(int page, int size, String keyword) {
        StoreVisitQuery query = new StoreVisitQuery();
        query.setStatus(1);
        query.setKeyword(StrUtil.blankToDefault(keyword, null));
        return doPage(page, size, query);
    }

    public List<StoreVisit> hot(int limit) {
        return storeVisitMapper.selectHot(limit);
    }

    /** 前台详情：仅已通过；浏览量+1 */
    public StoreVisit detail(Long id) {
        StoreVisit sv = requireStoreVisit(id);
        if (sv.getStatus() != 1) {
            throw new BusinessException("该探店信息不存在或尚未通过审核");
        }
        storeVisitMapper.updateViews(id);
        return sv;
    }

    /** 作者主页：其已通过探店分页 */
    public PageResult<StoreVisit> authorPage(Long authorId, int page, int size) {
        StoreVisitQuery query = new StoreVisitQuery();
        query.setUserId(authorId);
        query.setStatus(1);
        return doPage(page, size, query);
    }

    // ---- 管理端 ----

    public PageResult<StoreVisit> adminPage(int page, int size, String keyword, Integer status) {
        StoreVisitQuery query = new StoreVisitQuery();
        query.setKeyword(StrUtil.blankToDefault(keyword, null));
        query.setStatus(status);
        return doPage(page, size, query);
    }

    public void audit(Long id, AuditRequest req) {
        StoreVisit exist = requireStoreVisit(id);
        if (exist.getStatus() != 0) {
            throw new BusinessException("该探店信息不在待审核状态，请勿重复操作");
        }
        String reason = null;
        if (req.getStatus() == 2) {
            if (StrUtil.isBlank(req.getReason())) {
                throw new BusinessException("拒绝时必须填写拒绝理由");
            }
            reason = req.getReason().trim();
        }
        storeVisitMapper.updateAudit(id, req.getStatus(), reason);
    }

    public void adminDelete(Long id) {
        requireStoreVisit(id);
        storeVisitMapper.deleteById(id);
    }

    // ---- 私有 ----

    private PageResult<StoreVisit> doPage(int page, int size, StoreVisitQuery query) {
        PageHelper.startPage(page, size);
        PageInfo<StoreVisit> pageInfo = new PageInfo<>(storeVisitMapper.selectByQuery(query));
        return PageResult.of(pageInfo);
    }

    private StoreVisit requireStoreVisit(Long id) {
        StoreVisit sv = storeVisitMapper.selectDetail(id);
        if (sv == null) {
            throw new BusinessException("探店信息不存在");
        }
        return sv;
    }
}
