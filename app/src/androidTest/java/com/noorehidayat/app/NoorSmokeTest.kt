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
    fun homeScreenShowsDailyReminderAndMainNavigation() {
        composeRule.onNodeWithText("NOOR-E-HIDAYAT").assertIsDisplayed()
        composeRule.onNodeWithText("DAILY REMINDER").assertIsDisplayed()
        composeRule.onNodeWithText("Quran Tilawat").assertIsDisplayed()
        composeRule.onNodeWithText("Discover").assertIsDisplayed()
    }

    @Test
    fun learningGuideOpensFromMoreTab() {
        composeRule.onNodeWithText("More").performClick()
        composeRule.onNodeWithText("Learning").performClick()
        composeRule.onNodeWithText("Quran learning").assertIsDisplayed()
        composeRule.onNodeWithText("1. Arabic alphabet").assertIsDisplayed()
        composeRule.onNodeWithText("5. Daily revision").assertIsDisplayed()
    }

    @Test
    fun quizCanAnswerAndAdvanceToNextQuestion() {
        composeRule.onNodeWithText("More").performClick()
        composeRule.onNodeWithText("Quiz").performClick()
        composeRule.onNodeWithText("How many daily obligatory prayers are there?")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Five").performClick()
        composeRule.onNodeWithText("Correct answer.").assertIsDisplayed()
        composeRule.onNodeWithText("Next question").performClick()
        composeRule.onNodeWithText("Which month do Muslims fast?").assertIsDisplayed()
    }

    @Test
    fun settingsClearlyDisclosesTranslationStatus() {
        composeRule.onNodeWithText("More").performClick()
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("App language").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Language choice is saved; full interface translations are not yet implemented."
        ).assertIsDisplayed()
    }

    @Test
    fun mediaBackReturnsToTheScreenThatOpenedIt() {
        composeRule.onNodeWithText("Discover").performClick()
        composeRule.onNodeWithText("Islamic Dramas").performClick()
        composeRule.onNodeWithText("Browse on YouTube").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Browse this category").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Islamic content").assertIsDisplayed()
    }

    @Test
    fun discoverShowsHonestEmptyStateInsteadOfDeadEnd() {
        composeRule.onNodeWithText("Discover").performClick()
        composeRule.onNodeWithText("Islamic Dramas").performClick()
        composeRule.onNodeWithText("Browse this category").assertIsDisplayed()
        composeRule.onNodeWithText("Browse on YouTube").assertIsDisplayed()
        composeRule.onNodeWithText(
            "No verified playlist is configured yet. Browse YouTube results inside Noor-e-Hidayat and choose a video. Results are provided by YouTube and are not individually curated by this app."
        ).assertIsDisplayed()
    }
}
