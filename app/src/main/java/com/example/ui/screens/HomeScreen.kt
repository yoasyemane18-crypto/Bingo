package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.Screen
import com.example.model.UserProfile
import com.example.ui.components.CountdownCard
import com.example.ui.components.LuxuryButton
import com.example.ui.components.LuxuryOutlinedButton
import com.example.ui.theme.BingoBorderLight
import com.example.ui.theme.BingoBorderPurple
import com.example.ui.theme.BingoCardBg
import com.example.ui.theme.BingoGold
import com.example.ui.theme.BingoGoldLight
import com.example.ui.theme.BingoNight
import com.example.ui.theme.BingoPurple
import com.example.ui.theme.BingoPurpleDeep
import com.example.ui.theme.BingoPurpleVibrant
import com.example.ui.theme.BingoSurface
import com.example.ui.theme.BingoSurfaceLight
import com.example.ui.theme.BingoTextDark
import com.example.ui.theme.BingoTextMuted
import com.example.ui.theme.BingoTextWhite
import com.example.ui.theme.PlayfairFontFamily

@Composable
fun HomeScreen(
  countdownText: String,
  nextGameLabel: String,
  currentUser: UserProfile?,
  language: AppLanguage,
  onLogin: (String, String) -> Unit,
  onNavigate: (Screen) -> Unit,
  onStartFreePlay: () -> Unit,
  modifier: Modifier = Modifier
) {
  var usernameInput by remember { mutableStateOf("") }
  var passwordInput by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }

  val notificationText = when (language) {
    AppLanguage.EN -> "ⓘ BINGO game begins right after the countdown. Choose your cartelas and join!"
    AppLanguage.AM -> "ⓘ ቢንጎ ጨዋታ ከካውንትዳውን በኋላ ይጀምራል። ካርቴላዎን ይምረጡ እና ይቀላቀሉ!"
    AppLanguage.TI -> "ⓘ ናይ ቢንጎ ጸወታ ድሕሪ እቲ ቆጸራ ይጅምር። ካርቴላኹም ሕረዩ ከምኡውን ተሳተፉ!"
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(BingoNight)
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Countdown Card & Notice
    CountdownCard(
      countdownText = countdownText,
      label = if (nextGameLabel.isNotEmpty()) {
        when (language) {
          AppLanguage.EN -> "Next Game: BINGO Game at $nextGameLabel"
          AppLanguage.AM -> "ቀጣይ ጨዋታ፡ የቢንጎ ጨዋታ በ $nextGameLabel"
          AppLanguage.TI -> "ዝቕጽል ጸወታ፡ ናይ ቢንጎ ጸወታ ብ $nextGameLabel"
        }
      } else "",
      notificationNotice = notificationText
    )

    Spacer(modifier = Modifier.height(18.dp))

    // Main Content: User Card or Login Form
    if (currentUser != null) {
      // Logged-in User Profile Card
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BingoCardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, BingoGold.copy(alpha = 0.5f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("user_welcome_card")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(64.dp)
              .clip(CircleShape)
              .background(BingoPurpleDeep)
              .border(1.5.dp, BingoGold, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = "User profile",
              tint = BingoGold,
              modifier = Modifier.size(36.dp)
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = when (language) {
              AppLanguage.EN -> "Welcome, ${currentUser.username}"
              AppLanguage.AM -> "እንኳን ደህና መጡ፣ ${currentUser.username}"
              AppLanguage.TI -> "እንኳዕ ደሓን መጻእኩም፣ ${currentUser.username}"
            },
            fontFamily = PlayfairFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = BingoGoldLight,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = when (language) {
              AppLanguage.EN -> "Your playing time: ${currentUser.playingTime}"
              AppLanguage.AM -> "የእርስዎ የመጫወቻ ሰዓት፡ ${currentUser.playingTime}"
              AppLanguage.TI -> "ናይ ጸወታኹም ሰዓት፡ ${currentUser.playingTime}"
            },
            color = BingoTextMuted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = when (language) {
              AppLanguage.EN -> "Wallet: ${currentUser.walletBalance} birr"
              AppLanguage.AM -> "ቦርሳ፡ ${currentUser.walletBalance} ብር"
              AppLanguage.TI -> "ቦርሳ፡ ${currentUser.walletBalance} ብር"
            },
            color = BingoGold,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(20.dp))

          LuxuryButton(
            text = when (language) {
              AppLanguage.EN -> "▶ Play now"
              AppLanguage.AM -> "▶ አሁን ተጫወት"
              AppLanguage.TI -> "▶ ሕጂ ጻወት"
            },
            onClick = { onNavigate(Screen.PLAY_LOBBY) },
            isGold = false,
            testTag = "home_play_now_button"
          )

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = when (language) {
              AppLanguage.EN -> "⚡ Game starts automatically at 00:00:00 for all players"
              AppLanguage.AM -> "⚡ ቆጣሪው 00:00:00 ሲደርስ ጨዋታው ለሁሉም ተጫዋቾች ራሱ ይጀምራል"
              AppLanguage.TI -> "⚡ ቆጻሪ 00:00:00 ምስ በጽሐ ጸወታ ንኹሎም ተጻወትቲ ባዕሉ ይጅምር"
            },
            color = BingoGoldLight,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
          )
        }
      }
    } else {
      // Guest Login Card
      Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = BingoCardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, BingoGold.copy(alpha = 0.35f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("login_card")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(22.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(BingoPurpleDeep)
              .border(1.dp, BingoGold.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = "User icon",
              tint = BingoGold,
              modifier = Modifier.size(32.dp)
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = when (language) {
              AppLanguage.EN -> "⇥ Sign In"
              AppLanguage.AM -> "⇥ ግባ"
              AppLanguage.TI -> "⇥ እቶ"
            },
            fontFamily = PlayfairFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = BingoGoldLight
          )

          Spacer(modifier = Modifier.height(16.dp))

          OutlinedTextField(
            value = usernameInput,
            onValueChange = { usernameInput = it },
            label = {
              Text(
                when (language) {
                  AppLanguage.EN -> "Username"
                  AppLanguage.AM -> "የተጠቃሚ ስም"
                  AppLanguage.TI -> "ስም ተጠቃሚ"
                }
              )
            },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("login_username_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = BingoGold,
              unfocusedBorderColor = BingoBorderLight,
              focusedLabelColor = BingoGold,
              unfocusedLabelColor = BingoTextMuted,
              focusedTextColor = BingoTextWhite,
              unfocusedTextColor = BingoTextWhite
            ),
            shape = RoundedCornerShape(12.dp)
          )

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            label = {
              Text(
                when (language) {
                  AppLanguage.EN -> "Password"
                  AppLanguage.AM -> "የይለፍ ቃል"
                  AppLanguage.TI -> "መሕለፊ ቃል"
                }
              )
            },
            singleLine = true,
            trailingIcon = {
              IconButton(
                onClick = { passwordVisible = !passwordVisible },
                modifier = Modifier.testTag("home_password_visibility_toggle")
              ) {
                Icon(
                  imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = if (passwordVisible) "Hide password" else "Show password",
                  tint = BingoGoldLight
                )
              }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("login_password_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = BingoGold,
              unfocusedBorderColor = BingoBorderLight,
              focusedLabelColor = BingoGold,
              unfocusedLabelColor = BingoTextMuted,
              focusedTextColor = BingoTextWhite,
              unfocusedTextColor = BingoTextWhite
            ),
            shape = RoundedCornerShape(12.dp)
          )

          Spacer(modifier = Modifier.height(18.dp))

          LuxuryButton(
            text = when (language) {
              AppLanguage.EN -> "⇥ Sign In"
              AppLanguage.AM -> "⇥ ግባ"
              AppLanguage.TI -> "⇥ እቶ"
            },
            onClick = { onLogin(usernameInput, passwordInput) },
            isGold = false,
            testTag = "submit_login_button"
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Register Button (Golden)
      LuxuryButton(
        text = when (language) {
          AppLanguage.EN -> "✚ Register"
          AppLanguage.AM -> "✚ ተመዝገብ"
          AppLanguage.TI -> "✚ ተመዝገብ"
        },
        onClick = { onNavigate(Screen.REGISTER) },
        isGold = true,
        testTag = "navigate_register_button"
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Play For Free (Outline)
    LuxuryOutlinedButton(
      text = when (language) {
        AppLanguage.EN -> "▶ Play For Free"
        AppLanguage.AM -> "▶ በነጻ ተጫወት"
        AppLanguage.TI -> "▶ ብነጻ ጻወት"
      },
      onClick = onStartFreePlay,
      testTag = "play_free_button"
    )

    Spacer(modifier = Modifier.height(30.dp))
  }
}
