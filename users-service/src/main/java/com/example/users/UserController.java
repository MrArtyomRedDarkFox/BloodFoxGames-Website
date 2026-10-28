package com.example.users;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserRepository repository;

    public UserController(UserRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return repository.findAll();
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        return repository.save(user);
    }
}
