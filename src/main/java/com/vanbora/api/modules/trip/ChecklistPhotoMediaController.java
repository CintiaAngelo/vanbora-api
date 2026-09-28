package com.vanbora.api.modules.trip;

import com.vanbora.api.modules.trip.domain.SafetyChecklistPhoto;
import com.vanbora.api.modules.trip.repository.SafetyChecklistPhotoRepository;
import com.vanbora.api.modules.trip.service.ChecklistPhotoUrlSigner;
import com.vanbora.api.shared.storage.StorageService;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entrega a foto do checklist por URL ASSINADA (liberada em SecurityConfig, como /uploads,
 * porque o componente de imagem não envia o JWT). Sem assinatura válida e não expirada,
 * responde 404 — sem revelar se a foto existe. Foto já removida pela retenção responde 410.
 */
@RestController
@RequestMapping("/api/media/checklist-photos")
public class ChecklistPhotoMediaController {

    private final SafetyChecklistPhotoRepository photoRepository;
    private final StorageService storageService;
    private final ChecklistPhotoUrlSigner urlSigner;

    public ChecklistPhotoMediaController(SafetyChecklistPhotoRepository photoRepository,
                                         StorageService storageService,
                                         ChecklistPhotoUrlSigner urlSigner) {
        this.photoRepository = photoRepository;
        this.storageService = storageService;
        this.urlSigner = urlSigner;
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> photo(@PathVariable Long id,
                                          @RequestParam long exp,
                                          @RequestParam String sig) {
        if (!urlSigner.isValid(id, exp, sig)) {
            return ResponseEntity.notFound().build();
        }
        SafetyChecklistPhoto photo = photoRepository.findById(id).orElse(null);
        if (photo == null) {
            return ResponseEntity.notFound().build();
        }
        Path file = photo.isAvailable() ? storageService.loadPrivate(photo.getStorageKey()) : null;
        if (file == null) {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }
        MediaType type = photo.getContentType() != null
                ? MediaType.parseMediaType(photo.getContentType())
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(type)
                // Imagem sensível: nada de cache compartilhado (proxy/CDN).
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(new FileSystemResource(file));
    }
}
