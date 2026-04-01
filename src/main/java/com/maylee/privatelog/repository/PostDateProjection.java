package com.maylee.privatelog.repository;

import java.time.LocalDate;

public interface PostDateProjection {
    LocalDate getPostDate();
    Long getPostId();
}
