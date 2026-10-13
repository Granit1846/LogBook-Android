package com.example.logbook.ui.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/** Stores each screen's scale separately for each profile, including fresh app launches. */
@Composable
fun rememberPersistentZoomState(profileId: Long, screen: String): MutableState<Float> {
    val context = LocalContext.current.applicationContext
    val preview = LocalInspectionMode.current
    return remember(context, profileId, screen, preview) {
        if (preview) {
            mutableStateOf(1f)
        } else {
            val preferences = context.getSharedPreferences("table_zoom", Context.MODE_PRIVATE)
            PersistentZoomState(preferences, "profile_${profileId}_$screen")
        }
    }
}

private class PersistentZoomState(
    private val preferences: SharedPreferences,
    private val key: String
) : MutableState<Float> {
    private val state = mutableStateOf(preferences.getFloat(key, 1f).coerceIn(MIN_ZOOM, MAX_ZOOM))

    override var value: Float
        get() = state.value
        set(value) {
            if (!value.isFinite()) return
            val scale = value.coerceIn(MIN_ZOOM, MAX_ZOOM)
            if (scale != state.value) {
                state.value = scale
                preferences.edit().putFloat(key, scale).apply()
            }
        }

    override fun component1(): Float = value
    override fun component2(): (Float) -> Unit = { value = it }
}
