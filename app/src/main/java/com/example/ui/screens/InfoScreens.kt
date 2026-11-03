package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.ui.theme.BingoBorderPurple
import com.example.ui.theme.BingoCardBg
import com.example.ui.theme.BingoGold
import com.example.ui.theme.BingoGoldLight
import com.example.ui.theme.BingoNight
import com.example.ui.theme.BingoSurface
import com.example.ui.theme.BingoTextMuted
import com.example.ui.theme.BingoTextWhite
import com.example.ui.theme.PlayfairFontFamily

@Composable
fun AboutUsScreen(language: AppLanguage, modifier: Modifier = Modifier) {
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
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(22.dp)) {
        Text(
          text = when (language) {
            AppLanguage.EN -> "About Us"
            AppLanguage.AM -> "ስለ እኛ"
            AppLanguage.TI -> "ብዛዕባና"
          },
          fontFamily = PlayfairFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 26.sp,
          color = BingoGoldLight
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = when (language) {
            AppLanguage.EN -> "YEME BINGO is a premier Ethiopian bingo gaming platform with scheduled game times. Pick your cartelas, join live games with other players, mark the balls, and compete for victory. The first player to complete the target goal and claim Bingo wins!"
            AppLanguage.AM -> "YEME ቢንጎ በጊዜ ሰሌዳ የሚካሄድ የቢንጎ ጨዋታ ነው። ካርቴላዎችዎን ይምረጡ፣ ጨዋታዎን ይቀላቀሉ እና ከሌሎች ተጫዋቾች ጋር ይጫወቱ። ግቡን ሞልቶ ቢንጎ የሚል የመጀመሪያው ተጫዋች ያሸንፋል!"
            AppLanguage.TI -> "YEME ቢንጎ ብሰሌዳ ዝካየድ ናይ ቢንጎ ጸወታ እዩ። ካርቴላታትኩም ሕረዩ፣ ኣብ ጸወታኹም ተሳተፉ ከምኡውን ምስ ካልኦት ተጻዋትቲ ተጻወቱ። ዕላማ ዘማልአ ቢንጎ ዝጠወቐ ቀዳማይ ተጻዋታይ ይስዕር!"
          },
          fontSize = 15.sp,
          color = BingoTextWhite,
          lineHeight = 22.sp
        )
      }
    }
  }
}

@Composable
fun ContactUsScreen(language: AppLanguage, modifier: Modifier = Modifier) {
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
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(22.dp)) {
        Text(
          text = when (language) {
            AppLanguage.EN -> "Contact Us"
            AppLanguage.AM -> "ያግኙን"
            AppLanguage.TI -> "ርኸቡና"
          },
          fontFamily = PlayfairFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 26.sp,
          color = BingoGoldLight
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Customer Support & Telebirr Inquiries:\n" +
            "• Name: Yemane\n" +
            "• Telebirr Number: 0939383425\n" +
            "• Email: support@yemebingo.com\n" +
            "• Hours: Daily 8:00 AM - 10:00 PM (Ethiopian Time)",
          fontSize = 15.sp,
          color = BingoTextWhite,
          lineHeight = 24.sp
        )
      }
    }
  }
}

@Composable
fun FaqsScreen(language: AppLanguage, modifier: Modifier = Modifier) {
  val faqItems = when (language) {
    AppLanguage.EN -> listOf(
      Pair("Is it free to play?", "Yes! Tap 'Play For Free' from the home screen to enjoy unlimited practice bingo with no account required."),
      Pair("How much is a cartela?", "30 birr for one cartela, valid for 3 games played one after another in your playing time on the same day (10 birr per game). 60 birr for two cartelas, 90 birr for three, and so on."),
      Pair("Do I need an account?", "An account lets you buy cartelas, maintain your Telebirr wallet, and join the official scheduled games for cash rewards."),
      Pair("How do I win?", "Mark the called numbers matching the Target Goal pattern (e.g. Any Line, Four Corners, Full House) and press the glowing Bingo! button.")
    )
    AppLanguage.AM -> listOf(
      Pair("መጫወት ነጻ ነው?", "አዎ! በነጻ ለመለማመድ መለያ ሳያስፈልግዎት 'በነጻ ተጫወት'ን ይጫኑ።"),
      Pair("ካርቴላ ስንት ነው?", "አንድ ካርቴላ 30 ብር ሲሆን በመረጡት የመጫወቻ ሰዓት በዚያው ቀን አንድ ከአንዱ በኋላ ለሚካሄዱ 3 ጨዋታዎች ያገለግላል።"),
      Pair("መለያ ያስፈልገኛል?", "መለያ ካርቴላዎችን እንዲገዙ፣ ቦርሳ እንዲይዙ እና በጊዜ ሰሌዳ የተያዙ ጨዋታዎችን እንዲቀላቀሉ ያስችልዎታል።"),
      Pair("እንዴት አሸንፋለሁ?", "የተጠሩትን ቁጥሮች ምልክት ያድርጉ፣ የዒላማ ግቡን ይሙሉ፣ ከዚያ ቢንጎ! ይጫኑ።")
    )
    AppLanguage.TI -> listOf(
      Pair("ምጽዋት ነጻ ድዩ?", "እወ! ብነጻ ንምልምማድ 'ብነጻ ጻወት' ጠውቑ።"),
      Pair("ካርቴላ ክንደይ እዩ?", "ሓደ ካርቴላ 30 ብር ኮይኑ ኣብ ዝሓረኹሞ ናይ ጸወታ ሰዓት ኣብታ ዕለት ሓደ ድሕሪ ሓደ ንዝካየዱ 3 ጸወታታት ይሰርሕ።"),
      Pair("ሕሳብ የድልየኒ ድዩ?", "ሕሳብ ካርቴላታት ክትዕድግ፣ ቦርሳ ክትሕዝን ኣብ ዝተወሰኑ ጸወታታት ክትሳተፍ የኽእለካ።"),
      Pair("ከመይ ኢለ ይስዕር?", "ዝተጸውዑ ቁጽርታት ምልክት ግበሩ፣ ዕላማ ኣማልኡ፣ ድሕሪኡ ቢንጎ! ጠውቑ።")
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
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(22.dp)) {
        Text(
          text = when (language) {
            AppLanguage.EN -> "FAQs"
            AppLanguage.AM -> "ተደጋጋሚ ጥያቄዎች"
            AppLanguage.TI -> "ዝተደጋገሙ ሕቶታት"
          },
          fontFamily = PlayfairFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 26.sp,
          color = BingoGoldLight
        )

        Spacer(modifier = Modifier.height(14.dp))

        faqItems.forEach { (q, a) ->
          var expanded by remember { mutableStateOf(false) }

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp)
              .background(BingoSurface, RoundedCornerShape(12.dp))
              .clickable { expanded = !expanded }
              .padding(14.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = q,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = BingoGold,
                modifier = Modifier.weight(1f)
              )
              Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = BingoGold
              )
            }

            AnimatedVisibility(visible = expanded) {
              Column {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = a,
                  fontSize = 14.sp,
                  color = BingoTextWhite,
                  lineHeight = 20.sp
                )
              }
            }
          }
        }
      }
    }
  }
}
