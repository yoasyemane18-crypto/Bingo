package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveRound
import com.example.model.AppLanguage
import com.example.model.BingoCard
import com.example.model.BingoGoal
import com.example.model.StringsDictionary
import com.example.ui.components.ConfettiOverlay
import com.example.ui.components.WinnerCelebrationDialog
import com.example.ui.theme.BingoBorderLight
import com.example.ui.theme.BingoBorderPurple
import com.example.ui.theme.BingoCardBg
import com.example.ui.theme.BingoCoral
import com.example.ui.theme.BingoDeep
import com.example.ui.theme.BingoGold
import com.example.ui.theme.BingoGoldDark
import com.example.ui.theme.BingoGoldLight
import com.example.ui.theme.BingoGreen
import com.example.ui.theme.BingoNight
import com.example.ui.theme.BingoPurple
import com.example.ui.theme.BingoPurpleDeep
import com.example.ui.theme.BingoPurpleVibrant
import com.example.ui.theme.BingoRed
import com.example.ui.theme.BingoSurface
import com.example.ui.theme.BingoSurfaceLight
import com.example.ui.theme.BingoTextDark
import com.example.ui.theme.BingoTextMuted
import com.example.ui.theme.BingoTextWhite
import com.example.ui.theme.ColumnB
import com.example.ui.theme.ColumnG
import com.example.ui.theme.ColumnI
import com.example.ui.theme.ColumnN
import com.example.ui.theme.ColumnO
import com.example.ui.theme.PlayfairFontFamily

