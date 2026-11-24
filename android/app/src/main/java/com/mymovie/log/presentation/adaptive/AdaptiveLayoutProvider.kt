package com.mymovie.log.presentation.adaptive

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity

// only place that reads window metrics / posture, screens use LocalAdaptiveLayoutInfo
val LocalAdaptiveLayoutInfo = compositionLocalOf { AdaptiveLayoutInfo.PhonePortrait }

// currentWindowSize (app window, so split / freeform report their own size)
// currentWindowAdaptiveInfo (FoldingFeature -> tabletop, hinge bounds)
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
