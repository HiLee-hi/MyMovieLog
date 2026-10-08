package com.mymovie.log.testing

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.mymovie.log.presentation.adaptive.AdaptiveLayoutInfo
import com.mymovie.log.presentation.adaptive.NavigationLayout
import com.mymovie.log.presentation.adaptive.ProvideAdaptiveLayoutInfo
import com.mymovie.log.presentation.ui.theme.MyMovieLogTheme
import java.io.File
import kotlin.math.roundToInt

/**
 * A representative window: size and aspect ratio only. Names describe the kind of window,
 * they do not claim to emulate a particular device.
 */
data class WindowSpec(val name: String, val width: Int, val height: Int) {
    val info get() = AdaptiveLayoutInfo(width.dp, height.dp)
    override fun toString() = name
}

object WindowSpecs {
    val PhonePortrait = WindowSpec("phone_portrait", 411, 891)
    val PhoneLandscape = WindowSpec("phone_landscape", 891, 411)
    val FlipPortrait = WindowSpec("flip_like_portrait_22x9", 412, 1004)
    val NarrowCover = WindowSpec("narrow_cover_like", 344, 882)
    val BookInner = WindowSpec("book_style_inner_like", 690, 829)
    val Cover10x16 = WindowSpec("cover_like_10x16", 400, 640)
    val Inner4x3 = WindowSpec("inner_like_4x3", 880, 660)
    val Inner3x4 = WindowSpec("inner_like_3x4", 660, 880)
    val TabletLandscape = WindowSpec("tablet_landscape", 1280, 800)

    val all = listOf(PhonePortrait, PhoneLandscape, FlipPortrait, NarrowCover, BookInner, Cover10x16, Inner4x3, Inner3x4, TabletLandscape)
}

const val WINDOW_TAG = "test_window"

/**
 * Renders [content] inside a box of exactly the window size with the [AdaptiveLayoutInfo] the app
 * would compute for that window, optionally wrapped in the same NavigationSuiteScaffold choice
 * (bottom bar / rail) that AppNavHost makes.
 */
@Composable
fun TestWindow(spec: WindowSpec, withNavigationSuite: Boolean = false, content: @Composable () -> Unit) {
    val info = spec.info
    MyMovieLogTheme(dynamicColor = false) {
        ProvideAdaptiveLayoutInfo(info) {
            Box(modifier = Modifier.size(spec.width.dp, spec.height.dp).testTag(WINDOW_TAG)) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (withNavigationSuite) {
                        NavigationSuiteScaffold(
                            layoutType = if (info.navigationLayout == NavigationLayout.Rail) {
                                NavigationSuiteType.NavigationRail
                            } else {
                                NavigationSuiteType.NavigationBar
                            },
                            navigationSuiteItems = {
                                item(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text("홈") })
                                item(selected = false, onClick = {}, icon = { Icon(Icons.Default.Search, null) }, label = { Text("검색") })
                                item(selected = false, onClick = {}, icon = { Icon(Icons.Default.BarChart, null) }, label = { Text("통계") })
                            },
                            content = content
                        )
                    } else {
                        content()
                    }
                }
            }
        }
    }
}

/**
 * Saves the rendered window to app/build/outputs/adaptive-screenshots for manual review.
 * Draws the Compose root view directly (captureToImage waits for a redraw that Robolectric's
 * paused looper never delivers) and crops it to the test window.
 */
fun ComposeTestRule.saveWindowScreenshot(name: String) {
    waitForIdle()
    val node = onNodeWithTag(WINDOW_TAG).fetchSemanticsNode()
    val view = (node.root as ViewRootForTest).view
    val full = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    view.draw(Canvas(full))
    val bounds = node.boundsInRoot
    val left = bounds.left.roundToInt().coerceIn(0, full.width - 1)
    val top = bounds.top.roundToInt().coerceIn(0, full.height - 1)
    val cropped = Bitmap.createBitmap(
        full, left, top,
        bounds.width.roundToInt().coerceAtMost(full.width - left),
        bounds.height.roundToInt().coerceAtMost(full.height - top)
    )
    val dir = File("build/outputs/adaptive-screenshots").apply { mkdirs() }
    File(dir, "$name.png").outputStream().use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
}
