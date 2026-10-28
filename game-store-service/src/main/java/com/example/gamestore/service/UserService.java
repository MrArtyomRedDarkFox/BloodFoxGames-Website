package com.example.gamestore.service;

import com.example.gamestore.dto.LoginDto;
import com.example.gamestore.dto.RegisterDto;
import com.example.gamestore.dto.UserProfileDto;
import com.example.gamestore.dto.UpdateProfileDto;
import com.example.gamestore.model.Game;
import com.example.gamestore.model.User;
import com.example.gamestore.repository.GameRepository;
import com.example.gamestore.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final RestTemplate restTemplate = new RestTemplate();

    public UserService(UserRepository userRepository, GameRepository gameRepository) {
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
    }

    public User register(RegisterDto dto) {
        if (dto.getUsername() == null || dto.getUsername().isBlank()
                || dto.getEmail() == null || dto.getEmail().isBlank()
                || dto.getPassword() == null || dto.getPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Укажите имя, email и пароль длиной не менее 6 символов");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Имя пользователя уже занято");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email уже занят");
        }
        return userRepository.save(new User(
                dto.getUsername().trim(),
                dto.getEmail().trim(),
                passwordEncoder.encode(dto.getPassword())
        ));
    }

    public User login(LoginDto dto) {
        if (dto.getUsername() == null || dto.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверное имя пользователя или пароль");
        }
        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Неверное имя пользователя или пароль"));
        if (user.getPassword() == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Неверное имя пользователя или пароль");
        }
        return user;
    }

    public UserProfileDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getProfileDescription(),
                user.getAvatarKey(),
                user.getLibrary()
        );
    }


    @Transactional
    public UserProfileDto updateProfile(Long userId, UpdateProfileDto dto) {
        if (dto == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Настройки не переданы");
        }

        String displayName = dto.getDisplayName() == null
                ? "" : dto.getDisplayName().trim();
        String description = dto.getProfileDescription() == null
                ? "" : dto.getProfileDescription().trim();
        String avatarKey = dto.getAvatarKey() == null
                ? "" : dto.getAvatarKey().trim();

        if (displayName.isBlank() || displayName.length() > 32) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Имя должно содержать от 1 до 32 символов");
        }

        if (description.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Описание не должно превышать 500 символов");
        }

        Set<String> allowedAvatars = Set.of(
                "fox-red", "moon-wolf", "ember",
                "crimson-skull", "night-cat", "star"
        );

        if (!allowedAvatars.contains(avatarKey)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Неизвестный аватар");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Пользователь не найден"));

        user.setDisplayName(displayName);
        user.setProfileDescription(description);
        user.setAvatarKey(avatarKey);
        userRepository.save(user);

        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getProfileDescription(),
                user.getAvatarKey(),
                user.getLibrary()
        );
    }

    @Transactional
    public void buyGame(Long userId, Long gameId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Игра не найдена"));

        if (user.getLibrary() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Библиотека пользователя не инициализирована");
        }
        if (user.getLibrary().contains(game)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Эта игра уже есть в библиотеке");
        }
        user.getLibrary().add(game);
        userRepository.save(user);

        try {
            restTemplate.postForEntity(
                    "http://notifications-service:8082/api/v1/notifications",
                    Map.of("user_id", userId, "message", "Game purchased: " + game.getTitle()),
                    String.class
            );
        } catch (RestClientException exception) {
            // Покупка уже сохранена: ошибка уведомления не должна отменять библиотеку.
            System.err.println("Notification failed: " + exception.getMessage());
        }
    }

    public Set<Game> getLibrary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
        return user.getLibrary();
    }
}
