package com.vyroflix.encoding.entity;

/**
 * State lifecycle for background video transcoding jobs.
 *
 * <p>Lifecycle: QUEUED -> PROCESSING -> READY (or FAILED).</p>
 */
public enum EncodingStatus {
    QUEUED,
    PROCESSING,
    READY,
    FAILED
}
