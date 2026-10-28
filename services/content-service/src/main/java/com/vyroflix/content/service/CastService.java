package com.vyroflix.content.service;

import com.vyroflix.common.error.ApiException;
import com.vyroflix.common.error.ErrorCode;
import com.vyroflix.content.dto.CastMemberResponse;
import com.vyroflix.content.dto.PersonDtos.CreatePersonRequest;
import com.vyroflix.content.dto.PersonDtos.CreditEntry;
import com.vyroflix.content.dto.PersonDtos.PersonResponse;
import com.vyroflix.content.dto.PersonDtos.UpdateCreditsRequest;
import com.vyroflix.content.entity.Person;
import com.vyroflix.content.entity.TitleCredit;
import com.vyroflix.content.repository.PersonRepository;
import com.vyroflix.content.repository.TitleCreditRepository;
import com.vyroflix.content.repository.TitleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * People and their credits on titles. Credit changes on a PUBLISHED title are re-emitted as
 * catalog.title.updated.v1 so search stays current.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CastService {

    private static final List<String> ROLE_ORDER = List.of("ACTOR", "DIRECTOR", "WRITER", "PRODUCER");

    private final PersonRepository personRepository;
    private final TitleCreditRepository titleCreditRepository;
    private final TitleRepository titleRepository;
    private final AdminCatalogService adminCatalogService;

    @Transactional
    public PersonResponse createPerson(CreatePersonRequest request) {
        Person saved = personRepository.save(Person.builder()
                .name(request.getName().trim())
                .avatarUrl(blankToNull(request.getAvatarUrl()))
                .build());
        log.info("Created person id={}", saved.getId());
        return new PersonResponse(saved.getId(), saved.getName(), saved.getAvatarUrl());
    }

    /** Cast for one title: one credit query plus one person query (no N+1). */
    @Transactional(readOnly = true)
    public List<CastMemberResponse> castFor(UUID titleId) {
        List<TitleCredit> credits = titleCreditRepository.findByTitleId(titleId);
        Map<UUID, Person> people = peopleById(credits.stream().map(TitleCredit::getPersonId).collect(Collectors.toSet()));
        return credits.stream()
                .filter(c -> people.containsKey(c.getPersonId()))
                .sorted(Comparator
                        .comparingInt((TitleCredit c) -> roleRank(c.getRole()))
                        .thenComparing(c -> people.get(c.getPersonId()).getName()))
                .map(c -> toCast(c, people.get(c.getPersonId())))
                .toList();
    }

    /** Replaces the full credit list for a title. Every referenced person must exist. */
    @Transactional
    public List<CastMemberResponse> replaceCredits(UUID titleId, UpdateCreditsRequest request) {
        if (!titleRepository.existsById(titleId)) {
            throw new ApiException(ErrorCode.TITLE_NOT_FOUND, "Title not found with id: " + titleId);
        }
        Set<UUID> personIds = request.getCredits().stream()
                .map(CreditEntry::getPersonId)
                .collect(Collectors.toSet());
        if (peopleById(personIds).size() != personIds.size()) {
            throw new ApiException(ErrorCode.NOT_FOUND, "One or more people do not exist");
        }

        titleCreditRepository.deleteByTitleId(titleId);
        titleCreditRepository.saveAll(request.getCredits().stream()
                .map(e -> TitleCredit.builder()
                        .titleId(titleId)
                        .personId(e.getPersonId())
                        .role(e.getRole())
                        .characterName(blankToNull(e.getCharacterName()))
                        .build())
                .toList());

        adminCatalogService.emitUpdatedIfPublished(titleId);
        log.info("Replaced {} credits on title id={}", request.getCredits().size(), titleId);
        return castFor(titleId);
    }

    private Map<UUID, Person> peopleById(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return personRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Person::getId, Function.identity()));
    }

    private static CastMemberResponse toCast(TitleCredit credit, Person person) {
        return CastMemberResponse.builder()
                .personId(person.getId())
                .name(person.getName())
                .avatarUrl(person.getAvatarUrl())
                .role(credit.getRole())
                .characterName(credit.getCharacterName())
                .build();
    }

    private static int roleRank(String role) {
        int index = ROLE_ORDER.indexOf(role);
        return index < 0 ? ROLE_ORDER.size() : index;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
