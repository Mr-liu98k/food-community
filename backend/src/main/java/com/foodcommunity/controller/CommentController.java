package com.foodcommunity.controller;

import com.foodcommunity.common.PageResult;
import com.foodcommunity.common.Result;
import com.foodcommunity.common.UserContext;
import com.foodcommunity.dto.CommentRequest;
import com.foodcommunity.entity.Comment;
import com.foodcommunity.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论（用户端）：查看（公开）、发表、删除自己的评论、我的评论列表。
 */
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /** 菜谱评论：根评论分页（公开） */
    @GetMapping("/api/recipe/{recipeId}/comments")
    public Result<PageResult<Comment>> page(@PathVariable Long recipeId,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(commentService.page(recipeId, page, size));
    }

    /** 发表评论（登录）：content + parentId（空=根评论，回复楼中楼传 parentId） */
    @PostMapping("/api/recipe/{recipeId}/comment")
    public Result<Void> create(@PathVariable Long recipeId, @Valid @RequestBody CommentRequest req) {
        commentService.create(UserContext.id(), recipeId, req);
        return Result.ok();
    }

    /** 删除自己的评论（递归清理子孙） */
    @DeleteMapping("/api/comment/{id}")
    public Result<Void> deleteOwn(@PathVariable Long id) {
        commentService.deleteOwn(UserContext.id(), id);
        return Result.ok();
    }

    /** 我的评论（登录，联菜谱标题） */
    @GetMapping("/api/my/comments")
    public Result<PageResult<Comment>> myComments(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return Result.ok(commentService.myPage(UserContext.id(), page, size));
    }
}
