package com.example.logbook

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logbook.data.exchange.*
import com.example.logbook.ui.settings.SyncConflictDialog
import com.google.gson.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun conflictActionsStayVisibleAndEachChoiceIsExplicit() {
        fun packet(name: String)=JsonObject().apply {
            add("tables",JsonObject().apply { SyncStore.TABLES.forEach { table -> add(table,JsonArray().apply {
                if(table=="profiles") repeat(40) { add(JsonObject().apply {addProperty("id","profile-$it");addProperty("name","$name $it")}) }
            }) } })
        }.toString()
        var choice: Boolean?=null
        compose.setContent { MaterialTheme { SyncConflictDialog(SyncConflict(packet("Android"),packet("Сервер"),3),false,"",{}, {choice=it}) } }
        compose.onNodeWithText("Оставить Android").assertIsDisplayed().performClick()
        compose.runOnIdle {assertEquals(true,choice)}
        compose.onNodeWithText("Загрузить серверную").assertIsDisplayed().performClick()
        compose.runOnIdle {assertEquals(false,choice)}
        compose.onNodeWithText("Отмена").assertIsDisplayed()
        compose.onNodeWithText("Изменения есть в обеих",substring=true).assertIsDisplayed()
    }
}
