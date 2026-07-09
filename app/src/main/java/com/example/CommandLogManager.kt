package com.example

import kotlinx.coroutines.flow.MutableStateFlow

object CommandLogManager {
    val logs = MutableStateFlow<List<CommandLog>>(emptyList())
    
    fun addLog(key: String, status: String) {
        val current = logs.value.toMutableList()
        current.add(0, CommandLog(System.currentTimeMillis(), key, status))
        logs.value = current.take(50) // Keep the last 50 events
    }
}

data class CommandLog(
    val timestamp: Long,
    val key: String,
    val status: String
)
