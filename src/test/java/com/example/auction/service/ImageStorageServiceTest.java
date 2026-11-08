package com.example.auction.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.TempDirFactory;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageStorageServiceTest {
    @TempDir(factory = StorageDirectory.class) Path directory;

    static class StorageDirectory implements TempDirFactory {
        @Override public Path createTempDirectory(AnnotatedElementContext element, ExtensionContext context)
                throws IOException {
            return Files.createTempDirectory(Path.of("target").toAbsolutePath(), "image-storage-");
        }
    }

    @Test void rollbackDeletesImageAndCommitKeepsIt() throws IOException {
        var transactions = new TransactionTemplate(new DataSourceTransactionManager(
            new DriverManagerDataSource("jdbc:h2:mem:storage", "sa", "")));
        var storage = new ImageStorageService(directory.toString());
        var image = png();
        String rolledBack = transactions.execute(status -> {
            String name = storage.store(image);
            assertThat(directory.resolve(name)).exists();
            status.setRollbackOnly();
            return name;
        });
        assertThat(directory.resolve(rolledBack)).doesNotExist();

        String committed = transactions.execute(status -> storage.store(image));
        assertThat(directory.resolve(committed)).isRegularFile();
    }

    @Test void unavailableStorageReturnsServerError() throws IOException {
        Path notDirectory = Files.createFile(directory.resolve("file"));
        var storage = new ImageStorageService(notDirectory.toString());
        var image = png();
        assertThatThrownBy(() -> storage.store(image))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    private MockMultipartFile png() throws IOException {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return new MockMultipartFile("imageFile", "sample.png", "image/png", bytes.toByteArray());
    }
}
