package com.example.gymstruck.domain

/** 휴식 중 재생할 YouTube Short 영상 한 건. */
data class ShortVideo(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val channelTitle: String,
)
