package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BingoRepository
import com.example.model.AppLanguage
import com.example.model.BingoCard
import com.example.model.PaymentRequest
import com.example.model.RequestStatus
import com.example.model.StringsDictionary
import com.example.model.UserProfile
import com.example.model.WithdrawalRequest
import com.example.ui.components.CountdownCard
import com.example.ui.components.LuxuryButton
import com.example.ui.components.PasteableReceiptTextField
import com.example.ui.theme.BingoBorderLight
import com.example.ui.theme.BingoBorderPurple
import com.example.ui.theme.BingoCardBg
import com.example.ui.theme.BingoCoral
import com.example.ui.theme.BingoGold
import com.example.ui.theme.BingoGoldDark
import com.example.ui.theme.BingoGoldLight
import com.example.ui.theme.BingoGreen
import com.example.ui.theme.BingoGreenLight
import com.example.ui.theme.BingoNight
import com.example.ui.theme.BingoPurple
import com.example.ui.theme.BingoPurpleDeep
import com.example.ui.theme.BingoPurpleVibrant
import com.example.ui.theme.BingoRed
import com.example.ui.theme.BingoRedLight
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LobbyScreen(
  countdownText: String,
  nextGameLabel: String,
  currentUser: UserProfile?,
  language: AppLanguage,
  selectedCardNumber: Int,
  selectedCardsList: List<Int>,
  previewCard: BingoCard,
  paymentRequests: List<PaymentRequest>,
  withdrawalRequests: List<WithdrawalRequest>,
  isCountdownFinished: Boolean = false,
  hasCartelaTicket: Boolean = false,
  onCardSelected: (Int) -> Unit,
  onRefreshSeed: () -> Unit,
  onStartGame: () -> Unit,
  onStartPracticeGame: () -> Unit = {},
  onOpenTelebirrGateway: (Int) -> Unit = {},
  onSubmitPayment: (Int, String) -> Unit,
  onSubmitWithdrawal: (Int, String) -> Unit,
  onChangePlayingTime: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var quickGoInput by remember { mutableStateOf("") }
  var activeWalletTab by remember { mutableStateOf<String?>(null) } // "deposit", "withdraw", or null
  var depositAmount by remember { mutableStateOf("30") }
  var depositReceipt by remember { mutableStateOf("") }
  var withdrawAmount by remember { mutableStateOf("30") }
  var withdrawAccount by remember { mutableStateOf("") }

  var timeSelectDropdown by remember { mutableStateOf(false) }
  var selectedNewTime by remember {
    mutableStateOf(currentUser?.playingTime ?: StringsDictionary.SLOTS.first().label(language))
  }

  // Page index for 500 cards (50 cards per page)
  var cardPage by remember { mutableIntStateOf(0) }
  val pageSize = 50
  val totalPages = 10
  val pageCards = remember(cardPage) {
    val start = cardPage * pageSize + 1
    (start until (start + pageSize)).toList()
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(BingoNight)
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Top Countdown
    CountdownCard(
      countdownText = countdownText,
      label = if (currentUser != null) {
        when (language) {
          AppLanguage.EN -> "Your game: BINGO Game at ${currentUser.playingTime}"
          AppLanguage.AM -> "የእርስዎ ጨዋታ፡ የቢንጎ ጨዋታ በ ${currentUser.playingTime}"
          AppLanguage.TI -> "ናይ ጸወታኹም፡ ናይ ቢንጎ ጸወታ ብ ${currentUser.playingTime}"
        }
      } else {
        when (language) {
          AppLanguage.EN -> "Practice game · Balls are called every 5 seconds"
          AppLanguage.AM -> "ልምምድ · ቁጥሮች በየ5 ሰከንድ ይጠራሉ"
          AppLanguage.TI -> "ልምምድ · ቁጽርታት ኣብ ነፍሲ ወከፍ 5 ሰከንድ ይጽዋዑ"
        }
      }
    )

    Spacer(modifier = Modifier.height(18.dp))

    // Header: Select Card Number
    Text(
      text = when (language) {
        AppLanguage.EN -> "Select Card Number"
        AppLanguage.AM -> "ካርድ ቁጥር ይምረጡ"
        AppLanguage.TI -> "ቁጽሪ ካርድ ሕረዩ"
      },
      fontFamily = PlayfairFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 22.sp,
      color = BingoGoldLight,
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Quick Go to Card Number Input (1..500)
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = quickGoInput,
        onValueChange = { input ->
          if (input.isEmpty() || input.all { it.isDigit() }) {
            quickGoInput = input
            val n = input.toIntOrNull()
            if (n != null && n in 1..500) {
              onCardSelected(n)
              cardPage = (n - 1) / pageSize
            }
          }
        },
        placeholder = {
          Text(
            when (language) {
              AppLanguage.EN -> "Go to card number (1–500)"
              AppLanguage.AM -> "ወደ ካርድ ቁጥር ሂድ (1–500)"
              AppLanguage.TI -> "ናብ ቁጽሪ ካርድ ኪድ (1–500)"
            },
            color = BingoTextMuted,
            fontSize = 14.sp
          )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
          .weight(1f)
          .testTag("goto_card_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = BingoGold,
          unfocusedBorderColor = BingoBorderLight,
          focusedTextColor = BingoTextWhite,
          unfocusedTextColor = BingoTextWhite
        ),
        shape = RoundedCornerShape(12.dp)
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 500 Cards Grid with pagination pills
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = BingoCardBg),
      border = androidx.compose.foundation.BorderStroke(1.dp, BingoBorderLight),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        // Page Selector row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Cards ${(cardPage * pageSize + 1)} - ${(cardPage + 1) * pageSize}",
            color = BingoGold,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(enabled = cardPage > 0) { cardPage-- },
              color = if (cardPage > 0) BingoPurpleDeep else BingoSurface
            ) {
              Text(
                text = "‹ Prev",
                color = if (cardPage > 0) BingoGold else BingoTextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }

            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(enabled = cardPage < totalPages - 1) { cardPage++ },
              color = if (cardPage < totalPages - 1) BingoPurpleDeep else BingoSurface
            ) {
              Text(
                text = "Next ›",
                color = if (cardPage < totalPages - 1) BingoGold else BingoTextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grid of 50 numbers (5 rows of 10)
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          maxItemsInEachRow = 10
        ) {
          pageCards.forEach { cardNum ->
            val isSelected = selectedCardsList.contains(cardNum)
            val isCurrentPreview = selectedCardNumber == cardNum

            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                  if (isSelected) BingoGold else BingoSurface
                )
                .border(
                  width = if (isCurrentPreview) 2.dp else 1.dp,
                  color = if (isCurrentPreview) Color.White else BingoBorderLight,
                  shape = RoundedCornerShape(6.dp)
                )
                .clickable { onCardSelected(cardNum) }
                .testTag("card_select_$cardNum"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "$cardNum",
                color = if (isSelected) BingoTextDark else BingoTextWhite,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Cartela Selection Summary Text (30 Birr for all 3 consecutive games)
    val selectedCount = selectedCardsList.size
    val cost = selectedCount * BingoRepository.PAY_BIRR
    val summaryText = if (selectedCount > 0) {
      val sortedStr = selectedCardsList.sorted().joinToString(", #")
      when (language) {
        AppLanguage.EN -> "$selectedCount cartela(s) (#$sortedStr) · $cost birr (Covers all 3 games)"
        AppLanguage.AM -> "$selectedCount ካርቴላ (#$sortedStr) · $cost ብር (ለ 3ቱም ተከታታይ ጨዋታዎች)"
        AppLanguage.TI -> "$selectedCount ካርቴላ (#$sortedStr) · $cost ብር (ን 3ቲኦም ተኸታተልቲ ጸወታታት)"
      }
    } else {
      when (language) {
        AppLanguage.EN -> "No card selected · 30 birr for 3 games"
        AppLanguage.AM -> "ምንም ካርድ አልተመረጠም · 30 ብር ለ 3 ጨዋታዎች"
        AppLanguage.TI -> "ዋላ ሓደ ካርድ ኣይተመረጸን · 30 ብር ን 3 ጸወታታት"
      }
    }

    Text(
      text = summaryText,
      color = BingoGold,
      fontWeight = FontWeight.Bold,
      fontSize = 15.sp,
      textAlign = TextAlign.Center
    )

    Text(
      text = when (language) {
        AppLanguage.EN -> "1 cartela = 30 Birr for 3 consecutive games in this session"
        AppLanguage.AM -> "1 ካርቴላ = 30 ብር ለ 3ቱ ተከታታይ ጨዋታዎች"
        AppLanguage.TI -> "1 ካርቴላ = 30 ብር ን 3ቲኦም ተኸታተልቲ ጸወታታት"
      },
      color = BingoGoldLight,
      fontSize = 12.sp,
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Card Preview Header & Grid (5x5)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = when (language) {
          AppLanguage.EN -> "Card Preview · #$selectedCardNumber"
          AppLanguage.AM -> "የካርድ ቅድመ እይታ · #$selectedCardNumber"
          AppLanguage.TI -> "ቅድሚ ርእይቶ ካርድ · #$selectedCardNumber"
        },
        fontFamily = PlayfairFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = BingoGoldLight
      )

      IconButton(
        onClick = onRefreshSeed,
        modifier = Modifier.testTag("refresh_seed_button")
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = "Refresh card layout",
          tint = BingoGold
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 5x5 Card Preview Component
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = BingoCardBg),
      border = androidx.compose.foundation.BorderStroke(1.dp, BingoGold.copy(alpha = 0.5f)),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("preview_card_grid")
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        // B - I - N - G - O Column headers
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val colLetters = listOf("B", "I", "N", "G", "O")
          val colColors = listOf(ColumnB, ColumnI, ColumnN, ColumnG, ColumnO)
          colLetters.forEachIndexed { i, letter ->
            Box(
              modifier = Modifier
                .weight(1f)
                .height(30.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = letter,
                fontFamily = PlayfairFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = colColors[i]
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 5 Rows
        for (r in 0..4) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            for (c in 0..4) {
              val num = previewCard.grid[c][r]
              val isFree = c == 2 && r == 2
              Box(
                modifier = Modifier
                  .weight(1f)
                  .aspectRatio(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isFree) BingoGreen else BingoSurface)
                  .border(
                    1.dp,
                    if (isFree) BingoGold else BingoBorderPurple,
                    RoundedCornerShape(8.dp)
                  ),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = if (isFree) "★" else "$num",
                  fontWeight = FontWeight.Bold,
                  fontSize = 18.sp,
                  color = if (isFree) Color.White else BingoTextWhite
                )
              }
            }
          }
          if (r < 4) Spacer(modifier = Modifier.height(6.dp))
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Start Game / Join Game Action Button
    val buttonText = if (currentUser != null) {
      if (isCountdownFinished) {
        when (language) {
          AppLanguage.EN -> "▶ Start Game Now · 3 Games ($cost birr)"
          AppLanguage.AM -> "▶ ጨዋታውን አሁኑኑ ጀምር · 3 ጨዋታዎች ($cost ብር)"
          AppLanguage.TI -> "▶ ጸወታ ሕጂ ጀምር · 3 ጸወታታት ($cost ብር)"
        }
      } else if (hasCartelaTicket) {
        when (language) {
          AppLanguage.EN -> "⏳ Cartela Ready · Starts in $countdownText"
          AppLanguage.AM -> "⏳ ካርቴላ ተዘጋጅቷል · በ $countdownText ይጀምራል"
          AppLanguage.TI -> "⏳ ካርቴላ ተዳልዩ · ብ $countdownText ይጅምር"
        }
      } else {
        when (language) {
          AppLanguage.EN -> "Buy Cartela for 3 Games ($cost birr)"
          AppLanguage.AM -> "ለ 3ቱ ጨዋታዎች ካርቴላ ይግዙ ($cost ብር)"
          AppLanguage.TI -> "ን 3ቲኦም ጸወታታት ካርቴላ ዓድጉ ($cost ብር)"
        }
      }
    } else {
      when (language) {
        AppLanguage.EN -> "Start Game (Practice)"
        AppLanguage.AM -> "ጨዋታ ጀምር (ልምምድ)"
        AppLanguage.TI -> "ጸወታ ጀምር (ልምምድ)"
      }
    }

    LuxuryButton(
      text = buttonText,
      onClick = onStartGame,
      isGold = true,
      testTag = "lobby_start_game_button"
    )

    if (currentUser != null) {
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = if (isCountdownFinished) {
          when (language) {
            AppLanguage.EN -> "⏰ Playing Time Active! You will play 3 consecutive games."
            AppLanguage.AM -> "⏰ የጨዋታ ሰዓት ደርሷል! 3 ተከታታይ ጨዋታዎችን ይጫወታሉ።"
            AppLanguage.TI -> "⏰ ናይ ጸወታ ሰዓት በጺሑ! 3 ተኸታተልቲ ጸወታታት ክትጻወቱ ኢኹም።"
          }
        } else {
          when (language) {
            AppLanguage.EN -> "⏰ Playing Time: ${currentUser.playingTime}\nGame starts only when countdown finishes. 3 games in 1 session."
            AppLanguage.AM -> "⏰ የጨዋታ ሰዓት፡ ${currentUser.playingTime}\nጨዋታው የሚጀምረው ቆጣሪው ሲያልቅ ብቻ ነው። 3 ተከታታይ ጨዋታዎች በአንድ ክፍለ ጊዜ።"
            AppLanguage.TI -> "⏰ ናይ ጸወታ ሰዓት፡ ${currentUser.playingTime}\nጸወታ ዝጅምር እቲ ቆጻሪ ምስ ወደአ ጥራይ እዩ። 3 ተኸታተልቲ ጸወታታት።"
          }
        },
        color = BingoGoldLight,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(10.dp))
      OutlinedButton(
        onClick = onStartPracticeGame,
        modifier = Modifier
          .fillMaxWidth()
          .height(44.dp)
          .testTag("practice_while_waiting_button"),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BingoGold.copy(alpha = 0.7f))
      ) {
        Text(
          text = when (language) {
            AppLanguage.EN -> "🎮 Play Practice Game While Waiting"
            AppLanguage.AM -> "🎮 እየጠበቁ ሳሉ የልምምድ ጨዋታ ይጫወቱ"
            AppLanguage.TI -> "🎮 እናተጸበኹም ናይ ልምምድ ጸወታ ተጻወቱ"
          },
          color = BingoGoldLight,
          fontWeight = FontWeight.SemiBold,
          fontSize = 13.sp
        )
      }

      Spacer(modifier = Modifier.height(10.dp))
      OutlinedButton(
        onClick = { onOpenTelebirrGateway(selectedCardsList.size.coerceAtLeast(1)) },
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("open_telebirr_gateway_button"),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, BingoGold)
      ) {
        Icon(
          imageVector = Icons.Default.PhoneAndroid,
          contentDescription = null,
          tint = BingoGold,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = when (language) {
            AppLanguage.EN -> "⚡ Instant Telebirr Entry (${cost.coerceAtLeast(30)} birr)"
            AppLanguage.AM -> "⚡ በቴሌብር ወዲያውኑ ይግቡ (${cost.coerceAtLeast(30)} ብር)"
            AppLanguage.TI -> "⚡ ብቴሌብር ቀጥታ ተሳተፉ (${cost.coerceAtLeast(30)} ብር)"
          },
          color = BingoGold,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )
      }
    }

    // User Wallet Section (if logged in)
    if (currentUser != null) {
      Spacer(modifier = Modifier.height(24.dp))

      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = BingoCardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, BingoGold.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = when (language) {
              AppLanguage.EN -> "Wallet balance"
              AppLanguage.AM -> "የቦርሳ ቀሪ ሂሳብ"
              AppLanguage.TI -> "ሚዛን ቦርሳ"
            },
            color = BingoTextMuted,
            fontSize = 14.sp
          )

          Text(
            text = "${currentUser.walletBalance} birr",
            fontFamily = PlayfairFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            color = BingoGold
          )

          val availCartelas = currentUser.walletBalance / BingoRepository.PAY_BIRR
          Text(
            text = "$availCartelas cartela(s) available · ${BingoRepository.PAY_BIRR} birr each",
            color = BingoTextMuted,
            fontSize = 13.sp
          )

          Spacer(modifier = Modifier.height(16.dp))

          // Deposit & Withdraw Pill Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Surface(
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(50.dp))
                .clickable {
                  activeWalletTab = if (activeWalletTab == "deposit") null else "deposit"
                }
                .border(1.5.dp, BingoGold, RoundedCornerShape(50.dp)),
              color = if (activeWalletTab == "deposit") BingoGold else Color.Transparent
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = when (language) {
                    AppLanguage.EN -> "Deposit"
                    AppLanguage.AM -> "ገንዘብ አስገባ"
                    AppLanguage.TI -> "ገንዘብ ኣእቱ"
                  },
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = if (activeWalletTab == "deposit") BingoTextDark else BingoGold
                )
              }
            }

            Surface(
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(50.dp))
                .clickable {
                  activeWalletTab = if (activeWalletTab == "withdraw") null else "withdraw"
                }
                .border(1.5.dp, BingoGold, RoundedCornerShape(50.dp)),
              color = if (activeWalletTab == "withdraw") BingoGold else Color.Transparent
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = when (language) {
                    AppLanguage.EN -> "Withdraw"
                    AppLanguage.AM -> "ገንዘብ አውጣ"
                    AppLanguage.TI -> "ገንዘብ ኣውጽእ"
                  },
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = if (activeWalletTab == "withdraw") BingoTextDark else BingoGold
                )
              }
            }
          }

          // Deposit Section
          if (activeWalletTab == "deposit") {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(BingoSurface, RoundedCornerShape(14.dp))
                .padding(14.dp)
            ) {
              Text(
                text = "Add money via Telebirr",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BingoGoldLight
              )

              Spacer(modifier = Modifier.height(6.dp))

              Text(
                text = "Merchant: 0939383425 (Yemane)",
                fontSize = 13.sp,
                color = BingoTextWhite,
                fontWeight = FontWeight.SemiBold
              )

              Spacer(modifier = Modifier.height(10.dp))

              LuxuryButton(
                text = "⚡ Launch Telebirr & Verify Here",
                onClick = {
                  val amt = depositAmount.toIntOrNull() ?: 30
                  val cartelas = (amt / 30).coerceAtLeast(1)
                  onOpenTelebirrGateway(cartelas)
                  activeWalletTab = null
                },
                isGold = true,
                testTag = "wallet_launch_telebirr_gateway_btn"
              )

              Spacer(modifier = Modifier.height(14.dp))

              Text(
                text = "Or submit receipt manually for review:",
                fontSize = 12.sp,
                color = BingoTextMuted
              )

              Spacer(modifier = Modifier.height(8.dp))

              OutlinedTextField(
                value = depositAmount,
                onValueChange = { depositAmount = it },
                label = { Text("Amount (birr)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("deposit_amount_input"),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = BingoGold,
                  unfocusedBorderColor = BingoBorderLight,
                  focusedTextColor = BingoTextWhite,
                  unfocusedTextColor = BingoTextWhite
                )
              )

              Spacer(modifier = Modifier.height(8.dp))

              PasteableReceiptTextField(
                value = depositReceipt,
                onValueChange = { depositReceipt = it },
                label = "Telebirr receipt link or number",
                placeholder = "https://transactioninfo.ethiotelecom.et/receipt/... or CL...",
                testTag = "deposit_receipt_input"
              )

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = "30 birr = 1 cartela, 60 = 2, 90 = 3… Below 30 birr is not confirmed.",
                fontSize = 12.sp,
                color = BingoTextMuted
              )

              Spacer(modifier = Modifier.height(12.dp))

              LuxuryButton(
                text = "Submit payment",
                onClick = {
                  val amt = depositAmount.toIntOrNull() ?: 0
                  onSubmitPayment(amt, depositReceipt)
                  depositReceipt = ""
                  activeWalletTab = null
                },
                isGold = false,
                testTag = "submit_deposit_button"
              )
            }
          }

          // Withdraw Section
          if (activeWalletTab == "withdraw") {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(BingoSurface, RoundedCornerShape(14.dp))
                .padding(14.dp)
            ) {
              Text(
                text = "Withdraw to Telebirr",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BingoGoldLight
              )

              Spacer(modifier = Modifier.height(8.dp))

              OutlinedTextField(
                value = withdrawAmount,
                onValueChange = { withdrawAmount = it },
                label = { Text("Amount to withdraw (birr)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("withdraw_amount_input"),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = BingoGold,
                  unfocusedBorderColor = BingoBorderLight,
                  focusedTextColor = BingoTextWhite,
                  unfocusedTextColor = BingoTextWhite
                )
              )

              Spacer(modifier = Modifier.height(8.dp))

              OutlinedTextField(
                value = withdrawAccount,
                onValueChange = { withdrawAccount = it },
                label = { Text("Telebirr account number") },
                placeholder = { Text("09XXXXXXXX") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("withdraw_account_input"),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = BingoGold,
                  unfocusedBorderColor = BingoBorderLight,
                  focusedTextColor = BingoTextWhite,
                  unfocusedTextColor = BingoTextWhite
                )
              )

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = "Amount is deducted from wallet now. Admin sends it to your Telebirr number.",
                fontSize = 12.sp,
                color = BingoTextMuted
              )

              Spacer(modifier = Modifier.height(12.dp))

              LuxuryButton(
                text = "Request withdrawal",
                onClick = {
                  val amt = withdrawAmount.toIntOrNull() ?: 0
                  onSubmitWithdrawal(amt, withdrawAccount)
                  withdrawAccount = ""
                  activeWalletTab = null
                },
                isGold = true,
                testTag = "submit_withdrawal_button"
              )
            }
          }

          // Pending Requests History
          val myPays = paymentRequests.filter { it.username == currentUser.username }.takeLast(3).reversed()
          if (myPays.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = "Payment requests",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = BingoGoldLight,
              modifier = Modifier.fillMaxWidth()
            )
            myPays.forEach { p ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "${p.amountBirr} birr · ${p.receiptId}",
                  color = BingoTextWhite,
                  fontSize = 13.sp
                )
                Text(
                  text = when (p.status) {
                    RequestStatus.PENDING -> "Pending approval"
                    RequestStatus.APPROVED -> "Approved"
                    RequestStatus.DECLINED -> "Declined"
                  },
                  color = when (p.status) {
                    RequestStatus.PENDING -> BingoGold
                    RequestStatus.APPROVED -> BingoGreenLight
                    RequestStatus.DECLINED -> BingoRedLight
                  },
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
              }
            }
          }

          val myWds = withdrawalRequests.filter { it.username == currentUser.username }.takeLast(3).reversed()
          if (myWds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Withdrawals",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = BingoGoldLight,
              modifier = Modifier.fillMaxWidth()
            )
            myWds.forEach { w ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "${w.amountBirr} birr · ${w.telebirrAccount}",
                  color = BingoTextWhite,
                  fontSize = 13.sp
                )
                Text(
                  text = when (w.status) {
                    RequestStatus.PENDING -> "Pending approval"
                    RequestStatus.APPROVED -> "Paid"
                    RequestStatus.DECLINED -> "Declined (refunded)"
                  },
                  color = when (w.status) {
                    RequestStatus.PENDING -> BingoGold
                    RequestStatus.APPROVED -> BingoGreenLight
                    RequestStatus.DECLINED -> BingoRedLight
                  },
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
              }
            }
          }
        }
      }

      // Change Playing Time Selector
      Spacer(modifier = Modifier.height(18.dp))
      Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = selectedNewTime,
          onValueChange = {},
          readOnly = true,
          label = { Text("BINGO Playing Time") },
          trailingIcon = {
            Icon(
              imageVector = Icons.Default.ArrowDropDown,
              contentDescription = "Select Time",
              tint = BingoGold,
              modifier = Modifier.clickable { timeSelectDropdown = !timeSelectDropdown }
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .clickable { timeSelectDropdown = !timeSelectDropdown },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BingoGold,
            unfocusedBorderColor = BingoBorderLight,
            focusedTextColor = BingoTextWhite,
            unfocusedTextColor = BingoTextWhite
          ),
          shape = RoundedCornerShape(12.dp)
        )

        DropdownMenu(
          expanded = timeSelectDropdown,
          onDismissRequest = { timeSelectDropdown = false },
          modifier = Modifier.background(BingoSurface)
        ) {
          StringsDictionary.SLOTS.forEach { slot ->
            val label = slot.label(language)
            DropdownMenuItem(
              text = { Text(label, color = BingoTextWhite, fontWeight = FontWeight.SemiBold) },
              onClick = {
                selectedNewTime = label
                timeSelectDropdown = false
                onChangePlayingTime(label)
              }
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(40.dp))
  }
}
