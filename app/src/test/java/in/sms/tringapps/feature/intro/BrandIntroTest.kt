package `in`.sms.tringapps.feature.intro

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.sms.tringapps.core.designsystem.theme.TringappsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrandIntroTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsWordmarkThenFinishes() {
        var finished = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            TringappsTheme { BrandIntro(onFinished = { finished++ }) }
        }

        composeRule.onNodeWithTag(BRAND_INTRO_TAG).assertExists()
        composeRule.onNodeWithText("TRINGAPPS").assertExists()
        assertEquals(0, finished)

        composeRule.mainClock.advanceTimeBy(3_000)
        composeRule.waitForIdle()

        assertEquals(1, finished)
    }
}
