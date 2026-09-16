package com.aijapanese.speaking.admin.dto;

import java.time.LocalDate;

public record AdminDailyUsageResponse(
        LocalDate date,
        long sessionCount
) {
}
