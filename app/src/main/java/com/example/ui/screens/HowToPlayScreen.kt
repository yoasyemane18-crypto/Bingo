package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.ui.theme.BingoCardBg
import com.example.ui.theme.BingoGold
import com.example.ui.theme.BingoGoldLight
import com.example.ui.theme.BingoNight
import com.example.ui.theme.BingoTextMuted
import com.example.ui.theme.BingoTextWhite
import com.example.ui.theme.PlayfairFontFamily

@Composable
fun HowToPlayScreen(
  language: AppLanguage,
  modifier: Modifier = Modifier
) {
  val rules = when (language) {
    AppLanguage.EN -> listOf(
      "1. Register and choose your playing time, or tap Play For Free for instant practice.",
      "2. Pay 30 birr per cartela. Each cartela is valid for 3 games played one after another in your playing time on the same day (10 birr per game). Payments below 30 birr are not confirmed.",
      "3. Tap one or more card numbers (1–500). Each cartela covers 3 games in a row. Columns are B (1–15), I (16–30), N (31–45), G (46–60), and O (61–75). The center star (★) is free.",
      "4. The games start automatically for everyone when the countdown ends. All players receive the same called numbers.",
      "5. Tap a called number on your card to mark it, or turn on Auto-mark.",
      "6. Complete the Target Goal pattern and press the glowing Bingo! button to win!"
    )
    AppLanguage.AM -> listOf(
      "1. ተመዝግበው የመጫወቻ ሰዓትዎን ይምረጡ፣ ወይም በነጻ ተጫወት ይጫኑ።",
      "2. ለአንድ ካርቴላ 30 ብር ይክፈሉ። እያንዳንዱ ካርቴላ በመረጡት የመጫወቻ ሰዓት በዚያው ቀን አንድ ከአንዱ በኋላ ለሚካሄዱ 3 ጨዋታዎች ያገለግላል (በጨዋታ 10 ብር)።",
      "3. አንድ ወይም ከዚያ በላይ የካርድ ቁጥሮችን (1–500) ይንኩ። ዓምዶቹ B (1–15)፣ I (16–30)፣ N (31–45)፣ G (46–60) እና O (61–75) ናቸው። መሃሉ ነጻ (★) ነው።",
      "4. ቆጠራው ሲያበቃ ጨዋታዎቹ ለሁሉም ሰው በራስ-ሰር ይጀምራሉ፤ 3 ጨዋታዎች አንድ ከአንዱ በኋላ ይካሄዳሉ።",
      "5. በካርድዎ ላይ የተጠራውን ቁጥር ለማመልከት ይንኩት፣ ወይም ራስ-ሰር ምልክትን ያብሩ።",
      "6. የዒላማ ግቡን ይሙሉ እና ቢንጎ! ይጫኑ!"
    )
    AppLanguage.TI -> listOf(
      "1. ተመዝግቡ ናይ ጸወታ ሰዓትኩም ሕረዩ፣ ወይ ብነጻ ጻወት ጠውቑ።",
      "2. ንሓደ ካርቴላ 30 ብር ክፈሉ። ነፍሲ ወከፍ ካርቴላ ኣብ ዝሓረኹሞ ናይ ጸወታ ሰዓት ኣብታ ዕለት ሓደ ድሕሪ ሓደ ንዝካየዱ 3 ጸወታታት ይሰርሕ።",
      "3. ሓደ ወይ ልዕሊኡ ቁጽሪ ካርድ (1–500) ጠውቑ። ዓምዲታት B (1–15)፣ I (16–30)፣ N (31–45)፣ G (46–60) ከምኡውን O (61–75) እዮም። ማእከል ነጻ (★) እዩ።",
      "4. እቲ ቆጸራ ምስ ተወዲአ እቶም ጸወታታት ንኹሉ ብባዕሎም ይጅምሩ።",
      "5. ኣብ ካርድኩም ዝተጸውዐ ቁጽሪ ንምልክት ጠውቑዎ፣ ወይ ባዕሉ ምልክት ምግባር ኣብርሁ።",
      "6. ዕላማ ኣማልኡ ከምኡውን ቢንጎ! ጠውቑ!"
    )
  }

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
        .testTag("how_to_play_card")
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          text = when (language) {
            AppLanguage.EN -> "How to Play"
            AppLanguage.AM -> "አጨዋወት"
            AppLanguage.TI -> "ከመይ ይጻወት"
          },
          fontFamily = PlayfairFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 26.sp,
          color = BingoGoldLight
        )

        Spacer(modifier = Modifier.height(16.dp))

        rules.forEach { rule ->
          Text(
            text = rule,
            fontSize = 15.sp,
            color = BingoTextWhite,
            lineHeight = 22.sp,
            modifier = Modifier.padding(vertical = 6.dp)
          )
        }
      }
    }
  }
}
