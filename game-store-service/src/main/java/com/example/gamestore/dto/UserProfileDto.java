package com.example.gamestore.dto;

import com.example.gamestore.model.Game;
import java.util.Set;

public class UserProfileDto {
    private Long id;
    private String username;
    private String email;
    private String displayName;
    private String profileDescription;
    private String avatarKey;
    private Set<Game> library;

    public UserProfileDto(
            Long id,
            String username,
            String email,
            String displayName,
            String profileDescription,
            String avatarKey,
            Set<Game> library) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.profileDescription = profileDescription;
        this.avatarKey = avatarKey;
        this.library = library;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getProfileDescription() {
        return profileDescription;
    }

    public String getAvatarKey() {
        return avatarKey;
    }

    public Set<Game> getLibrary() {
        return library;
    }
}
