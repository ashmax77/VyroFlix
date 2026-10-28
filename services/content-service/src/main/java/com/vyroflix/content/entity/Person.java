package com.vyroflix.content.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** A cast or crew member. Not a BaseEntity: the persons table has no audit columns. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "persons", schema = "content")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
}
