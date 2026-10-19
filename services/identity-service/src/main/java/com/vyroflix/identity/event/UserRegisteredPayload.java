package com.vyroflix.identity.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredPayload {

    private UUID userId;
    private String email;
    private String displayName;
    private Instant registeredAt;
}
