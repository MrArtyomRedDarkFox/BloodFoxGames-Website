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
    private String avatarUrl;
    private String profileColor;
    private Set<Game> library;

    public UserProfileDto(
            Long id,
            String username,
            String email,
            String displayName,
            String profileDescription,
            String avatarKey,
            String avatarUrl,
            String profileColor,
            Set<Game> library) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.profileDescription = profileDescription;
        this.avatarKey = avatarKey;
        this.avatarUrl = avatarUrl;
        this.profileColor = profileColor;
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

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getProfileColor() {
        return profileColor;
    }

    public Set<Game> getLibrary() {
        return library;
    }
}
