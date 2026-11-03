package com.noorehidayat.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
    fun homeScreenShowsDailyReminderAndMainNavigation() {
        composeRule.onNodeWithText("NOOR-E-HIDAYAT").assertExists()
        composeRule.onNodeWithText("DAILY REMINDER").assertExists()
        composeRule.onNodeWithText("Quran Tilawat").assertExists()
        composeRule.onNodeWithText("Discover").assertExists()
    }

    @Test
    fun learningGuideOpensFromMoreTab() {
        composeRule.onNodeWithText("More").performClick()
        composeRule.onNodeWithText("Learning").performClick()
        composeRule.onNodeWithText("Quran learning").assertExists()
        composeRule.onNodeWithText("1. Arabic alphabet").assertExists()
        composeRule.onNodeWithText("5. Daily revision").assertExists()
    }

    @Test
    fun quizCanAnswerAndAdvanceToNextQuestion() {
        composeRule.onNodeWithText("More").performClick()
        composeRule.onNodeWithText("Quiz").performClick()
        composeRule.onNodeWithText("How many daily obligatory prayers are there?")
            .assertExists()
        composeRule.onNodeWithText("Five").performClick()
        composeRule.onNodeWithText("Correct answer.").assertExists()
        composeRule.onNodeWithText("Next question").performClick()
        composeRule.onNodeWithText("Which month do Muslims fast?").assertExists()
    }

    @Test
    fun settingsClearlyDisclosesTranslationStatus() {
        composeRule.onNodeWithText("More").performClick()
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("App language").assertExists()
        composeRule.onNodeWithText("English").assertExists()
        composeRule.onNodeWithText(
            "Language choice is saved; full interface translations are not yet implemented."
        ).assertExists()
    }

    @Test
    fun mediaBackReturnsToTheScreenThatOpenedIt() {
        composeRule.onNodeWithText("Discover").performClick()
        composeRule.onNodeWithText("Islamic Dramas").performClick()
        composeRule.onNodeWithText("Browse on YouTube").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Browse this category").assertExists()
    }

    @Test
    fun discoverShowsHonestEmptyStateInsteadOfDeadEnd() {
        composeRule.onNodeWithText("Discover").performClick()
        composeRule.onNodeWithText("Islamic Dramas").performClick()
        composeRule.onNodeWithText("Browse this category").assertExists()
        composeRule.onNodeWithText("Browse on YouTube").assertExists()
        composeRule.onNodeWithText(
            "No verified playlist is configured yet. Browse YouTube results inside Noor-e-Hidayat and choose a video. Results are provided by YouTube and are not individually curated by this app."
        ).assertExists()
    }
}
