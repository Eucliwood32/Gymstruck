package com.example.gymstruck.domain

sealed interface SideEffect {
    data object KeepScreenOn : SideEffect
    data object ReleaseScreen : SideEffect
    data object StartDucking : SideEffect  // 휴식 종료 → Working 진입 시 더킹 시작
    data object StopDucking : SideEffect   // 세트 완료 / 리셋 시 더킹 종료
}
