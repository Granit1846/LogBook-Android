package com.example.logbook

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.ProfileEntity
import com.example.logbook.data.model.MaintenanceJournal
import com.example.logbook.notifications.DefectNotifications
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MaintenanceUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var db: AppDatabase
    private var profileId = 0L
    private val name = "Журнал ${UUID.randomUUID().toString().take(8)}"

    @Before fun setup() {
        db = AppDatabase.getInstance(rule.activity)
        profileId = runBlocking { db.profileDao().insert(ProfileEntity(name = name, boardNumber = "ТЕСТ")) }
    }
    @After fun cleanup() { if (profileId != 0L) runBlocking {
        db.profileDao().getById(profileId)?.let { db.profileDao().delete(it) }
        DefectNotifications.refresh(rule.activity, db)
    } }

    @Test fun journalTabCanCreateAndCompleteDefect() {
        rule.waitUntil(5_000) { rule.onAllNodes(hasText(name)).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(name).performClick()
        rule.onNodeWithText("Журнал").performClick()
        rule.onNodeWithText("Периодический осмотр").assertIsDisplayed()
        rule.onNodeWithText("История").performClick()
        rule.onNodeWithText("Выполненные работы").assertIsDisplayed()
        rule.onNodeWithText("Закрыть").performClick()
        rule.onNodeWithContentDescription("Добавить").performClick()
        rule.onNodeWithText("Добавить работу").performClick()
        rule.onNodeWithText("Укажите работы").assertIsDisplayed()
        rule.onNodeWithText("Описание").assertDoesNotExist()
        rule.onNodeWithText("Укажите работы").performClick()
        val workOptions = rule.onAllNodes(hasText("Периодический осмотр"))
        workOptions[workOptions.fetchSemanticsNodes().lastIndex].performClick()
        rule.onNodeWithText("Дата:", substring = true).performClick()
        rule.onNodeWithText("Выбрать").assertIsDisplayed()
        rule.onNodeWithText("Выбрать").performClick()
        rule.onNodeWithText("Сохранить").performClick()
        rule.waitUntil(5_000) { runBlocking { MaintenanceJournal.works(db.profileDao().getById(profileId)!!).isNotEmpty() } }
        assertEquals("", runBlocking { MaintenanceJournal.works(db.profileDao().getById(profileId)!!).single().description })
        rule.onNodeWithContentDescription("Добавить").performClick()
        rule.onNodeWithText("Добавить неисправность").performClick()
        rule.onNodeWithText("Описание").performTextInput("Проверить течь")
        rule.onNodeWithText("Сохранить").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Проверить течь")).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(isToggleable())[0].performClick()
        rule.waitUntil(5_000) { runBlocking { MaintenanceJournal.openTasks(db.profileDao().getById(profileId)!!).isEmpty() } }
        assertEquals(1, runBlocking { MaintenanceJournal.tasks(db.profileDao().getById(profileId)!!).size })
    }
}
