package com.vyroflix.content.service;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.content.entity.PublicationState;
import com.vyroflix.content.entity.Title;
import com.vyroflix.content.entity.TitleType;
import com.vyroflix.content.repository.TitleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDeleteTitleTest {

    @Mock private TitleRepository titleRepository;
    @InjectMocks private AdminCatalogService adminCatalogService;

    @Test
    @DisplayName("deleteTitle refuses a PUBLISHED title with CONFLICT")
    void deletePublished_Conflict() {
        UUID id = UUID.randomUUID();
        when(titleRepository.findById(id)).thenReturn(Optional.of(
                titleWithId(id, PublicationState.PUBLISHED)));

        assertThatThrownBy(() -> adminCatalogService.deleteTitle(id))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        verify(titleRepository, never()).delete(any(Title.class));
    }

    @Test
    @DisplayName("deleteTitle removes a DRAFT title")
    void deleteDraft() {
        UUID id = UUID.randomUUID();
        Title draft = titleWithId(id, PublicationState.DRAFT);
        when(titleRepository.findById(id)).thenReturn(Optional.of(draft));

        adminCatalogService.deleteTitle(id);

        verify(titleRepository).delete(draft);
    }

    private static Title titleWithId(UUID id, PublicationState state) {
        Title title = Title.builder().title("Title").type(TitleType.MOVIE).publicationState(state).build();
        title.setId(id);
        return title;
    }
}
