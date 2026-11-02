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
    fun discoverShowsHonestEmptyStateInsteadOfDeadEnd() {
        composeRule.onNodeWithText("Discover").performClick()
        composeRule.onNodeWithText("Islamic Dramas").performClick()
        composeRule.onNodeWithText("Preparing this collection").assertIsDisplayed()
        composeRule.onNodeWithText(
            "There is no verified, embeddable playlist published in this category yet. We will not show guessed IDs or unverified video links."
        ).assertIsDisplayed()
    }
}
