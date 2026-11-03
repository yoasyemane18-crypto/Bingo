package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.StringsDictionary
import com.example.ui.theme.BingoBorderLight
import com.example.ui.theme.BingoBorderPurple
import com.example.ui.theme.BingoCardBg
import com.example.ui.theme.BingoGold
import com.example.ui.theme.BingoGoldLight
import com.example.ui.theme.BingoNight
import com.example.ui.theme.BingoPurpleDeep
import com.example.ui.theme.BingoSurface
import com.example.ui.theme.BingoTextDark
import com.example.ui.theme.BingoTextMuted
import com.example.ui.theme.BingoTextWhite
import com.example.ui.theme.PlayfairFontFamily

@Composable
fun ScheduleScreen(
  language: AppLanguage,
  nextGameLabel: String,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(BingoNight)
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = BingoCardBg),
      border = androidx.compose.foundation.BorderStroke(1.dp, BingoGold.copy(alpha = 0.4f)),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("schedule_card")
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          text = when (language) {
            AppLanguage.EN -> "Schedule"
            AppLanguage.AM -> "የጊዜ ሰሌዳ"
            AppLanguage.TI -> "ናይ ግዜ ሰሌዳ"
          },
          fontFamily = PlayfairFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 26.sp,
          color = BingoGoldLight
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Table Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(BingoPurpleDeep, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = when (language) {
              AppLanguage.EN -> "Game"
              AppLanguage.AM -> "ጨዋታ"
              AppLanguage.TI -> "ጸወታ"
            },
            color = BingoGold,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
          Text(
            text = when (language) {
              AppLanguage.EN -> "Time"
              AppLanguage.AM -> "ሰዓት"
              AppLanguage.TI -> "ሰዓት"
            },
            color = BingoGold,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Table Rows
        StringsDictionary.SLOTS.forEach { slot ->
          val label = slot.label(language)
          val isNext = label.contains(nextGameLabel, ignoreCase = true) || nextGameLabel.contains(slot.labelEn, ignoreCase = true)

          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            color = if (isNext) BingoGold else BingoSurface,
            border = if (isNext) androidx.compose.foundation.BorderStroke(1.dp, BingoGoldLight) else null
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "BINGO Game",
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                color = if (isNext) BingoTextDark else BingoTextWhite,
                fontSize = 15.sp
              )
              Text(
                text = label,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                color = if (isNext) BingoTextDark else BingoTextWhite,
                fontSize = 15.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = when (language) {
            AppLanguage.EN -> "The highlighted row is the next upcoming game. Each playing time session includes 3 rounds played one after another."
            AppLanguage.AM -> "ጎልቶ የሚታየው ረድፍ ቀጣዩ ጨዋታ ነው። እያንዳንዱ የመጫወቻ ሰዓት አንድ ከአንዱ በኋላ የሚካሄዱ 3 ጨዋታዎችን ያካትታል።"
            AppLanguage.TI -> "እቲ ዝተመልከተ መስመር እቲ ዝቕጽል ጸወታ እዩ። ነፍሲ ወከፍ ናይ ጸወታ ሰዓት ሓደ ድሕሪ ሓደ ዝካየዱ 3 ጸወታታት የጠቓልል።"
          },
          fontSize = 13.sp,
          color = BingoTextMuted,
          lineHeight = 18.sp
        )
      }
    }
  }
}
