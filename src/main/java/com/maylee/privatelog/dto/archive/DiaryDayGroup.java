package com.maylee.privatelog.dto.archive;

import java.time.LocalDate;
import java.util.List;

public record DiaryDayGroup(
        LocalDate date,
        List<DiaryDayEntry> posts
) {}
