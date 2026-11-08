package com.example.auction.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;

@Service
public class ImageStorageService {
    private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);
    private final Path directory;
    public ImageStorageService(@Value("${file.upload-dir}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image limitée à 5 Mo");
        }
        BufferedImage image;
        try (InputStream input = file.getInputStream()) {
            image = decode(input);
        } catch (IOException ex) {
            throw storageFailure(ex);
        }

        Path path = null;
        try {
            Files.createDirectories(directory);
            path = Files.createTempFile(directory, "product-", ".png");
            try (OutputStream output = Files.newOutputStream(path)) {
                if (!ImageIO.write(image, "png", output)) throw new IOException("Encodeur PNG indisponible");
            }
            deleteOnRollback(path);
            return path.getFileName().toString();
        } catch (IOException ex) {
            if (path != null) delete(path);
            throw storageFailure(ex);
        }
    }

    private BufferedImage decode(InputStream input) {
        try (var stream = new MemoryCacheImageInputStream(input)) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw invalid();
            var reader = readers.next();
            try {
                reader.setInput(stream);
                String format = reader.getFormatName();
                if (!(format.equalsIgnoreCase("png") || format.equalsIgnoreCase("jpeg"))
                    || (long) reader.getWidth(0) * reader.getHeight(0) > 20_000_000) throw invalid();
                return reader.read(0);
            } finally { reader.dispose(); }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image illisible");
        }
    }

    private void deleteOnRollback(Path path) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) delete(path);
                }
            });
        }
    }

    private void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("Impossible de supprimer l'image {}", path.getFileName(), ex);
        }
    }

    private ResponseStatusException storageFailure(IOException ex) {
        log.error("Stockage des images indisponible", ex);
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stockage des images indisponible");
    }
    private ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image PNG ou JPEG valide requise (20 mégapixels maximum)");
    }
}
