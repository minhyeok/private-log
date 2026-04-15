package com.maylee.privatelog.dto.archive;

import java.util.List;

public record DiaryYearGroup(
        int year,
        List<DiaryMonthGroup> months
) {}
