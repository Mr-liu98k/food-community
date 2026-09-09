package com.foodcommunity.service;

import com.foodcommunity.common.BusinessException;
import com.foodcommunity.common.PageResult;
import com.foodcommunity.dto.NoticeSaveRequest;
import com.foodcommunity.entity.Notice;
import com.foodcommunity.mapper.NoticeMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 公告模块：前台分页/最新/详情 + 管理端增删改查。
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeMapper noticeMapper;

    /** 前台/管理端共用分页（keyword 匹配标题） */
    public PageResult<Notice> page(int page, int size, String keyword) {
        PageHelper.startPage(page, size);
        PageInfo<Notice> pageInfo = new PageInfo<>(
                noticeMapper.selectPage(keyword == null || keyword.isBlank() ? null : keyword.trim()));
        return PageResult.of(pageInfo);
    }

    public List<Notice> latest(int limit) {
        return noticeMapper.selectLatest(limit);
    }

    public Notice getById(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException("公告不存在");
        }
        return notice;
    }

    public void create(NoticeSaveRequest req) {
        Notice notice = new Notice();
        notice.setTitle(req.getTitle().trim());
        notice.setContent(req.getContent());
        noticeMapper.insert(notice);
    }

    public void update(Long id, NoticeSaveRequest req) {
        requireNotice(id);
        Notice notice = new Notice();
        notice.setId(id);
        notice.setTitle(req.getTitle().trim());
        notice.setContent(req.getContent());
        noticeMapper.update(notice);
    }

    public void delete(Long id) {
        requireNotice(id);
        noticeMapper.deleteById(id);
    }

    private Notice requireNotice(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException("公告不存在");
        }
        return notice;
    }
}
