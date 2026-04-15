package com.maylee.privatelog.dto.archive;

import java.util.List;

public record DiaryMonthGroup(
        int month,
        List<DiaryDayGroup> days
) {}
