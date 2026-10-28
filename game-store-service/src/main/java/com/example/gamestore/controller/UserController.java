package com.example.gamestore.controller;

import com.example.gamestore.dto.LoginDto;
import com.example.gamestore.dto.RegisterDto;
import com.example.gamestore.dto.UserProfileDto;
import com.example.gamestore.dto.UpdateProfileDto;
import com.example.gamestore.model.Game;
import com.example.gamestore.model.User;
import com.example.gamestore.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody RegisterDto dto) {
        return ResponseEntity.ok(userService.register(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody LoginDto dto) {
        return ResponseEntity.ok(userService.login(dto));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileDto> getProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }


    @PutMapping("/{userId}/profile")
    public ResponseEntity<UserProfileDto> updateProfile(
            @PathVariable Long userId,
            @RequestBody UpdateProfileDto dto) {
        return ResponseEntity.ok(userService.updateProfile(userId, dto));
    }

    @PostMapping("/{userId}/buy/{gameId}")
    public ResponseEntity<String> buyGame(@PathVariable Long userId, @PathVariable Long gameId) {
        userService.buyGame(userId, gameId);
        return ResponseEntity.ok("Игра добавлена в библиотеку!");
    }

    @GetMapping("/{userId}/library")
    public ResponseEntity<Set<Game>> getLibrary(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getLibrary(userId));
    }
}
