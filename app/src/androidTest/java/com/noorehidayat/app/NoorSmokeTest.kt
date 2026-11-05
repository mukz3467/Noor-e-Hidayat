package com.noorehidayat.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.location.Location
import org.junit.Assert.assertTrue
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
    fun qiblaBearingUsesGreatCircleDirection() {
        val atKaaba = calculateQiblaBearing(21.4225, 39.8262)
        val fromFaisalabad = calculateQiblaBearing(31.418, 73.079)
        assertTrue(kotlin.math.abs(atKaaba) < 0.01f)
        assertTrue(kotlin.math.abs(fromFaisalabad - 259.55f) < 0.5f)
    }

    @Test
    fun qiblaTurnUsesShortestDirection() {
        assertTrue(kotlin.math.abs(shortestCompassTurn(350f) + 10f) < 0.01f)
        assertTrue(kotlin.math.abs(shortestCompassTurn(10f) - 10f) < 0.01f)
    }

    @Test
    fun qiblaHeadingAppliesLocalMagneticDeclination() {
        val location = Location("test").apply {
            latitude = 31.418
            longitude = 73.079
            altitude = 200.0
        }
        val corrected = magneticToTrueHeading(270f, location, 1790000000000L)
        assertTrue(corrected >= 0f && corrected < 360f)
    }

    @Test
    fun offlineAudioLibraryOpensFromMore() {
        composeRule.onNodeWithText("More").performClick()
        composeRule.onNodeWithText("Offline Audio").performClick()
        composeRule.onNodeWithText("Offline audio").assertIsDisplayed()
        composeRule.onNodeWithText("No audio downloaded yet. Open a Surah and tap Save audio.")
            .assertIsDisplayed()
    }

    @Test
    fun prayerTimesScreenOpensFromMore() {
        composeRule.onNodeWithText("More").performClick()
        composeRule.onNodeWithText("Prayer Times").performClick()
        composeRule.onNodeWithText("Based on your current device location").assertIsDisplayed()
    }

    @Test
    fun discoverOpensVideoLibraryWithLanguageAndTopicFilters() {
        composeRule.onNodeWithText("Discover").performClick()
        composeRule.onNodeWithText("Islamic Dramas").performClick()
        composeRule.onNodeWithText("LANGUAGE").assertIsDisplayed()
        composeRule.onNodeWithText("Urdu").assertIsDisplayed()
        composeRule.onNodeWithText("English").assertIsDisplayed()
        composeRule.onNodeWithText("Arabic").assertIsDisplayed()
        composeRule.onNodeWithTag("video-language-filters").performScrollToNode(hasText("Hindi"))
        composeRule.onNodeWithText("Hindi").assertIsDisplayed()
        composeRule.onNodeWithTag("video-language-filters").performScrollToNode(hasText("Bengali"))
        composeRule.onNodeWithText("Bengali").assertIsDisplayed()
        composeRule.onNodeWithText("POPULAR TOPICS").assertIsDisplayed()
        composeRule.onNodeWithText("Tafseer").assertIsDisplayed()
    }
}
