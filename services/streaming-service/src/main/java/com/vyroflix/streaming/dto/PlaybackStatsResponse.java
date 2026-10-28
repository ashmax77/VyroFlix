package com.vyroflix.streaming.dto;

import java.time.LocalDate;
import java.util.List;

/** Aggregate playback figures for the admin dashboard. Contains no per-user data. */
public record PlaybackStatsResponse(long activeSessions, List<DailyCount> daily) {

    public record DailyCount(LocalDate date, long sessions) {
    }
}
