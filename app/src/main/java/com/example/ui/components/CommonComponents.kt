package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AppLanguage
import com.example.model.Screen
import com.example.ui.theme.BingoBorderLight
import com.example.ui.theme.BingoBorderPurple
import com.example.ui.theme.BingoCardBg
import com.example.ui.theme.BingoCoral
import com.example.ui.theme.BingoGold
import com.example.ui.theme.BingoGoldDark
import com.example.ui.theme.BingoGoldLight
import com.example.ui.theme.BingoGreen
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
import kotlin.random.Random

@Composable
fun YemeTopBar(
  currentLanguage: AppLanguage,
  isDrawerOpen: Boolean,
  onLanguageClick: () -> Unit,
  onToggleDrawer: () -> Unit,
  onBrandClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val barBrush = Brush.horizontalGradient(
    colors = listOf(BingoPurpleDeep, Color(0xFF6D28D9), BingoPurpleDeep)
  )

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .shadow(8.dp),
    color = Color.Transparent
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(barBrush)
        .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Brand Title
        Row(
          modifier = Modifier
            .clickable { onBrandClick() }
            .testTag("brand_title"),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "▦ YEME BINGO",
            fontFamily = PlayfairFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = BingoGoldLight,
            letterSpacing = 0.5.sp
          )
        }

        // Actions: Language Toggle & Menu Hamburger
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Language Switcher Pill
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .clickable { onLanguageClick() }
              .border(1.dp, BingoGold.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
              .testTag("language_switch_button"),
            color = Color.Transparent
          ) {
            Text(
              text = currentLanguage.shortLabel,
              color = BingoGold,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }

          // Drawer Toggle Button
          IconButton(
            onClick = onToggleDrawer,
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(if (isDrawerOpen) BingoGold else BingoSurface)
              .border(1.dp, BingoGold.copy(alpha = 0.4f), CircleShape)
              .testTag("menu_toggle_button")
          ) {
            Icon(
              imageVector = if (isDrawerOpen) Icons.Default.Close else Icons.Default.Menu,
              contentDescription = if (isDrawerOpen) "Close menu" else "Open menu",
              tint = if (isDrawerOpen) BingoTextDark else BingoGold
            )
          }
        }
      }
    }
  }
}

@Composable
fun YemeDrawer(
  currentScreen: Screen,
  username: String?,
  currentLanguage: AppLanguage,
  onLanguageSelected: (AppLanguage) -> Unit,
  onNavigate: (Screen) -> Unit,
  onLogout: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .width(290.dp)
      .fillMaxSize(),
    color = BingoNight,
    shadowElevation = 16.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(BingoPurpleDeep, BingoNight)
          )
        )
    ) {
      // Logo Header
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(BingoNight.copy(alpha = 0.8f))
          .padding(vertical = 24.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "YEME",
            fontFamily = PlayfairFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            color = BingoGoldLight,
            letterSpacing = 2.sp
          )
          Text(
            text = "BINGO",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = BingoGold,
            letterSpacing = 4.sp
          )
        }
      }

      // User Greeting or Guest
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(BingoSurface)
          .padding(vertical = 12.dp, horizontal = 16.dp)
      ) {
        Text(
          text = if (username != null) "◉ $username" else "☰ Menu",
          color = BingoGoldLight,
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp
        )
      }

      // Language Switcher Selector Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        AppLanguage.entries.forEach { lang ->
          val isSelected = currentLanguage == lang
          Surface(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .clickable { onLanguageSelected(lang) }
              .border(
                1.dp,
                if (isSelected) BingoGold else BingoBorderLight,
                RoundedCornerShape(8.dp)
              ),
            color = if (isSelected) BingoGold else BingoSurface
          ) {
            Text(
              text = lang.displayName,
              color = if (isSelected) BingoTextDark else BingoTextWhite,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(vertical = 8.dp)
            )
          }
        }
      }

      // Navigation Items List
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(vertical = 8.dp)
      ) {
        data class DrawerNavEntry(
          val screen: Screen?,
          val label: String,
          val isSignOut: Boolean = false,
          val onClick: (() -> Unit)? = null
        )

        val navItems = mutableListOf<DrawerNavEntry>()
        navItems.add(DrawerNavEntry(Screen.HOME, "⌂ Home"))

        if (username == null) {
          navItems.add(DrawerNavEntry(Screen.REGISTER, "✚ Register"))
          val signInLabel = when (currentLanguage) {
            AppLanguage.EN -> "⇥ Sign In"
            AppLanguage.AM -> "⇥ ግባ"
            AppLanguage.TI -> "⇥ እቶ"
          }
          navItems.add(DrawerNavEntry(Screen.SIGN_IN, signInLabel))
        } else {
          val signOutLabel = when (currentLanguage) {
            AppLanguage.EN -> "⇤ Sign Out"
            AppLanguage.AM -> "⇤ ውጣ"
            AppLanguage.TI -> "⇤ ውጻእ"
          }
          navItems.add(DrawerNavEntry(null, signOutLabel, isSignOut = true) {
            onLogout()
            onClose()
          })
        }

        navItems.add(DrawerNavEntry(Screen.SCHEDULE, "☰ Schedule"))
        navItems.add(DrawerNavEntry(Screen.HOW_TO_PLAY, "ⓘ How to Play"))
        navItems.add(DrawerNavEntry(Screen.ABOUT_US, "● About Us"))
        navItems.add(DrawerNavEntry(Screen.CONTACT_US, "✉ Contact Us"))
        navItems.add(DrawerNavEntry(Screen.FAQS, "? FAQs"))
        navItems.add(DrawerNavEntry(Screen.ADMIN, "⚙ Admin"))

        navItems.forEach { entry ->
          val isSelected = entry.screen != null && currentScreen == entry.screen
          val itemBg = if (isSelected) {
            Brush.horizontalGradient(listOf(BingoGold, BingoGoldDark))
          } else {
            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(itemBg)
              .clickable {
                if (entry.onClick != null) {
                  entry.onClick.invoke()
                } else if (entry.screen != null) {
                  onNavigate(entry.screen)
                  onClose()
                }
              }
              .padding(horizontal = 16.dp, vertical = 13.dp)
          ) {
            Text(
              text = entry.label,
              fontSize = 16.sp,
              fontWeight = if (isSelected || entry.isSignOut) FontWeight.Bold else FontWeight.Medium,
              color = when {
                isSelected -> BingoTextDark
                entry.isSignOut -> BingoCoral
                else -> BingoTextWhite
              }
            )
          }
        }
      }
    }
  }
}

