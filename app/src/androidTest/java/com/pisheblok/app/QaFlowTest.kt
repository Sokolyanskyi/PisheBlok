package com.pisheblok.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pisheblok.app.data.model.Section
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * End-to-end UI tests running on a real device against the real local database.
 * All created data is prefixed with "QA_" and removed afterwards (both via UI
 * where it is part of the scenario and via the repository in the cleanup step).
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class QaFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun repo() = (
        androidx.test.platform.app.InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .applicationContext as PisheBlokApp
        ).container.repository

    @Before
    fun waitForMainScreen() {
        // the device profile is signed in; the Food tab renders the add tile
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("Добавить комнату").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText("Еда").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun cleanup() {
        runBlocking {
            val locs = repo().observeAllLocations().first()
            locs.filter { it.name.startsWith("QA_") }.forEach { repo().deleteLocationCascade(it) }
            val shopping = repo().observeShopping().first()
            shopping.filter { it.name.startsWith("QA_") }.forEach { repo().deleteShopping(it) }
        }
    }

    private fun addRoom(name: String) {
        rule.onNodeWithText("Добавить комнату").performClick()
        rule.onNodeWithTag("location_name_field").performTextInput(name)
        rule.onNodeWithText("Добавить").performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun t1_smokeLaunch() {
        rule.onNodeWithText("ПищеБлок").assertIsDisplayed()
        rule.onNodeWithText("Добавить комнату").assertIsDisplayed()
        rule.onNodeWithContentDescription("SHOPPING").assertIsDisplayed()
        rule.onNodeWithContentDescription("CHEM").assertIsDisplayed()
    }

    @Test
    fun t2_addAndDeleteRoom() {
        addRoom("QA_Kitchen")
        rule.onNodeWithText("QA_Kitchen").assertIsDisplayed()
        // long press -> edit dialog -> Delete -> confirm
        rule.onNodeWithText("QA_Kitchen").performTouchInput { longClick() }
        rule.onNodeWithText("Изменить").assertIsDisplayed()
        rule.onNodeWithText("Удалить").performClick()
        rule.onNodeWithText("Удалить «QA_Kitchen»?").assertIsDisplayed()
        rule.onNodeWithText("Удалить").performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("QA_Kitchen").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun t3_placeItemShoppingFlow() {
        addRoom("QA_Kitchen")
        rule.onNodeWithText("QA_Kitchen").performClick()
        // add storage place
        rule.onNodeWithText("Добавить место хранения").performClick()
        rule.onNodeWithTag("location_name_field").performTextInput("QA_Fridge")
        rule.onNodeWithText("Добавить").performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("QA_Fridge").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("QA_Fridge").performClick()
        // add item: qty 1, low threshold 2 -> running low
        rule.onNodeWithText("Добавить продукт").performClick()
        rule.onNodeWithTag("item_name_field").performTextInput("QA_Milk")
        rule.onNodeWithTag("item_min_field").performTextClearance()
        rule.onNodeWithTag("item_min_field").performTextInput("2")
        rule.onNodeWithText("Сохранить").performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("QA_Milk").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Заканчивается").assertIsDisplayed()
        // add to buy list with qty 3, urgent (default)
        rule.onNodeWithContentDescription("В список покупок").performClick()
        rule.onNodeWithTag("qty_field").performTextClearance()
        rule.onNodeWithTag("qty_field").performTextInput("3")
        rule.onNodeWithText("В список покупок").performClick()
        // switch to the Buy tab
        rule.onNodeWithContentDescription("SHOPPING").performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("QA_Milk").fetchSemanticsNodes().size >= 2
        }
        // item is listed in the urgent section with quantity 3
        rule.onNodeWithText("3 шт").assertIsDisplayed()
        // remove the buy entry (its delete icon + confirm)
        rule.onNodeWithContentDescription("Удалить").performClick()
        rule.onNodeWithText("Убрать «QA_Milk» из списка?").assertIsDisplayed()
        rule.onNodeWithText("Удалить").performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("3 шт").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun t4_searchShowsLocation() {
        addRoom("QA_Kitchen")
        rule.onNodeWithText("QA_Kitchen").performClick()
        rule.onNodeWithText("Добавить место хранения").performClick()
        rule.onNodeWithTag("location_name_field").performTextInput("QA_Fridge")
        rule.onNodeWithText("Добавить").performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("QA_Fridge").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("QA_Fridge").performClick()
        rule.onNodeWithText("Добавить продукт").performClick()
        rule.onNodeWithTag("item_name_field").performTextInput("QA_Milk")
        rule.onNodeWithText("Сохранить").performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("QA_Milk").fetchSemanticsNodes().isNotEmpty()
        }
        // search from the place screen
        rule.onNodeWithContentDescription("Поиск").performClick()
        rule.onNodeWithTag("search_field").performTextInput("QA_Milk")
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("QA_Kitchen › QA_Fridge").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("QA_Kitchen › QA_Fridge").assertIsDisplayed()
        rule.onNodeWithContentDescription("Поиск").performClick()
    }

    @Test
    fun t5_languageSwitch() {
        rule.onNodeWithContentDescription("Аккаунт").performClick()
        rule.onNodeWithText("Язык").performClick()
        rule.onNodeWithText("English").performClick()
        rule.onNodeWithText("ОК").performClick()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("Add room").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Account").performClick()
        rule.onNodeWithText("Language").performClick()
        rule.onNodeWithText("Русский").performClick()
        rule.onNodeWithText("ОК").performClick()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("Добавить комнату").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
