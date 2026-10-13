package com.example.logbook.ui.util

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun Modifier.bringIntoViewOnFocus(): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val requestJob = remember { mutableStateOf<Job?>(null) }

    bringIntoViewRequester(requester).onFocusChanged { focus ->
        requestJob.value?.cancel()
        if (focus.hasFocus) {
            requestJob.value = scope.launch {
                requester.bringIntoView()
                delay(300)
                requester.bringIntoView()
            }
        }
    }
}
