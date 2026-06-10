package com.example.gymstruck.domain

/** 휴식 시간 동작 방식. */
enum class RestMode {
    /** 기존 동작: 배경 음악 더킹만 한다. */
    DUCKING,

    /** 휴식 동안 검색된 YouTube Shorts를 연속 재생한다. */
    SHORTS,
}
