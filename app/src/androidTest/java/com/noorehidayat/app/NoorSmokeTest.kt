package com.noorehidayat.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoorSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun learningGuideOpensFromMoreTab() {
        composeRule.onNodeWithText("More").performClick()
        composeRule.onNodeWithText("Learning").performClick()
        composeRule.onNodeWithText("Quran learning").assertIsDisplayed()
        composeRule.onNodeWithText("1. Arabic alphabet").assertIsDisplayed()
    }

    @Test
    fun quizOpensAndShowsFirstQuestion() {
        composeRule.onNodeWithText("More").performClick()
        composeRule.onNodeWithText("Quiz").performClick()
        composeRule.onNodeWithText("How many daily obligatory prayers are there?")
            .assertIsDisplayed()
    }

    @Test
    fun qiblaCompassOpensAndRequestsLocationOnlyWhenNeeded() {
        composeRule.onNodeWithText("More").performClick()
        composeRule.onNodeWithText("Qibla").performClick()
        composeRule.onNodeWithText("Qibla Compass").assertIsDisplayed()
        composeRule.onNodeWithText("Find Qibla direction").assertIsDisplayed()
    }

    @Test
    fun prayerTimesScreenOpensFromMore() {
        composeRule.onNodeWithText("More").performClick()
        composeRule.onNodeWithText("Prayer Times").performClick()
        composeRule.onNodeWithText("Calculated for your device location").assertIsDisplayed()
    }

    @Test
    fun discoverOpensVideoLibraryWithLanguageAndTopicFilters() {
        composeRule.onNodeWithText("Discover").performClick()
        composeRule.onNodeWithText("Islamic Dramas").performClick()
        composeRule.onNodeWithText("LANGUAGE").assertIsDisplayed()
        composeRule.onNodeWithText("Urdu").assertIsDisplayed()
        composeRule.onNodeWithText("English").assertIsDisplayed()
        composeRule.onNodeWithText("Arabic").assertIsDisplayed()
        composeRule.onNodeWithText("POPULAR TOPICS").assertIsDisplayed()
        composeRule.onNodeWithText("Tafseer").assertIsDisplayed()
    }
}
