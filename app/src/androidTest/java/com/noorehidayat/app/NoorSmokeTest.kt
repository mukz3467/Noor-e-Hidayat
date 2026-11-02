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
    fun learningGuideOpensFromHome() {
        composeRule.onNodeWithText("Quran Learning").performClick()
        composeRule.onNodeWithText("Quran learning").assertIsDisplayed()
        composeRule.onNodeWithText("1. Arabic alphabet").assertIsDisplayed()
    }

    @Test
    fun quizOpensAndShowsFirstQuestion() {
        composeRule.onNodeWithText("Islamic Quiz").performClick()
        composeRule.onNodeWithText("How many daily obligatory prayers are there?")
            .assertIsDisplayed()
    }

    @Test
    fun discoverShowsHonestEmptyStateInsteadOfDeadEnd() {
        composeRule.onNodeWithText("Discover").performClick()
        composeRule.onNodeWithText("Islamic Dramas").performClick()
        composeRule.onNodeWithText("Preparing this collection").assertIsDisplayed()
    }
}
