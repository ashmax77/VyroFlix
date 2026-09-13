package com.vyroflix.common.util;

/**
 * Shared constants used across VyroFlix services.
 */
public final class Constants {

    private Constants() {
        // utility class
    }

    // ------------------------------------------------------------------ //
    // Correlation / Tracing                                               //
    // ------------------------------------------------------------------ //

    /** HTTP header for correlation ID propagation. */
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    /** HTTP header for the authenticated user ID (set by gateway). */
    public static final String HEADER_USER_ID = "X-User-Id";

    /** HTTP header for the authenticated user's role (set by gateway). */
    public static final String HEADER_USER_ROLE = "X-User-Role";

    // ------------------------------------------------------------------ //
    // Kafka Topics                                                        //
    // ------------------------------------------------------------------ //

    public static final String TOPIC_VIDEO_UPLOADED = "video.uploaded.v1";
    public static final String TOPIC_VIDEO_ENCODED = "video.encoded.v1";
    public static final String TOPIC_CATALOG_TITLE_CREATED = "catalog.title.created.v1";
    public static final String TOPIC_CATALOG_TITLE_UPDATED = "catalog.title.updated.v1";
    public static final String TOPIC_CATALOG_TITLE_PUBLISHED = "catalog.title.published.v1";
    public static final String TOPIC_CATALOG_TITLE_UNPUBLISHED = "catalog.title.unpublished.v1";
    public static final String TOPIC_HISTORY_PROGRESS_UPDATED = "history.progress.updated.v1";
    public static final String TOPIC_HISTORY_CONTENT_COMPLETED = "history.content.completed.v1";
    public static final String TOPIC_WATCHLIST_ITEM_ADDED = "watchlist.item.added.v1";
    public static final String TOPIC_WATCHLIST_ITEM_REMOVED = "watchlist.item.removed.v1";
    public static final String TOPIC_PLAYBACK_STARTED = "playback.started.v1";
    public static final String TOPIC_PLAYBACK_ENDED = "playback.ended.v1";
    public static final String TOPIC_USER_REGISTERED = "user.registered.v1";

    // ------------------------------------------------------------------ //
    // Roles                                                               //
    // ------------------------------------------------------------------ //

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    // ------------------------------------------------------------------ //
    // Service Names (for event envelope producer field)                    //
    // ------------------------------------------------------------------ //

    public static final String SERVICE_IDENTITY = "identity-service";
    public static final String SERVICE_CONTENT = "content-service";
    public static final String SERVICE_VIDEO = "video-service";
    public static final String SERVICE_ENCODING = "encoding-service";
    public static final String SERVICE_STREAMING = "streaming-service";
    public static final String SERVICE_HISTORY = "history-service";
    public static final String SERVICE_SEARCH = "search-service";
    public static final String SERVICE_RECOMMENDATION = "recommendation-service";

    // ------------------------------------------------------------------ //
    // Pagination Defaults                                                 //
    // ------------------------------------------------------------------ //

    public static final int DEFAULT_PAGE_SIZE = 24;
    public static final int MAX_PAGE_SIZE = 100;

    // ------------------------------------------------------------------ //
    // Object Storage                                                      //
    // ------------------------------------------------------------------ //

    public static final String BUCKET_RAW = "vyroflix-raw";
    public static final String BUCKET_CONTENT = "vyroflix-content";
}
