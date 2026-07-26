package com.vanbora.api.modules.chat;

import com.vanbora.api.modules.chat.dto.ConversationResponse;
import com.vanbora.api.modules.chat.dto.MessageResponse;
import com.vanbora.api.modules.chat.dto.SendMessageRequest;
import com.vanbora.api.modules.chat.dto.StartConversationRequest;
import com.vanbora.api.modules.chat.service.ChatService;
import com.vanbora.api.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    /** [Responsável] Abre (ou reusa) a conversa com um transportador. */
    @PostMapping
    @PreAuthorize("hasRole('GUARDIAN')")
    public ResponseEntity<ConversationResponse> start(@Valid @RequestBody StartConversationRequest request) {
        ConversationResponse response = chatService.startConversation(
                currentUserProvider.requireUser(), request.transporterId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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

    /** Envia uma imagem na conversa. */
    @PostMapping("/{id}/messages/image")
    public ResponseEntity<MessageResponse> sendImage(@PathVariable Long id,
                                                     @RequestParam("file") MultipartFile file) {
        MessageResponse response =
                chatService.sendImage(currentUserProvider.requireUser(), id, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Marca a conversa como lida pelo usuário autenticado. */
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        chatService.markRead(currentUserProvider.requireUser(), id);
        return ResponseEntity.noContent().build();
    }
}
