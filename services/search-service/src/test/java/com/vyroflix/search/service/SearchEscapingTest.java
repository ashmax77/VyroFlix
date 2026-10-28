package com.vyroflix.search.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchEscapingTest {

    @Test
    @DisplayName("LIKE wildcards and the escape character are escaped so user input matches literally")
    void escapesLikeWildcards() {
        assertThat(SearchService.escapeLike("100%")).isEqualTo("100\\%");
        assertThat(SearchService.escapeLike("a_b")).isEqualTo("a\\_b");
        assertThat(SearchService.escapeLike("c:\\x")).isEqualTo("c:\\\\x");
        assertThat(SearchService.escapeLike(null)).isEmpty();
    }
}