@Composable
fun CountdownCard(
  countdownText: String,
  label: String,
  notificationNotice: String? = null,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    if (!notificationNotice.isNullOrEmpty()) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 10.dp),
        shape = RoundedCornerShape(10.dp),
        color = BingoSurfaceLight,
        border = androidx.compose.foundation.BorderStroke(1.dp, BingoBorderLight)
      ) {
        Text(
          text = notificationNotice,
          color = BingoGoldLight,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          lineHeight = 18.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(12.dp)
        )
      }
    }

    // Shimmering Golden Countdown Display
    Text(
      text = countdownText,
      fontFamily = PlayfairFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 42.sp,
      color = BingoGold,
      letterSpacing = (-1).sp,
      modifier = Modifier.padding(vertical = 4.dp)
    )

    if (label.isNotEmpty()) {
      Text(
        text = label,
        color = BingoTextMuted,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun LuxuryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isGold: Boolean = true,
  testTag: String = ""
) {
  val backgroundBrush = if (isGold) {
    Brush.horizontalGradient(listOf(BingoGold, BingoGoldDark))
  } else {
    Brush.horizontalGradient(listOf(BingoPurpleVibrant, BingoPurple))
  }

  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier
      .fillMaxWidth()
      .height(54.dp)
      .testTag(testTag),
    shape = RoundedCornerShape(14.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = if (isGold) BingoGold else BingoPurpleVibrant,
      contentColor = if (isGold) BingoTextDark else BingoTextWhite,
      disabledContainerColor = BingoSurface,
      disabledContentColor = BingoTextMuted
    ),
    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
  ) {
    Text(
      text = text,
      fontWeight = FontWeight.Bold,
      fontSize = 18.sp,
      letterSpacing = 0.5.sp
    )
  }
}

@Composable
fun LuxuryOutlinedButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  testTag: String = ""
) {
  OutlinedButton(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier
      .fillMaxWidth()
      .height(52.dp)
      .testTag(testTag),
    shape = RoundedCornerShape(50.dp),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, BingoGold),
    colors = ButtonDefaults.outlinedButtonColors(
      contentColor = BingoGold
    )
  ) {
    Text(
      text = text,
      fontWeight = FontWeight.Bold,
      fontSize = 16.sp
    )
  }
}

// Confetti Particle Effect
private data class Particle(
  val x: Float,
  val initialY: Float,
  val color: Color,
  val size: Float,
  val speed: Float
)

