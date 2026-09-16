package com.aijapanese.speaking.manager.dto;

import java.time.LocalDate;

public record DailyStudyMinutesResponse(
        LocalDate date,
        int studyMinutes
) {
}