@Composable
fun GameScreen(
  language: AppLanguage,
  targetGoal: BingoGoal,
  activeRound: ActiveRound?,
  rewardBirr: Int?,
  activeCards: List<BingoCard>,
  activeCardIndex: Int,
  calledNumbers: List<Int>,
  lastCalledNumber: Int?,
  isAutoMark: Boolean,
  isSoundOn: Boolean,
  voiceLanguage: String,
  readyStep: String?,
  isGameEnded: Boolean,
  showWinDialog: Boolean,
  showConfetti: Boolean,
  winnerCardNum: Int?,
  winnerCallsCount: Int?,
  onCardTabSelected: (Int) -> Unit,
  onCellTapped: (cardIndex: Int, col: Int, row: Int) -> Unit,
  onClaimBingo: () -> Unit,
  onToggleAutoMark: () -> Unit,
  onToggleSound: () -> Unit,
  onToggleVoiceLang: () -> Unit,
  onBack: () -> Unit,
  onDismissWinDialog: () -> Unit,
  modifier: Modifier = Modifier
) {
  val currentCard = activeCards.getOrNull(activeCardIndex) ?: activeCards.firstOrNull()

  // Shimmer pulse animation for Bingo button & winning cells
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "bingoPulse"
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(BingoNight)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // 1. Target Goal Banner
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("target_goal_banner"),
        shape = RoundedCornerShape(12.dp),
        color = BingoPurpleDeep,
        border = androidx.compose.foundation.BorderStroke(1.dp, BingoGold.copy(alpha = 0.6f)),
        shadowElevation = 6.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = when (language) {
              AppLanguage.EN -> "Target Goal:"
              AppLanguage.AM -> "የዒላማ ግብ፡"
              AppLanguage.TI -> "ዕላማ፡"
            },
            color = BingoTextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )

          Text(
            text = targetGoal.name(language),
            fontFamily = PlayfairFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = BingoGold
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // 2. Round Info & Reward Pool
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = if (activeRound != null) {
            when (language) {
              AppLanguage.EN -> "Game ${activeRound.roundNumber} of 3"
              AppLanguage.AM -> "ጨዋታ ${activeRound.roundNumber} ከ 3"
              AppLanguage.TI -> "ጸወታ ${activeRound.roundNumber} ካብ 3"
            }
          } else {
            when (language) {
              AppLanguage.EN -> "Practice Game"
              AppLanguage.AM -> "የልምምድ ጨዋታ"
              AppLanguage.TI -> "ናይ ልምምድ ጸወታ"
            }
          },
          color = BingoGoldLight,
          fontWeight = FontWeight.SemiBold,
          fontSize = 14.sp
        )

        if (rewardBirr != null && rewardBirr > 0) {
          Text(
            text = when (language) {
              AppLanguage.EN -> "Reward: $rewardBirr birr"
              AppLanguage.AM -> "ሽልማት፡ $rewardBirr ብር"
              AppLanguage.TI -> "ሽልማት፡ $rewardBirr ብር"
            },
            color = BingoGold,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 3. 1..75 Numbers Calling Board
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BingoDeep),
        border = androidx.compose.foundation.BorderStroke(1.dp, BingoBorderPurple),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("called_numbers_board")
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          // 10 columns matrix for 75 numbers
          val rowsCount = 8 // 8 rows x 10 = 80 cells (75 numbers)
          for (r in 0 until rowsCount) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              for (c in 0 until 10) {
                val num = r * 10 + c + 1
                if (num <= 75) {
                  val isCalled = calledNumbers.contains(num)
                  val isLast = lastCalledNumber == num

                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .aspectRatio(1f)
                      .clip(RoundedCornerShape(4.dp))
                      .background(
                        if (isCalled) {
                          Brush.horizontalGradient(listOf(BingoGold, BingoGoldDark))
                        } else {
                          Brush.horizontalGradient(listOf(BingoSurface, BingoSurface))
                        }
                      )
                      .border(
                        width = if (isLast) 1.5.dp else 0.5.dp,
                        color = if (isLast) Color.White else BingoBorderPurple,
                        shape = RoundedCornerShape(4.dp)
                      ),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = "$num",
                      fontSize = 10.sp,
                      fontWeight = if (isCalled) FontWeight.Bold else FontWeight.Normal,
                      color = if (isCalled) BingoTextDark else BingoTextMuted
                    )
                  }
                } else {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
            if (r < rowsCount - 1) Spacer(modifier = Modifier.height(4.dp))
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Last ball called notification
          val lastLetter = lastCalledNumber?.let { StringsDictionary.getBallLetter(it) } ?: ""
          Text(
            text = if (lastCalledNumber != null) {
              when (language) {
                AppLanguage.EN -> "Called ${calledNumbers.size} of 75 · Last: $lastLetter $lastCalledNumber"
                AppLanguage.AM -> "ከ 75 ${calledNumbers.size} ተጠርተዋል · የመጨረሻ፡ $lastLetter $lastCalledNumber"
                AppLanguage.TI -> "ካብ 75 ${calledNumbers.size} ተጸዊዖም · ናይ መወዳእታ፡ $lastLetter $lastCalledNumber"
              }
            } else {
              when (language) {
                AppLanguage.EN -> "Waiting for the first call…"
                AppLanguage.AM -> "የመጀመሪያው ቁጥር እየተጠበቀ ነው…"
                AppLanguage.TI -> "እቲ ናይ መጀመሪያ ቁጽሪ ይጽበ ኣሎ…"
              }
            },
            color = BingoGoldLight,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 4. Multi-Card Tabs (if user is playing with multiple cartelas)
      if (activeCards.size > 1) {
        ScrollableTabRow(
          selectedTabIndex = activeCardIndex,
          containerColor = BingoSurface,
          contentColor = BingoGold,
          edgePadding = 8.dp,
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
        ) {
          activeCards.forEachIndexed { i, card ->
            Tab(
              selected = activeCardIndex == i,
              onClick = { onCardTabSelected(i) },
              text = {
                Text(
                  text = "Card #${card.cardNum} (${card.markedCells.size})",
                  fontWeight = if (activeCardIndex == i) FontWeight.Bold else FontWeight.Normal,
                  color = if (activeCardIndex == i) BingoGold else BingoTextWhite,
                  fontSize = 13.sp
                )
              }
            )
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      // 5. Active Player's 5x5 Card
      if (currentCard != null) {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = BingoCardBg),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, BingoGold.copy(alpha = 0.6f)),
          modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp)
            .testTag("active_game_card")
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            // Card Title (e.g. Card #24)
            Text(
              text = "Card #${currentCard.cardNum}",
              fontFamily = PlayfairFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = BingoGoldLight,
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // B - I - N - G - O Columns
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              val letters = listOf("B", "I", "N", "G", "O")
              val colors = listOf(ColumnB, ColumnI, ColumnN, ColumnG, ColumnO)
              letters.forEachIndexed { i, l ->
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .height(34.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = l,
                    fontFamily = PlayfairFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = colors[i]
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 5 Rows of Tappable Cells
            for (r in 0..4) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                for (c in 0..4) {
                  val num = currentCard.grid[c][r]
                  val isFree = c == 2 && r == 2
                  val isMarked = currentCard.isMarked(c, r)
                  val isWinning = currentCard.winningCells.contains(Pair(c, r))

                  val cellBackground = when {
                    isWinning -> Brush.horizontalGradient(listOf(Color(0xFFFFF3C9), BingoGold))
                    isFree -> Brush.horizontalGradient(listOf(BingoGreen, Color(0xFF1E824C)))
                    isMarked -> Brush.horizontalGradient(listOf(BingoPurpleVibrant, BingoPurple))
                    else -> Brush.horizontalGradient(listOf(BingoSurface, BingoSurface))
                  }

                  val textColor = when {
                    isWinning -> BingoTextDark
                    isFree -> Color.White
                    isMarked -> Color.White
                    else -> BingoTextWhite
                  }

                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .aspectRatio(1f)
                      .clip(RoundedCornerShape(10.dp))
                      .background(cellBackground)
                      .border(
                        width = if (isWinning) 2.5.dp else if (isMarked) 1.5.dp else 1.dp,
                        color = if (isWinning) BingoGold else if (isMarked) BingoGold.copy(alpha = 0.8f) else BingoBorderPurple,
                        shape = RoundedCornerShape(10.dp)
                      )
                      .clickable {
                        onCellTapped(activeCardIndex, c, r)
                      }
                      .testTag("cell_${c}_${r}"),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = if (isFree) "★" else "$num",
                      fontWeight = FontWeight.Bold,
                      fontSize = 20.sp,
                      color = textColor
                    )
                  }
                }
              }
              if (r < 4) Spacer(modifier = Modifier.height(6.dp))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // 6. Giant Glowing "BINGO!" Button
      Button(
        onClick = onClaimBingo,
        enabled = !isGameEnded,
        modifier = Modifier
          .fillMaxWidth()
          .height(58.dp)
          .scale(if (!isGameEnded) pulseScale else 1f)
          .shadow(16.dp, RoundedCornerShape(50.dp))
          .testTag("claim_bingo_button"),
        shape = RoundedCornerShape(50.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = BingoGold,
          contentColor = BingoTextDark,
          disabledContainerColor = BingoSurface,
          disabledContentColor = BingoTextMuted
        )
      ) {
        Text(
          text = "BINGO!",
          fontFamily = PlayfairFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 24.sp,
          letterSpacing = 4.sp
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 7. In-Game Controls Row (Auto-Mark, Sound, Voice, Back)
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BingoSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BingoBorderPurple),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Auto-mark checkbox
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.clickable { onToggleAutoMark() }
            ) {
              Checkbox(
                checked = isAutoMark,
                onCheckedChange = { onToggleAutoMark() },
                colors = CheckboxDefaults.colors(
                  checkedColor = BingoGold,
                  checkmarkColor = BingoTextDark
                )
              )
              Text(
                text = when (language) {
                  AppLanguage.EN -> "Auto-mark"
                  AppLanguage.AM -> "ራስ-ሰር ምልክት"
                  AppLanguage.TI -> "ባዕሉ ምልክት ምግባር"
                },
                color = BingoTextWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
              )
            }

            // Sound Toggle Button
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggleSound() }
                .border(1.dp, BingoBorderLight, RoundedCornerShape(8.dp)),
              color = BingoSurfaceLight
            ) {
              Text(
                text = if (isSoundOn) "🔊 On" else "🔇 Off",
                color = BingoGold,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }

            // Voice Language Toggle
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggleVoiceLang() }
                .border(1.dp, BingoBorderLight, RoundedCornerShape(8.dp)),
              color = BingoSurfaceLight
            ) {
              Text(
                text = if (voiceLanguage == "am") "ድምጽ፡ አማ" else "Voice: EN",
                color = BingoGold,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Back to Lobby Button
          Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BingoPurpleDeep)
          ) {
            Text(
              text = "‹ " + when (language) {
                AppLanguage.EN -> "Back to lobby"
                AppLanguage.AM -> "ወደ መቀበያው ተመለስ"
                AppLanguage.TI -> "ናብ መእተዊ ተመለስ"
              },
              color = BingoGoldLight,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(40.dp))
    }

    // Ready Countdown Overlay ("Ready", "3", "2", "1")
    AnimatedVisibility(
      visible = readyStep != null,
      enter = fadeIn() + scaleIn(),
      exit = fadeOut()
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(BingoNight.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = readyStep ?: "",
          fontFamily = PlayfairFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 80.sp,
          color = BingoGold,
          letterSpacing = 2.sp
        )
      }
    }

    // Confetti Falling Particle Overlay
    ConfettiOverlay(
      isActive = showConfetti,
      modifier = Modifier.fillMaxSize()
    )

    // Winner Dialog
    if (showWinDialog && winnerCardNum != null) {
      WinnerCelebrationDialog(
        winnerCardNum = winnerCardNum,
        callCount = winnerCallsCount ?: calledNumbers.size,
        rewardBirr = rewardBirr,
        onDismiss = {
          onDismissWinDialog()
          onBack()
        }
      )
    }
  }
}
