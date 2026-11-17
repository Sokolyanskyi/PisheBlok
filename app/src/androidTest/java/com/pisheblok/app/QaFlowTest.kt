package com.pisheblok.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
import com.pisheblok.app.ui.theme.ThemePrefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end UI tests running on a real device against the real local database.
 * All created data is prefixed with "QA_" and removed afterwards.
 *
 * Notes on stability:
 *  - "No compose hierarchies" may be thrown while the app process is still starting;
 *    every polling condition therefore wraps lookups in try/catch.
 *  - Some dialog buttons live in the unmerged tree, so text lookups use useUnmergedTree.
 */
@RunWith(AndroidJUnit4::class)
class QaFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun repo() = (
        androidx.test.platform.app.InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .applicationContext as PisheBlokApp
        ).container.repository

    /** Count of nodes matching text; safe to call while the app is starting. */
    private fun count(text: String): Int = try {
        rule.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().size
    } catch (_: IllegalStateException) {
        0
    } catch (_: RuntimeException) {
        0
    }

    private fun countTag(tag: String): Int = try {
        rule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size
    } catch (_: IllegalStateException) {
        0
    } catch (_: RuntimeException) {
        0
    }

    private fun waitFor(vararg conditions: () -> Int, timeoutMs: Long = 20_000) {
        rule.waitUntil(timeoutMs) { conditions.any { it() > 0 } }
    }

    private fun waitForGone(text: String, timeoutMs: Long = 15_000) {
        rule.waitUntil(timeoutMs) { count(text) == 0 }
    }

    @Before
    fun setUp() {
        waitFor(
            { count("Добавить комнату") },
            { count("Еда") },
            { countTag("auth_email_field") },
        )
        if (countTag("auth_email_field") > 0) {
            rule.onNodeWithTag("auth_email_field", useUnmergedTree = true)
                .performTextInput("9riksok@gmail.com")
            rule.onNodeWithTag("auth_password_field", useUnmergedTree = true)
                .performTextInput("Spameripidori23")
            rule.onNodeWithTag("auth_login_btn", useUnmergedTree = true).performClick()
            waitFor(
                { count("Добавить комнату") },
                { count("Еда") },
                { count("Вход") },
            )
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
        ThemePrefs.save(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext,
            com.pisheblok.app.ui.theme.ThemeScheme.TERRACOTTA,
        )
    }

    private fun addRoom(name: String) {
        rule.onNodeWithText("Добавить комнату", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("location_name_field", useUnmergedTree = true).performTextInput(name)
        rule.onNodeWithText("Добавить", useUnmergedTree = true).performClick()
        waitFor({ count(name) })
    }

    @Test
    fun t1_smokeLaunch() {
        waitFor({ count("ПищеБлок") })
        rule.onNodeWithText("ПищеБлок", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Купить", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Химия", useUnmergedTree = true).assertExists()
    }

    @Test
    fun t2_addAndDeleteRoom() {
        addRoom("QA_Kitchen")
        rule.onNodeWithText("QA_Kitchen", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("QA_Kitchen", useUnmergedTree = true).performTouchInput { longClick() }
        waitFor({ count("Удалить") })
        // Hold-to-confirm вместо диалога. При выключенных анимациях
        // (см. run: settings put global animator_duration_scale 0) hold
        // срабатывает мгновенно по нажатию.
        rule.onNodeWithText("Удалить", useUnmergedTree = true).performClick()
        waitForGone("QA_Kitchen")
    }

    @Test
    fun t3_placeItemShoppingFlow() {
        addRoom("QA_Kitchen")
        rule.onNodeWithText("QA_Kitchen", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Добавить место хранения", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("location_name_field", useUnmergedTree = true).performTextInput("QA_Fridge")
        rule.onNodeWithText("Добавить", useUnmergedTree = true).performClick()
        waitFor({ count("QA_Fridge") })
        rule.onNodeWithText("QA_Fridge", useUnmergedTree = true).performClick()

        rule.onNodeWithTag("add_item_fab", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("item_name_field", useUnmergedTree = true).performTextInput("QA_Milk")
        rule.onNodeWithTag("item_min_field", useUnmergedTree = true).performTextClearance()
        rule.onNodeWithTag("item_min_field", useUnmergedTree = true).performTextInput("2")
        rule.onNodeWithText("Сохранить", useUnmergedTree = true).performClick()
        waitFor({ count("QA_Milk") })

        // add to the buy list, qty 3, urgent (default)
        rule.onNodeWithContentDescription("В список покупок", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("qty_field", useUnmergedTree = true).performTextClearance()
        rule.onNodeWithTag("qty_field", useUnmergedTree = true).performTextInput("3")
        rule.onNodeWithText("В список покупок", useUnmergedTree = true).performClick()

        rule.onNodeWithContentDescription("Купить", useUnmergedTree = true).performClick()
        waitFor({ count("QA_Milk") })
        rule.waitUntil(15_000) { count("QA_Milk") >= 2 } // low row + buy row
        rule.onNodeWithText("3 шт", useUnmergedTree = true).assertExists()

        // Обратно на «Еду»: удаление товара — hold-to-confirm на строке
        // (при выключенных анимациях срабатывает мгновенно).
        rule.onNodeWithContentDescription("Еда", useUnmergedTree = true).performClick()
        waitFor({ count("QA_Milk") })
        rule.onNodeWithContentDescription("Удалить", useUnmergedTree = true).performClick()
        waitForGone("QA_Milk")
    }

    @Test
    fun t4_searchShowsLocation() {
        addRoom("QA_Kitchen")
        rule.onNodeWithText("QA_Kitchen", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Добавить место хранения", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("location_name_field", useUnmergedTree = true).performTextInput("QA_Fridge")
        rule.onNodeWithText("Добавить", useUnmergedTree = true).performClick()
        waitFor({ count("QA_Fridge") })
        rule.onNodeWithText("QA_Fridge", useUnmergedTree = true).performClick()

        rule.onNodeWithTag("add_item_fab", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("item_name_field", useUnmergedTree = true).performTextInput("QA_Milk")
        rule.onNodeWithText("Сохранить", useUnmergedTree = true).performClick()
        waitFor({ count("QA_Milk") })

        rule.onNodeWithContentDescription("Поиск", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("search_field", useUnmergedTree = true).performTextInput("QA_Milk")
        waitFor({ count("QA_Kitchen › QA_Fridge") })
        rule.onNodeWithText("QA_Kitchen › QA_Fridge", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Поиск", useUnmergedTree = true).performClick()
    }

    @Test
    fun t5_languageSwitch() {
        rule.onNodeWithContentDescription("Аккаунт", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Язык", useUnmergedTree = true).performClick()
        rule.onNodeWithText("English", useUnmergedTree = true).performClick()
        rule.onNodeWithText("ОК", useUnmergedTree = true).performClick()
        waitFor({ count("Add room") })
        rule.onNodeWithContentDescription("Account", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Language", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Русский", useUnmergedTree = true).performClick()
        // Приложение пока в английской локали — кнопка подтверждения «OK».
        rule.waitUntil(10_000) { count("ОК") > 0 || count("OK") > 0 }
        val okText = if (count("ОК") > 0) "ОК" else "OK"
        rule.onNodeWithText(okText, useUnmergedTree = true).performClick()
        waitFor({ count("Добавить комнату") })
    }
}
