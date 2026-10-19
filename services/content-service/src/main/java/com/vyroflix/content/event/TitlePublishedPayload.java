package com.vyroflix.content.event;

import com.vyroflix.content.entity.TitleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TitlePublishedPayload {
    private UUID titleId;
    private String title;
    private TitleType type;
    private Set<String> genreSlugs;
    private Integer releaseYear;
    private String maturityRating;
    private Instant publishedAt;
}
