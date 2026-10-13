package com.example.logbook.data.model

data class AggregateReplacement(
    val type: String = "",       // "Vr80", "Ta14", "Vk250001", "Vk250002"
    val oldNumber: String = "",
    val newNumber: String = "",
    val reason: String = "",
    val date: Long = 0,          // timestamp операции
    val installationDate: String = "" // дата установки, ISO; пусто в старой истории
)