@Composable
fun ConfettiOverlay(
  isActive: Boolean,
  modifier: Modifier = Modifier
) {
  if (!isActive) return

  val progress = remember { Animatable(0f) }
  val particles = remember {
    val colors = listOf(BingoGold, BingoGoldLight, BingoPurpleVibrant, BingoGreen, Color(0xFFFFF3C9))
    List(40) {
      Particle(
        x = Random.nextFloat(),
        initialY = Random.nextFloat() * -0.5f,
        color = colors[Random.nextInt(colors.size)],
        size = Random.nextFloat() * 12f + 8f,
        speed = Random.nextFloat() * 0.8f + 0.6f
      )
    }
  }

  LaunchedEffect(isActive) {
    progress.snapTo(0f)
    progress.animateTo(
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
        animation = tween(3000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
      )
    )
  }

  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    for (p in particles) {
      val curY = ((p.initialY + progress.value * p.speed) % 1.2f) * h
      val curX = p.x * w
      drawRect(
        color = p.color,
        topLeft = Offset(curX, curY),
        size = androidx.compose.ui.geometry.Size(p.size, p.size * 1.5f)
      )
    }
  }
}

@Composable
fun WinnerCelebrationDialog(
  winnerCardNum: Int,
  callCount: Int,
  rewardBirr: Int?,
  roundNumber: Int = 1,
  totalRounds: Int = 3,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = BingoCardBg),
      border = androidx.compose.foundation.BorderStroke(1.5.dp, BingoGold),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("winner_celebration_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "BINGO!",
          fontFamily = PlayfairFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 36.sp,
          color = BingoGold,
          letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Round $roundNumber of $totalRounds",
          fontWeight = FontWeight.SemiBold,
          fontSize = 14.sp,
          color = BingoGoldLight
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "🏆 Card #$winnerCardNum won in $callCount calls!",
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp,
          color = BingoTextWhite,
          textAlign = TextAlign.Center
        )

        if (rewardBirr != null && rewardBirr > 0) {
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = BingoPurpleDeep,
            border = androidx.compose.foundation.BorderStroke(1.dp, BingoGold)
          ) {
            Text(
              text = "Reward: $rewardBirr birr added to wallet",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = BingoGoldLight,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LuxuryButton(
          text = if (roundNumber < totalRounds) "Next Game ($roundNumber of $totalRounds)" else "Complete Session",
          onClick = onDismiss,
          isGold = true,
          testTag = "dialog_back_button"
        )
      }
    }
  }
}

@Composable
fun SessionSummaryCelebrationDialog(
  slotLabel: String,
  totalWinningsBirr: Int,
  language: AppLanguage,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = BingoCardBg),
      border = androidx.compose.foundation.BorderStroke(2.dp, BingoGold),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("session_summary_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "🎉 3 GAMES COMPLETE!",
          fontFamily = PlayfairFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 24.sp,
          color = BingoGold,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = when (language) {
            AppLanguage.EN -> "You have played all 3 consecutive games for this playing time."
            AppLanguage.AM -> "ለዚህ የጨዋታ ሰዓት 3ቱንም ተከታታይ ጨዋታዎች አጠናቀዋል።"
            AppLanguage.TI -> "ነዚ ናይ ጸወታ ሰዓት 3ቲኦም ተኸታተልቲ ጸወታታት ዛዚምኩም።"
          },
          fontSize = 15.sp,
          color = BingoTextWhite,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = BingoPurpleDeep,
          border = androidx.compose.foundation.BorderStroke(1.dp, BingoGold)
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Total Won: $totalWinningsBirr Birr",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = BingoGoldLight
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Playing Time: $slotLabel",
              fontSize = 13.sp,
              color = BingoTextMuted
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LuxuryButton(
          text = when (language) {
            AppLanguage.EN -> "Back to Lobby"
            AppLanguage.AM -> "ወደ መቀበያው ተመለስ"
            AppLanguage.TI -> "ናብ መእተዊ ተመለስ"
          },
          onClick = onDismiss,
          isGold = true,
          testTag = "summary_back_to_lobby_button"
        )
      }
    }
  }
}

@Composable
fun YemeNoticeModal(
  title: String = "YEME BINGO",
  message: String,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = BingoCardBg),
      border = androidx.compose.foundation.BorderStroke(1.dp, BingoGold),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("yeme_notice_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp)
      ) {
        Text(
          text = title,
          fontFamily = PlayfairFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 22.sp,
          color = BingoGoldLight
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = message,
          fontSize = 15.sp,
          color = BingoTextWhite,
          lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BingoGold),
            modifier = Modifier.testTag("notice_ok_button")
          ) {
            Text(
              text = "OK",
              color = BingoTextDark,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}
