package com.serv.common;

/** Processing state of an uploaded video: only READY videos are shown to visitors. */
public enum VideoStatus {
    /** Uploaded, waiting for or being re-encoded. */
    PROCESSING,
    READY,
    /** Not a usable video (unreadable, too long…): kept so the worker sees why, until they delete it. */
    FAILED
}
