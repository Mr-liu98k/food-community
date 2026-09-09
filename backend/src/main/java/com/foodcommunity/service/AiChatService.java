package com.foodcommunity.service;

import com.foodcommunity.common.BusinessException;
import com.foodcommunity.dto.AiChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 美食专家（基于 Spring AI + 阿里云百炼 OpenAI 兼容端点）。
 *
 * 无状态多轮：前端携带最近对话历史，服务端注入"美食专家"人设后整体发给模型，
 * 回答以 SSE 逐字流式返回。未配置 DASHSCOPE_API_KEY 时后端照常启动，
 * 仅调用此接口时给出友好提示。
 */
@Service
@RequiredArgsConstructor
public class AiChatService {

    private static final int MAX_HISTORY = 10;

    private final ObjectProvider<ChatModel> chatModelProvider;

    public Flux<String> chat(AiChatRequest req) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            throw new BusinessException("AI 服务尚未配置：请在 backend/.env 中设置 DASHSCOPE_API_KEY 后重启");
        }
        return chatModel.stream(new Prompt(buildMessages(req)))
                .map(resp -> resp.getResult().getOutput().getText())
                .onErrorResume(e -> Flux.just("\n\n⚠️ 回答中断：" + safeMessage(e)));
    }

    private List<Message> buildMessages(AiChatRequest req) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));

        // 只取最近 MAX_HISTORY 条，防御超长
        int from = Math.max(0, req.getMessages().size() - MAX_HISTORY);
        for (AiChatRequest.AiMessage msg : req.getMessages().subList(from, req.getMessages().size())) {
            String content = msg.getContent();
            if (content == null || content.isBlank()) {
                continue;
            }
            messages.add("user".equals(msg.getRole())
                    ? new UserMessage(content)
                    : new AssistantMessage(content));
        }
        return messages;
    }

    private String safeMessage(Throwable e) {
        String msg = e.getMessage();
        return msg == null || msg.isBlank() ? "服务暂时不可用，请稍后再试" : msg;
    }

    private static final String SYSTEM_PROMPT = """
            你是"美食社区"的 AI 美食专家，一位热心、专业的中华料理与全球美食顾问。
            你可以解答：菜谱做法与改进、食材挑选与保存、火候与调味技巧、探店选店建议、饮食营养搭配等。

            回答要求：
            1. 语气亲切自然，像一位懂吃会做的老友；
            2. 涉及具体做法时分条列出步骤，必要时标注用量与火候；
            3. 不确定的信息（如价格、门店营业状态）要如实说明需要以实际为准，不要编造；
            4. 回答保持简洁实用，一般不超过 400 字，除非用户明确要求详细展开。
            """;
}
