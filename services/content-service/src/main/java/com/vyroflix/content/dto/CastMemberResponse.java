package com.vyroflix.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CastMemberResponse {
    private UUID personId;
    private String name;
    private String avatarUrl;
    private String role;
    private String characterName;
}
