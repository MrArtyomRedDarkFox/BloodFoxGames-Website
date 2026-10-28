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
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

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
