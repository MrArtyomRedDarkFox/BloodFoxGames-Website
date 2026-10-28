package com.example.gamestore.model;

import jakarta.persistence.*;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity(name = "StoreUser")
@Table(name = "store_users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String email;

@Column(length = 80)
private String displayName;

@Column(length = 500)
private String profileDescription = "";

@Column(length = 40)
private String avatarKey = "fox-red";

@Column(length = 100)
private String avatarFilename;

    @Column(length = 7)
    private String profileColor = "#ef3340";

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @ManyToMany
    @JoinTable(
        name = "user_games",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "game_id")
    )
    private Set<Game> games;

    public User() {}

    public User(String username, String email, String password) {
    this.username = username;
    this.email = email;
    this.password = password;
    this.displayName = username;
    this.profileDescription = "";
    this.avatarKey = "fox-red";
}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() {
        return displayName == null || displayName.isBlank()
                ? username : displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getProfileDescription() {
        return profileDescription == null ? "" : profileDescription;
    }

    public void setProfileDescription(String profileDescription) {
        this.profileDescription = profileDescription;
    }

    public String getAvatarKey() {
        return avatarKey == null || avatarKey.isBlank()
                ? "fox-red" : avatarKey;
    }

    public void setAvatarKey(String avatarKey) {
        this.avatarKey = avatarKey;
    }
    public String getProfileColor() {
        return profileColor == null
                || !profileColor.matches("^#[0-9a-fA-F]{6}$")
                ? "#ef3340" : profileColor;
    }

    public void setProfileColor(String profileColor) {
        this.profileColor = profileColor;
    }

    public String getAvatarFilename() {
        return avatarFilename;
    }

    public void setAvatarFilename(String avatarFilename) {
        this.avatarFilename = avatarFilename;
    }


    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Set<Game> getGames() { return games; }
    public void setGames(Set<Game> games) { this.games = games; }

    public Set<Game> getLibrary() {
        return games;
    }

    public void setLibrary(Set<Game> games) {
        this.games = games;
    }
}
