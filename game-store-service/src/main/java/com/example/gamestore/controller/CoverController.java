package com.example.gamestore.controller;

import com.example.gamestore.model.User;
import com.example.gamestore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class CoverController {
    private static final long MAX_FILE_SIZE = 8L * 1024 * 1024;

    private final UserRepository userRepository;
    private final Path coverDirectory;

    public CoverController(
            UserRepository userRepository,
            @Value("${app.cover-dir:/data/uploads/covers}") String directory) {
        this.userRepository = userRepository;
        this.coverDirectory = Paths.get(directory).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.coverDirectory);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Не удалось создать каталог обложек профиля", e);
        }
    }

    @PostMapping(
            value = "/{userId}/cover",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadCover(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) {
        User user = findUser(userId);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Выбери изображение обложки");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Размер обложки не должен превышать 8 МБ");
        }

        try {
            byte[] bytes = file.getBytes();

            boolean png = bytes.length >= 8
                    && (bytes[0] & 0xff) == 0x89
                    && bytes[1] == 0x50
                    && bytes[2] == 0x4e
                    && bytes[3] == 0x47
                    && bytes[4] == 0x0d
                    && bytes[5] == 0x0a
                    && bytes[6] == 0x1a
                    && bytes[7] == 0x0a;

            boolean jpeg = bytes.length >= 3
                    && (bytes[0] & 0xff) == 0xff
                    && (bytes[1] & 0xff) == 0xd8
                    && (bytes[2] & 0xff) == 0xff;

            if (!png && !jpeg) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Поддерживаются только PNG и JPEG");
            }

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null
                    || image.getWidth() > 4096
                    || image.getHeight() > 4096
                    || (long) image.getWidth() * image.getHeight() > 16_000_000L) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Изображение повреждено или имеет слишком большие размеры");
            }

            String filename = UUID.randomUUID() + (png ? ".png" : ".jpg");
            Path target = coverDirectory.resolve(filename).normalize();

            if (!target.startsWith(coverDirectory)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Недопустимый путь файла");
            }

            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);

            String oldFilename = user.getCoverFilename();
            user.setCoverFilename(filename);

            try {
                userRepository.saveAndFlush(user);
            } catch (RuntimeException e) {
                Files.deleteIfExists(target);
                throw e;
            }

            deleteOldFile(oldFilename);

            return ResponseEntity.ok(Map.of(
                    "coverUrl", "/api/v1/users/" + userId + "/cover",
                    "message", "Обложка профиля сохранена"
            ));
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Не удалось обработать изображение обложки", e);
        }
    }

    @GetMapping("/{userId}/cover")
    public ResponseEntity<Resource> getCover(@PathVariable Long userId) {
        User user = findUser(userId);
        String filename = user.getCoverFilename();

        if (filename == null
                || !filename.matches("[0-9a-fA-F-]{36}\\.(png|jpg)")) {
            return ResponseEntity.notFound().build();
        }

        Path file = coverDirectory.resolve(filename).normalize();
        if (!file.startsWith(coverDirectory) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = filename.endsWith(".png")
                ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.noCache())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(file));
    }

    @DeleteMapping("/{userId}/cover")
    public ResponseEntity<Map<String, String>> deleteCover(
            @PathVariable Long userId) {
        User user = findUser(userId);
        String oldFilename = user.getCoverFilename();

        user.setCoverFilename(null);
        userRepository.saveAndFlush(user);
        deleteOldFile(oldFilename);

        return ResponseEntity.ok(Map.of(
                "coverColor", user.getCoverColor(),
                "message", "Обложка удалена; будет показан однотонный цвет"
        ));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Пользователь не найден"));
    }

    private void deleteOldFile(String filename) {
        if (filename == null
                || !filename.matches("[0-9a-fA-F-]{36}\\.(png|jpg)")) {
            return;
        }

        try {
            Path file = coverDirectory.resolve(filename).normalize();
            if (file.startsWith(coverDirectory)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            System.err.println(
                    "Не удалось удалить старую обложку: " + e.getMessage());
        }
    }
}
