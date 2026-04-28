package com.example.Naengbuhae.user;

import lombok.Getter;

@Getter
public class UserResponseDto {
    private Long id;
    private String username;
    private UserRole role;

    public UserResponseDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.role = user.getRole();
    }
}
