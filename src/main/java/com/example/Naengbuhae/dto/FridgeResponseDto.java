package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Fridge;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class FridgeResponseDto {
    private final UUID id;
    private final String name;
    private final String ownerUsername;
    private final List<MemberDto> members;
    private final boolean isOwner; // 호출자가 owner인지 (초대 코드 발급/멤버 제거 권한)

    public FridgeResponseDto(Fridge fridge, List<MemberDto> members, boolean isOwner) {
        this.id = fridge.getId();
        this.name = fridge.getName();
        this.ownerUsername = fridge.getOwner().getUsername();
        this.members = members;
        this.isOwner = isOwner;
    }

    @Getter
    public static class MemberDto {
        private final String username;
        private final String name;
        private final boolean isOwner;

        public MemberDto(String username, String name, boolean isOwner) {
            this.username = username;
            this.name = name;
            this.isOwner = isOwner;
        }
    }
}
