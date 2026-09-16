package com.aijapanese.speaking.admin.dto;

import java.util.List;

public record AdminDashboardResponse(
        long organizationCount,
        long totalUserCount,
        long monthlySpeakingCount,
        long monthlyActiveUserCount,
        long pendingManagerCount,
        List<AdminDailyUsageResponse> weeklyUsage,
        long weeklyTotalCount,
        List<RecentOrganizationResponse> recentOrganizations
) {
}
