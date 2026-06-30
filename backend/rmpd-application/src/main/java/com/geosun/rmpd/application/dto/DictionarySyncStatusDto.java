package com.geosun.rmpd.application.dto;

import java.time.Instant;

public record DictionarySyncStatusDto(String dictType, long entryCount, Instant lastSyncedAt, boolean stale) {}
