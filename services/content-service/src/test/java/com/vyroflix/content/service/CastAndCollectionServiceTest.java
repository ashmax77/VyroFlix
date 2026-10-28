package com.vyroflix.content.service;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.content.dto.CollectionDtos.CreateCollectionRequest;
import com.vyroflix.content.dto.CollectionDtos.UpdateCollectionTitlesRequest;
import com.vyroflix.content.dto.PersonDtos.CreditEntry;
import com.vyroflix.content.dto.PersonDtos.UpdateCreditsRequest;
import com.vyroflix.content.entity.ContentCollection;
import com.vyroflix.content.entity.Person;
import com.vyroflix.content.entity.PublicationState;
import com.vyroflix.content.entity.Title;
import com.vyroflix.content.entity.TitleType;
import com.vyroflix.content.repository.ContentCollectionRepository;
import com.vyroflix.content.repository.PersonRepository;
import com.vyroflix.content.repository.TitleCreditRepository;
import com.vyroflix.content.repository.TitleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CastAndCollectionServiceTest {

    @Mock private PersonRepository personRepository;
    @Mock private TitleCreditRepository titleCreditRepository;
    @Mock private TitleRepository titleRepository;
    @Mock private AdminCatalogService adminCatalogService;
    @Mock private ContentCollectionRepository collectionRepository;

    @InjectMocks private CastService castService;
    @InjectMocks private CollectionService collectionService;

    @Test
    @DisplayName("replaceCredits refuses a credit that points at an unknown person and changes nothing")
    void replaceCredits_UnknownPerson() {
        UUID titleId = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        when(titleRepository.existsById(titleId)).thenReturn(true);
        when(personRepository.findAllById(any())).thenReturn(List.of());

        UpdateCreditsRequest request = new UpdateCreditsRequest(List.of(new CreditEntry(unknown, "ACTOR", "Hero")));

        assertThatThrownBy(() -> castService.replaceCredits(titleId, request))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verify(titleCreditRepository, never()).deleteByTitleId(any());
        verify(adminCatalogService, never()).emitUpdatedIfPublished(any());
    }

    @Test
    @DisplayName("replaceCredits on an existing title re-emits the updated event for search")
    void replaceCredits_EmitsUpdate() {
        UUID titleId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        when(titleRepository.existsById(titleId)).thenReturn(true);
        when(personRepository.findAllById(any())).thenReturn(List.of(Person.builder().id(personId).name("Ada").build()));
        when(titleCreditRepository.findByTitleId(titleId)).thenReturn(List.of());

        castService.replaceCredits(titleId, new UpdateCreditsRequest(List.of(new CreditEntry(personId, "DIRECTOR", null))));

        verify(titleCreditRepository).deleteByTitleId(titleId);
        verify(adminCatalogService).emitUpdatedIfPublished(titleId);
    }

    @Test
    @DisplayName("creating a collection with an existing slug is a conflict")
    void createCollection_DuplicateSlug() {
        when(collectionRepository.existsBySlug("staff-picks")).thenReturn(true);

        assertThatThrownBy(() -> collectionService.create(new CreateCollectionRequest("Staff picks", "staff-picks", 1)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    @DisplayName("setTitles rejects a request that names a title which does not exist")
    void setTitles_MissingTitle() {
        UUID collectionId = UUID.randomUUID();
        UUID existing = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        when(collectionRepository.findById(collectionId)).thenReturn(Optional.of(
                ContentCollection.builder().id(collectionId).name("A").slug("a").build()));
        when(titleRepository.findAllById(any())).thenReturn(List.of(
                titleWithId(existing, PublicationState.DRAFT)));

        assertThatThrownBy(() -> collectionService.setTitles(collectionId,
                new UpdateCollectionTitlesRequest(List.of(existing, missing))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TITLE_NOT_FOUND));
        verify(collectionRepository, never()).save(any());
    }

    private static Title titleWithId(UUID id, PublicationState state) {
        Title title = Title.builder().title("Title").type(TitleType.MOVIE).publicationState(state).build();
        title.setId(id);
        return title;
    }
}
