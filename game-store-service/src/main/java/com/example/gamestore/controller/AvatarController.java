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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class AvatarController {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private final UserRepository userRepository;
    private final Path avatarDirectory;

    public AvatarController(
            UserRepository userRepository,
            @Value("${app.avatar-dir:/data/uploads/avatars}") String directory) {
        this.userRepository = userRepository;
        this.avatarDirectory = Paths.get(directory).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.avatarDirectory);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Не удалось создать каталог аватаров", e);
        }
    }

    @PostMapping(
            value = "/{userId}/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) {

        User user = findUser(userId);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Выбери изображение");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Размер изображения не должен превышать 5 МБ");
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

            BufferedImage image = ImageIO.read(
                    new ByteArrayInputStream(bytes));

            if (image == null
                    || image.getWidth() > 4096
                    || image.getHeight() > 4096
                    || (long) image.getWidth() * image.getHeight()
                        > 16_000_000L) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Изображение повреждено или имеет слишком большие размеры");
            }

            String extension = png ? ".png" : ".jpg";
            String filename = UUID.randomUUID() + extension;

            Path target = avatarDirectory.resolve(filename).normalize();

            if (!target.startsWith(avatarDirectory)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Недопустимый путь файла");
            }

            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);

            String oldFilename = user.getAvatarFilename();
            user.setAvatarFilename(filename);

            try {
                userRepository.save(user);
            } catch (RuntimeException e) {
                Files.deleteIfExists(target);
                throw e;
            }

            deleteOldFile(oldFilename);

            return ResponseEntity.ok(Map.of(
                    "avatarUrl", "/api/v1/users/" + userId + "/avatar",
                    "message", "Аватар успешно обновлён"
            ));

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Не удалось обработать изображение", e);
        }
    }

    @GetMapping("/{userId}/avatar")
    public ResponseEntity<Resource> getAvatar(
            @PathVariable Long userId) {

        User user = findUser(userId);
        String filename = user.getAvatarFilename();

        if (filename == null
                || !filename.matches(
                    "[0-9a-fA-F-]{36}\\.(png|jpg)")) {
            return ResponseEntity.notFound().build();
        }

        Path file = avatarDirectory.resolve(filename).normalize();

        if (!file.startsWith(avatarDirectory)
                || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = filename.endsWith(".png")
                ? MediaType.IMAGE_PNG
                : MediaType.IMAGE_JPEG;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.noCache())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(file));
    }

    @DeleteMapping("/{userId}/avatar")
    public ResponseEntity<Map<String, String>> deleteAvatar(
            @PathVariable Long userId) {

        User user = findUser(userId);
        String oldFilename = user.getAvatarFilename();

        user.setAvatarFilename(null);
        userRepository.save(user);

        deleteOldFile(oldFilename);

        return ResponseEntity.ok(Map.of(
                "avatarKey", user.getAvatarKey(),
                "message", "Возвращён встроенный аватар"
        ));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Пользователь не найден"));
    }

    private void deleteOldFile(String filename) {
        if (filename == null
                || !filename.matches(
                    "[0-9a-fA-F-]{36}\\.(png|jpg)")) {
            return;
        }

        try {
            Path file = avatarDirectory.resolve(filename).normalize();

            if (file.startsWith(avatarDirectory)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            System.err.println(
                    "Не удалось удалить старый аватар: " + e.getMessage());
        }
    }
}
