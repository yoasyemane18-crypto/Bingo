package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ActivityRecord(
  val id: String,
  val type: String,
  val username: String,
  val amountBirr: Int = 0,
  val description: String,
  val extra: String = "",
  val timestamp: Long = System.currentTimeMillis()
) {
  fun formatTime(): String {
    return try {
      val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
      sdf.format(Date(timestamp))
    } catch (_: Exception) {
      ""
    }
  }
}
