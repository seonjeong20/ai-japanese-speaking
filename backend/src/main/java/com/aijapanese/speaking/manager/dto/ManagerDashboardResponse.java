package com.aijapanese.speaking.manager.dto;

import java.util.List;

public record ManagerDashboardResponse(
        long learnerCount,
        long monthlySpeakingCount,
        double averageStudyMinutes,
        long monthlyInterviewCount,
        long pendingLearnerCount,
        List<DailyStudyMinutesResponse> weeklyUsage
) {
}
