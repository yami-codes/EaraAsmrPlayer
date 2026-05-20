package com.asmr.player.ui.nav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.asmr.player.ui.player.MiniPlayerDisplayMode
import com.asmr.player.ui.theme.AsmrPlayerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

private const val BOTTOM_CHROME_UNDERLAY_TAG = "bottomChromeUnderlay"
private const val BOTTOM_CHROME_ROOT_TAG = "bottomChromeRoot"

@RunWith(AndroidJUnit4::class)
class BottomChromeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun blankAreaTap_doesNotPassThroughToUnderlyingContent() {
        var underlayClicks = 0

        composeRule.setContent {
            AsmrPlayerTheme {
                Box(modifier = Modifier.size(width = 360.dp, height = 120.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(BOTTOM_CHROME_UNDERLAY_TAG)
                            .clickable { underlayClicks++ }
                    )
                    BottomChrome(
                        activeRoute = Routes.Library,
                        miniPlayerVisible = false,
                        miniPlayerDisplayMode = MiniPlayerDisplayMode.CoverOnly,
                        onMiniPlayerDisplayModeChange = {},
                        onOpenNowPlaying = {},
                        onOpenQueue = {},
                        onNavigate = {},
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        composeRule.onNodeWithTag(BottomNavBarTag)
            .performTouchInput {
                down(centerLeft.copy(y = 4f))
                up()
            }

        composeRule.runOnIdle {
            assertEquals(0, underlayClicks)
        }
    }

    @Test
    fun navItemTap_stillInvokesNavigation() {
        var lastRoute: String? = null

        composeRule.setContent {
            AsmrPlayerTheme {
                BottomChrome(
                    activeRoute = Routes.Library,
                    miniPlayerVisible = false,
                    miniPlayerDisplayMode = MiniPlayerDisplayMode.CoverOnly,
                    onMiniPlayerDisplayModeChange = {},
                    onOpenNowPlaying = {},
                    onOpenQueue = {},
                    onNavigate = { lastRoute = it },
                    modifier = Modifier.size(width = 360.dp, height = 120.dp)
                )
            }
        }

        composeRule.onNodeWithTag("bottomNavItem:search").performClick()

        composeRule.runOnIdle {
            assertEquals(Routes.Search, lastRoute)
        }
    }

    @Test
    fun navBar_withoutMiniPlayer_staysCentered() {
        composeRule.setContent {
            AsmrPlayerTheme {
                Box(
                    modifier = Modifier
                        .size(width = 360.dp, height = 120.dp)
                        .testTag(BOTTOM_CHROME_ROOT_TAG),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    BottomChrome(
                        activeRoute = Routes.Library,
                        miniPlayerVisible = false,
                        miniPlayerDisplayMode = MiniPlayerDisplayMode.CoverOnly,
                        onMiniPlayerDisplayModeChange = {},
                        onOpenNowPlaying = {},
                        onOpenQueue = {},
                        onNavigate = {},
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        val rootBounds = composeRule.onNodeWithTag(BOTTOM_CHROME_ROOT_TAG).getUnclippedBoundsInRoot()
        val navBounds = composeRule.onNodeWithTag(BottomNavBarTag).getUnclippedBoundsInRoot()
        val rootCenterX = (rootBounds.left + rootBounds.right) / 2f
        val navCenterX = (navBounds.left + navBounds.right) / 2f

        assertTrue(
            "Expected nav bar center to stay near the screen center when mini player is hidden",
            kotlin.math.abs((navCenterX - rootCenterX).value) <= 1f
        )
    }

    @Test
    fun navBar_andMiniPlayer_areCenteredAsAGroup() {
        composeRule.setContent {
            AsmrPlayerTheme {
                Box(
                    modifier = Modifier
                        .size(width = 360.dp, height = 120.dp)
                        .testTag(BOTTOM_CHROME_ROOT_TAG),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    BottomChrome(
                        activeRoute = Routes.Library,
                        miniPlayerVisible = true,
                        miniPlayerDisplayMode = MiniPlayerDisplayMode.CoverOnly,
                        onMiniPlayerDisplayModeChange = {},
                        onOpenNowPlaying = {},
                        onOpenQueue = {},
                        onNavigate = {},
                        modifier = Modifier.fillMaxSize(),
                        miniPlayerContent = { miniModifier ->
                            Box(
                                modifier = miniModifier
                                    .height(56.dp)
                                    .testTag("testMiniPlayer"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Mini")
                            }
                        }
                    )
                }
            }
        }

        val rootBounds = composeRule.onNodeWithTag(BOTTOM_CHROME_ROOT_TAG).getUnclippedBoundsInRoot()
        val navBounds = composeRule.onNodeWithTag(BottomNavBarTag).getUnclippedBoundsInRoot()
        val miniBounds = composeRule.onNodeWithTag("testMiniPlayer").getUnclippedBoundsInRoot()
        val rootCenterX = (rootBounds.left + rootBounds.right) / 2f
        val groupCenterX = (navBounds.left + miniBounds.right) / 2f

        assertTrue(
            "Expected bottom chrome group to stay centered when mini player is visible",
            kotlin.math.abs((groupCenterX - rootCenterX).value) <= 1f
        )
    }

    @Test
    fun inactiveNavItemContainer_isTransparent() {
        val activeContainer = Color(0xFFF2D4C3)

        val inactive = resolveBottomNavItemContainerColor(
            activeContainer = activeContainer,
            selectedProgress = 0f
        )
        val active = resolveBottomNavItemContainerColor(
            activeContainer = activeContainer,
            selectedProgress = 1f
        )

        assertEquals(Color.Transparent, inactive)
        assertEquals(activeContainer, active)
    }

    @Test
    fun partialNavItemContainer_preservesScaledAlpha() {
        val activeContainer = Color(0xCCF2D4C3)

        val partial = resolveBottomNavItemContainerColor(
            activeContainer = activeContainer,
            selectedProgress = 0.5f
        )

        assertEquals(activeContainer.red, partial.red, 0.0001f)
        assertEquals(activeContainer.green, partial.green, 0.0001f)
        assertEquals(activeContainer.blue, partial.blue, 0.0001f)
        assertTrue(abs(partial.alpha - (activeContainer.alpha * 0.5f)) <= 0.0001f)
    }
}
