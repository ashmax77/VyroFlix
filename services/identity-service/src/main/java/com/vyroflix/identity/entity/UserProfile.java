package com.vyroflix.identity.entity;

import com.vyroflix.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * UserProfile entity containing user presentation and preference data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_profiles", schema = "identity")
public class UserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
}
