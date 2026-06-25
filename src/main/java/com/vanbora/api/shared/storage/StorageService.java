package com.vanbora.api.shared.storage;

import com.vanbora.api.shared.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Armazena imagens (fotos de perfil/ajudante) em disco e devolve a URL pública
 * servida estaticamente pelo Spring (ver WebConfig). Sem dependência de nuvem.
 */
@Service
public class StorageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB

    private final Path root;
    private final String publicPath;

    public StorageService(
            @Value("${vanbora.uploads.dir:uploads}") String dir,
            @Value("${vanbora.uploads.public-path:/uploads}") String publicPath) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        this.publicPath = publicPath;
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível criar o diretório de uploads: " + root, e);
        }
    }

    /**
     * Salva a imagem em {@code subfolder} e devolve a URL pública
     * (ex.: {@code /uploads/helpers/uuid.jpg}).
     */
    public String store(MultipartFile file, String subfolder) {
        validateImage(file);
        try {
            Path dir = root.resolve(subfolder).normalize();
            if (!dir.startsWith(root)) {
                throw new BusinessException("Destino de upload inválido.");
            }
            Files.createDirectories(dir);
            String filename = UUID.randomUUID() + extension(file);
            file.transferTo(dir.resolve(filename));
            return publicPath + "/" + subfolder + "/" + filename;
        } catch (IOException e) {
            throw new BusinessException("Falha ao salvar a imagem.");
        }
    }

    /** Remove o arquivo apontado por uma URL pública retornada por {@link #store} (best-effort). */
    public void delete(String publicUrl) {
        String prefix = publicPath + "/";
        if (publicUrl == null || !publicUrl.startsWith(prefix)) {
            return;
        }
        Path target = root.resolve(publicUrl.substring(prefix.length())).normalize();
        if (target.startsWith(root)) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // best-effort: um arquivo órfão não deve quebrar a operação principal.
            }
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Arquivo de imagem vazio.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("Envie um arquivo de imagem.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException("Imagem muito grande (máx. 5 MB).");
        }
    }

    private String extension(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return ".img";
        }
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/heic", "image/heif" -> ".heic";
            default -> ".img";
        };
    }
}
