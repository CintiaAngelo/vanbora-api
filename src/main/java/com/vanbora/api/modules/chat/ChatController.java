package com.vanbora.api.modules.chat;

import com.vanbora.api.modules.chat.dto.ConversationResponse;
import com.vanbora.api.modules.chat.dto.MessageResponse;
import com.vanbora.api.modules.chat.dto.SendMessageRequest;
import com.vanbora.api.modules.chat.service.ChatService;
import com.vanbora.api.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de conversas e mensagens (compartilhados pelos dois perfis). */
@RestController
@RequestMapping("/api/conversations")
public class ChatController {

    private final ChatService chatService;
    private final CurrentUserProvider currentUserProvider;

    public ChatController(ChatService chatService, CurrentUserProvider currentUserProvider) {
        this.chatService = chatService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public List<ConversationResponse> conversations() {
        return chatService.listConversations(currentUserProvider.requireUser());
    }

    @GetMapping("/{id}/messages")
    public List<MessageResponse> messages(@PathVariable Long id) {
        return chatService.listMessages(currentUserProvider.requireUser(), id);
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageResponse> send(@PathVariable Long id,
                                                @Valid @RequestBody SendMessageRequest request) {
        MessageResponse response =
                chatService.sendMessage(currentUserProvider.requireUser(), id, request.text());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
