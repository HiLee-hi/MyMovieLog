package com.mymovie.log.presentation.adaptive

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity

/**
 * The single place that reads window metrics and foldable posture.
 * Screens read [LocalAdaptiveLayoutInfo] instead of calling Window APIs themselves.
 */
val LocalAdaptiveLayoutInfo = compositionLocalOf { AdaptiveLayoutInfo.PhonePortrait }

/**
 * Builds [AdaptiveLayoutInfo] from the official Material 3 adaptive APIs:
 * [currentWindowSize] (WindowMetricsCalculator — the app window, so split screen and freeform
 * windows report their own size) and [currentWindowAdaptiveInfo] (Jetpack WindowManager
 * FoldingFeature → tabletop posture and hinge bounds).
 */
@Composable
fun rememberAdaptiveLayoutInfo(): AdaptiveLayoutInfo {
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val windowSize = currentWindowSize()
    val density = LocalDensity.current
    val posture = windowAdaptiveInfo.windowPosture
    return remember(windowSize, posture, density) {
        with(density) {
            AdaptiveLayoutInfo(
                windowWidth = windowSize.width.toDp(),
                windowHeight = windowSize.height.toDp(),
                isTabletop = posture.isTabletop,
                hinges = posture.hingeList
            )
        }
    }
}

@Composable
fun ProvideAdaptiveLayoutInfo(
    info: AdaptiveLayoutInfo = rememberAdaptiveLayoutInfo(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAdaptiveLayoutInfo provides info, content = content)
}
