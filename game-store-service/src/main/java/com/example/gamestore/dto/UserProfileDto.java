package com.example.gamestore.dto;

import com.example.gamestore.model.Game;
import java.util.Set;

public class UserProfileDto {
    private Long id;
    private String username;
    private String email;
    private Set<Game> library;

    public UserProfileDto(Long id, String username, String email, Set<Game> library) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.library = library;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public Set<Game> getLibrary() { return library; }
}
