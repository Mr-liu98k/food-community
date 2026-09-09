package com.foodcommunity.service;

import com.foodcommunity.common.BusinessException;
import com.foodcommunity.common.PageResult;
import com.foodcommunity.dto.CommentRequest;
import com.foodcommunity.entity.Comment;
import com.foodcommunity.entity.Recipe;
import com.foodcommunity.mapper.CommentMapper;
import com.foodcommunity.mapper.RecipeMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评论模块（楼中楼）：
 * - 发评论时 parentId 为空=根评论；否则服务端推导 rootId/replyUserId（防篡改）
 * - 根评论分页，每条根评论携带其全部子评论
 * - 删除评论递归清理其下所有子孙
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final RecipeMapper recipeMapper;

    /** 某菜谱的评论：根评论分页，每条带 children */
    public PageResult<Comment> page(Long recipeId, int page, int size) {
        PageHelper.startPage(page, size);
        List<Comment> roots = commentMapper.selectRootPage(recipeId);
        PageInfo<Comment> pageInfo = new PageInfo<>(roots);

        if (!roots.isEmpty()) {
            List<Long> rootIds = roots.stream().map(Comment::getId).toList();
            Map<Long, List<Comment>> grouped = new HashMap<>();
            for (Comment child : commentMapper.selectChildrenByRoots(rootIds)) {
                grouped.computeIfAbsent(child.getRootId(), k -> new ArrayList<>()).add(child);
            }
            roots.forEach(root -> root.setChildren(grouped.getOrDefault(root.getId(), List.of())));
        }
        return PageResult.of(pageInfo);
    }

    public void create(Long userId, Long recipeId, CommentRequest req) {
        Recipe recipe = recipeMapper.selectDetail(recipeId);
        if (recipe == null || recipe.getStatus() != 1) {
            throw new BusinessException("菜谱不存在或尚未通过审核");
        }
        Comment comment = new Comment();
        comment.setRecipeId(recipeId);
        comment.setUserId(userId);
        comment.setContent(req.getContent().trim());

        if (req.getParentId() == null || req.getParentId() == 0) {
            // 根评论
            comment.setParentId(0L);
            comment.setRootId(0L);
        } else {
            // 楼中楼：parentId/rootId/replyUserId 一律由服务端推导
            Comment parent = commentMapper.findById(req.getParentId());
            if (parent == null || !parent.getRecipeId().equals(recipeId)) {
                throw new BusinessException("回复的评论不存在");
            }
            comment.setParentId(parent.getId());
            comment.setRootId(parent.getRootId() == 0 ? parent.getId() : parent.getRootId());
            comment.setReplyUserId(parent.getUserId());
        }
        commentMapper.insert(comment);
    }

    /** 删除自己的评论（递归清理子孙） */
    public void deleteOwn(Long userId, Long id) {
        Comment comment = requireComment(id);
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("无权删除他人的评论");
        }
        deleteRecursively(id);
    }

    public void adminDelete(Long id) {
        requireComment(id);
        deleteRecursively(id);
    }

    public PageResult<Comment> myPage(Long userId, int page, int size) {
        PageHelper.startPage(page, size);
        PageInfo<Comment> pageInfo = new PageInfo<>(commentMapper.selectMyPage(userId));
        return PageResult.of(pageInfo);
    }

    public PageResult<Comment> adminPage(int page, int size, String keyword) {
        PageHelper.startPage(page, size);
        PageInfo<Comment> pageInfo = new PageInfo<>(
                commentMapper.selectAdminPage(keyword == null || keyword.isBlank() ? null : keyword.trim()));
        return PageResult.of(pageInfo);
    }

    /** 递归收集 id 与其全部子孙并删除 */
    private void deleteRecursively(Long id) {
        List<Long> ids = new ArrayList<>();
        ids.add(id);
        int from = 0;
        while (from < ids.size()) {
            int to = ids.size();
            for (int i = from; i < to; i++) {
                ids.addAll(commentMapper.selectChildIds(ids.get(i)));
            }
            from = to;
        }
        commentMapper.deleteByIds(ids);
    }

    private Comment requireComment(Long id) {
        Comment comment = commentMapper.findById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        return comment;
    }
}
