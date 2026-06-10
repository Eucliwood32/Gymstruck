package com.example.gymstruck.ui

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.gymstruck.domain.ShortVideo
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.launch

/**
 * YouTube Shorts 연속 피드.
 * 검증된 android-youtube-player 라이브러리(IFrame Player 래퍼)를 사용해
 * 각 페이지에서 영상 1개를 재생하고, 세로 스와이프로 다음 영상으로 넘어간다.
 * 현재 페이지만 재생하고 나머지는 일시정지한다.
 */
@Composable
fun ShortsFeed(
    shorts: List<ShortVideo>,
    modifier: Modifier = Modifier,
) {
    if (shorts.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { shorts.size })
    val scope = rememberCoroutineScope()

    VerticalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
        key = { shorts[it].videoId },
    ) { page ->
        val isCurrent = pagerState.currentPage == page && !pagerState.isScrollInProgress
        ShortPlayer(
            videoId = shorts[page].videoId,
            playing = isCurrent,
            // 임베드 불가/오류 영상은 현재 페이지일 때 자동으로 다음으로 넘긴다.
            onUnplayable = {
                if (page == pagerState.currentPage) {
                    val next = page + 1
                    if (next < shorts.size) scope.launch { pagerState.animateScrollToPage(next) }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private class PlayerHolder {
    var view: YouTubePlayerView? = null
    var player: YouTubePlayer? = null
    var pendingPlay: Boolean = false
}

@Composable
private fun ShortPlayer(
    videoId: String,
    playing: Boolean,
    onUnplayable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // videoId가 바뀌면 새 플레이어가 생성되도록 remember 키로 사용
    val holder = remember(videoId) { PlayerHolder() }
    // 콜백은 factory에서 1회 캡처되므로 항상 최신 값을 부르도록 rememberUpdatedState 사용
    val currentOnUnplayable by rememberUpdatedState(onUnplayable)

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                YouTubePlayerView(context).apply {
                    // 기본(wrap_content)은 16:9로 측정되어 작은 가로 박스가 된다.
                    // 화면 전체를 채우도록 layoutParams를 MATCH_PARENT로 지정한다.
                    // (라이브러리 matchParent()는 기존 layoutParams가 필요해 팩토리 시점엔 NPE)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    // 라이브러리의 자동 lifecycle 초기화를 끄고 수동 init/release 한다.
                    enableAutomaticInitialization = false

                    val options = IFramePlayerOptions.Builder(context)
                        .controls(1)
                        .rel(0)
                        .build()

                    initialize(
                        object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                holder.player = youTubePlayer
                                if (holder.pendingPlay) {
                                    youTubePlayer.loadVideo(videoId, 0f) // 자동재생
                                } else {
                                    youTubePlayer.cueVideo(videoId, 0f)  // 대기(미재생)
                                }
                            }

                            override fun onError(
                                youTubePlayer: YouTubePlayer,
                                error: PlayerConstants.PlayerError,
                            ) {
                                // 임베드 불가 등 재생 불가 영상이면 다음으로 넘긴다.
                                currentOnUnplayable()
                            }
                        },
                        true,
                        options,
                    )
                    holder.view = this
                }
            },
            update = {
                holder.pendingPlay = playing
                val p = holder.player ?: return@AndroidView
                if (playing) p.play() else p.pause()
            },
        )
    }

    DisposableEffect(videoId) {
        onDispose {
            holder.view?.release()
            holder.view = null
            holder.player = null
        }
    }
}
