package com.parking.model;

public class User {
    private final int id;
    private String username;
    private String password;
    private Role role;

    public User(int id, String username, String password, Role role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String toCsv() {
        return id + "," + username + "," + password + "," + role;
    }

    public static User fromCsv(String line) {
        String[] parts = line.split(",");
        return new User(
                Integer.parseInt(parts[0]),
                parts[1],
                parts[2],
                Role.valueOf(parts[3])
        );
    }
}
