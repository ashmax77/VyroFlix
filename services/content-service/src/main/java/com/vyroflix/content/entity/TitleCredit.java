package com.vyroflix.content.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Links a person to a title in a role (ACTOR, DIRECTOR, WRITER, PRODUCER).
 * Ids are plain columns; people are resolved with one batched query to avoid N+1 reads.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "title_credits", schema = "content")
public class TitleCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title_id", nullable = false)
    private UUID titleId;

    @Column(name = "person_id", nullable = false)
    private UUID personId;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Column(name = "character_name", length = 255)
    private String characterName;
}
