package com.mymovie.log.presentation.adaptive

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mymovie.log.presentation.ui.theme.MyMovieLogTheme

// generic window sizes for reflow checks, "-like" names are hints, not device emulation
@Preview(name = "Phone portrait", group = "window", showBackground = true, device = "spec:width=411dp,height=891dp")
@Preview(name = "Phone landscape", group = "window", showBackground = true, device = "spec:width=891dp,height=411dp")
@Preview(name = "Flip-like portrait 22:9", group = "window", showBackground = true, device = "spec:width=412dp,height=1004dp")
@Preview(name = "Narrow cover-like", group = "window", showBackground = true, device = "spec:width=344dp,height=882dp")
@Preview(name = "Book-style foldable inner-like", group = "window", showBackground = true, device = "spec:width=690dp,height=829dp")
@Preview(name = "Cover-like 10:16", group = "window", showBackground = true, device = "spec:width=400dp,height=640dp")
@Preview(name = "Inner display-like 4:3", group = "window", showBackground = true, device = "spec:width=880dp,height=660dp")
@Preview(name = "Inner display-like 3:4", group = "window", showBackground = true, device = "spec:width=660dp,height=880dp")
@Preview(name = "Tablet landscape", group = "window", showBackground = true, device = "spec:width=1280dp,height=800dp")
annotation class AdaptiveWindowPreviews

// theme + AdaptiveLayoutInfo from the preview size, same as rememberAdaptiveLayoutInfo
// no navigation rail / bar around it
@Composable
fun AdaptivePreviewSurface(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val info = AdaptiveLayoutInfo(
        windowWidth = configuration.screenWidthDp.dp,
        windowHeight = configuration.screenHeightDp.dp
    )
    MyMovieLogTheme {
        ProvideAdaptiveLayoutInfo(info) {
            Surface(content = content)
        }
    }
}
