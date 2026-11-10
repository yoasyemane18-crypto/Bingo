package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

data class BingoPushMessage(
  val id: String,
  val scenarioTag: String,
  val scenarioTitle: String,
  val titleAm: String,
  val titleEn: String,
  val bodyAm: String,
  val bodyEn: String,
  val emoji: String,
  val wordCountAm: Int
)

data class PlayerNotificationContext(
  val username: String = "",
  val walletBalance: Int = 0,
  val hasCartelaTicket: Boolean = false,
  val ownedCardsCount: Int = 0,
  val minutesLeft: Int = 60,
  val isPracticeMode: Boolean = false,
  val practiceGamesPlayed: Int = 0,
  val lastWonBirr: Int = 0,
  val daysInactive: Int = 0,
  val isDepositApproved: Boolean = false,
  val isWithdrawalApproved: Boolean = false,
  val isSunday: Boolean = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY,
  val currentHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
)

object BingoNotificationEngine {

  const val CHANNEL_ID = "yeme_bingo_push_channel"
  const val CHANNEL_NAME = "YEME BINGO Game & Prize Alerts"

  val SUNDAY_MORNING = BingoPushMessage(
    id = "sun_morning",
    scenarioTag = "SUNDAY",
    scenarioTitle = "Sunday Morning Kickoff",
    titleAm = "የእሁድ ልዩ ጨዋታ",
    titleEn = "Sunday Morning Special",
    bodyAm = "መልካም እሁድ! የዛሬው የእሁድ ልዩ የቢንጎ ጨዋታ ጀምሯል፤ አሁኑኑ ገብተው ያሸንፉ! ☀️",
    bodyEn = "Happy Sunday! The Sunday special Bingo game is on, join now and win! ☀️",
    emoji = "☀️",
    wordCountAm = 11
  )

  val SUNDAY_AFTERNOON = BingoPushMessage(
    id = "sun_afternoon",
    scenarioTag = "SUNDAY",
    scenarioTitle = "Sunday Afternoon Leisure",
    titleAm = "የእሁድ እረፍት በቢንጎ",
    titleEn = "Sunday Afternoon Leisure",
    bodyAm = "የእሁድ እረፍትዎን በቢንጎ ጨዋታ እያዝናኑ በቀላሉ ትልቅ የገንዘብ ሽልማት በእጅዎ ያስገቡ! ☕",
    bodyEn = "Relax your Sunday with Bingo and take home cash rewards with ease! ☕",
    emoji = "☕",
    wordCountAm = 11
  )

  val SUNDAY_EVENING = BingoPushMessage(
    id = "sun_evening",
    scenarioTag = "SUNDAY",
    scenarioTitle = "Sunday Evening Grand Finale",
    titleAm = "የእሁድ ምሽት ታላቅ ጨዋታ",
    titleEn = "Sunday Evening Grand Finale",
    bodyAm = "የእሁድ ምሽትዎን በድል ያጠናቁ፤ የሳምንቱ ትልቅ የቢንጎ ሽልማት አሁኑኑ እየጠበቀዎት ነው! 🏆",
    bodyEn = "Finish your Sunday with victory; the week's biggest jackpot is waiting for you! 🏆",
    emoji = "🏆",
    wordCountAm = 11
  )

  val SUNDAY_3_IN_1 = BingoPushMessage(
    id = "sun_3in1",
    scenarioTag = "SUNDAY",
    scenarioTitle = "Sunday 3-in-1 Special",
    titleAm = "የእሁድ 3 ጨዋታዎች እድል",
    titleEn = "Sunday 3-in-1 Opportunity",
    bodyAm = "እሁድን በደስታ ያሳልፉ፤ 30 ብር ብቻ ከፍለው በ3 ተከታታይ ጨዋታዎች ይሸለሙ! 🎯",
    bodyEn = "Spend Sunday in joy; pay just 30 Birr and win in 3 consecutive games! 🎯",
    emoji = "🎯",
    wordCountAm = 11
  )

  val GAME_STARTING_SOON = BingoPushMessage(
    id = "game_soon",
    scenarioTag = "COUNTDOWN",
    scenarioTitle = "Game Starting Soon",
    titleAm = "ጨዋታው ሊጀምር ነው",
    titleEn = "Game Starting Soon",
    bodyAm = "የቢንጎ ጨዋታዎ ሊጀምር ጥቂት ደቂቃዎች ቀርተዋል፤ አሁኑኑ ገብተው ካርቴላዎን ይጫወቱ! 🎯",
    bodyEn = "Your Bingo game is starting in minutes; enter now and play your cartela! 🎯",
    emoji = "🎯",
    wordCountAm = 10
  )

  val UNPURCHASED_CARTELA = BingoPushMessage(
    id = "unpurchased",
    scenarioTag = "CARTELA",
    scenarioTitle = "Cartela Selection Reminder",
    titleAm = "ትልቅ ሽልማት እየጠበቀዎት ነው",
    titleEn = "Jackpot Waiting",
    bodyAm = "የዛሬው ትልቅ የቢንጎ ሽልማት እየጠበቀዎት ነው፤ አሁኑኑ ገብተው እድልዎን ይሞክሩ! 🏆",
    bodyEn = "Today's big Bingo reward is waiting for you; enter now and try your luck! 🏆",
    emoji = "🏆",
    wordCountAm = 10
  )

  val SCHEDULED_SESSION_START = BingoPushMessage(
    id = "scheduled_start",
    scenarioTag = "SESSION",
    scenarioTitle = "Scheduled Session Starting",
    titleAm = "የመጫወቻ ሰዓት ደርሷል",
    titleEn = "Playing Time Arrived",
    bodyAm = "የመረጡት የመጫወቻ ሰዓት ደርሷል፤ አሁኑኑ ገብተው ቢንጎ በማለት ያሸንፉ! ⚡",
    bodyEn = "Your scheduled game time has arrived; join now and shout BINGO to win! ⚡",
    emoji = "⚡",
    wordCountAm = 9
  )

  val LOW_BALANCE = BingoPushMessage(
    id = "low_balance",
    scenarioTag = "WALLET",
    scenarioTitle = "Low Balance Prompt",
    titleAm = "ሂሳብዎን ይሙሉ",
    titleEn = "Top-up Wallet",
    bodyAm = "ሂሳብዎን በቴሌብር ይሙሉና ለዛሬው ትልቅ ጨዋታ ካርቴላዎን አሁኑኑ ይቁረጡ! 💳",
    bodyEn = "Top up with Telebirr and grab your cartela for today's big game! 💳",
    emoji = "💳",
    wordCountAm = 9
  )

  val PRACTICE_TO_REAL = BingoPushMessage(
    id = "practice_to_real",
    scenarioTag = "CONVERT",
    scenarioTitle = "Practice to Real Cash",
    titleAm = "ወደ እውነተኛ ጨዋታ ይግቡ",
    titleEn = "Play for Real Rewards",
    bodyAm = "ልምምድዎ አልቋል፤ አሁን ወደ እውነተኛው ጨዋታ ገብተው የገንዘብ ሽልማት ያሸንፉ! 💰",
    bodyEn = "Your practice is done; now join the live game and win cash prizes! 💰",
    emoji = "💰",
    wordCountAm = 10
  )

  val COUNTDOWN_URGENCY_5MIN = BingoPushMessage(
    id = "urgency_5min",
    scenarioTag = "URGENCY",
    scenarioTitle = "Final 5 Minutes Urgency",
    titleAm = "የመጨረሻ 5 ደቂቃ",
    titleEn = "Last 5 Minutes",
    bodyAm = "ጨዋታው ሊዘጋ 5 ደቂቃ ብቻ ቀርቷል፤ እንዳያመልጥዎት አሁኑኑ ካርቴላ ይያዙ! ⏰",
    bodyEn = "Only 5 minutes left before game lock; grab your cartela before it starts! ⏰",
    emoji = "⏰",
    wordCountAm = 10
  )

  val PURCHASED_CARDS_READY = BingoPushMessage(
    id = "cards_ready",
    scenarioTag = "CALL_TO_ROOM",
    scenarioTitle = "Purchased Cards Waiting",
    titleAm = "ካርቴላዎችዎት ተዘጋጅተዋል",
    titleEn = "Cards Ready in Arena",
    bodyAm = "የተዘጋጁት ካርቴላዎችዎት እየጠበቁዎት ነው፤ ጨዋታው ስለጀመረ ወዲያውኑ ወደ አዳራሹ ይግቡ! 🚪",
    bodyEn = "Your prepared cards are ready; the game is live, enter the arena now! 🚪",
    emoji = "🚪",
    wordCountAm = 10
  )

  val WINNER_STREAK = BingoPushMessage(
    id = "winner_streak",
    scenarioTag = "RETENTION",
    scenarioTitle = "Winner Hot Streak",
    titleAm = "ድልዎን ይድገሙ",
    titleEn = "Repeat Your Victory",
    bodyAm = "የትናንቱ ዕድልዎ ዛሬም አለ፤ አሁኑኑ ተመልሰው ተጨማሪ የቢንጎ ድል ያጣጥሙ! 🌟",
    bodyEn = "Yesterday's luck is still here; come back now and celebrate another Bingo win! 🌟",
    emoji = "🌟",
    wordCountAm = 10
  )

  val INACTIVE_REENGAGEMENT = BingoPushMessage(
    id = "inactive",
    scenarioTag = "REENGAGE",
    scenarioTitle = "Inactive Player Welcome Back",
    titleAm = "ናፍቀውናል",
    titleEn = "We Miss You",
    bodyAm = "ናፍቀውናል! ዛሬ በመረጡት ሰዓት ገብተው ትልቁን የቢንጎ ሽልማት በእጅዎ ያስገቡ! 🎉",
    bodyEn = "We missed you! Play at your chosen hour today and seize the grand reward! 🎉",
    emoji = "🎉",
    wordCountAm = 10
  )

  val DEPOSIT_APPROVED = BingoPushMessage(
    id = "deposit_approved",
    scenarioTag = "TRANSACTION",
    scenarioTitle = "Deposit Approved",
    titleAm = "ገቢ ተረጋግጧል",
    titleEn = "Deposit Confirmed",
    bodyAm = "ያስገቡት ብር በስኬት ገቢ ሆኗል፤ አሁኑኑ ካርቴላ መርጠው ጨዋታዎን ይጀምሩ! ✅",
    bodyEn = "Your deposit succeeded; select your cartela now and start the game! ✅",
    emoji = "✅",
    wordCountAm = 10
  )

  val WITHDRAWAL_TRANSFERRED = BingoPushMessage(
    id = "withdrawal_approved",
    scenarioTag = "TRANSACTION",
    scenarioTitle = "Withdrawal Transferred",
    titleAm = "ገንዘብዎ ተልኳል",
    titleEn = "Withdrawal Sent",
    bodyAm = "ያሸነፉት ገንዘብ ወደ ቴሌብር አካውንትዎ ተልኳል፤ ዛሬም ገብተው ድጋሚ ያሸንፉ! 💸",
    bodyEn = "Your winnings were transferred to Telebirr; play again today and win! 💸",
    emoji = "💸",
    wordCountAm = 10
  )

  val ALL_TEMPLATES: List<BingoPushMessage> = listOf(
    SUNDAY_MORNING,
    SUNDAY_AFTERNOON,
    SUNDAY_EVENING,
    SUNDAY_3_IN_1,
    GAME_STARTING_SOON,
    UNPURCHASED_CARTELA,
    SCHEDULED_SESSION_START,
    LOW_BALANCE,
    PRACTICE_TO_REAL,
    COUNTDOWN_URGENCY_5MIN,
    PURCHASED_CARDS_READY,
    WINNER_STREAK,
    INACTIVE_REENGAGEMENT,
    DEPOSIT_APPROVED,
    WITHDRAWAL_TRANSFERRED
  )

  /**
   * Intelligently selects the best-converting notification for a player's real-time state.
   */
  fun evaluateState(ctx: PlayerNotificationContext): BingoPushMessage {
    // 1. Transactional high priority triggers
    if (ctx.isDepositApproved) return DEPOSIT_APPROVED
    if (ctx.isWithdrawalApproved) return WITHDRAWAL_TRANSFERRED

    // 2. Urgent in-game timing triggers
    if (ctx.minutesLeft in 1..5 && !ctx.hasCartelaTicket) return COUNTDOWN_URGENCY_5MIN
    if (ctx.minutesLeft in 1..10 && ctx.hasCartelaTicket) return PURCHASED_CARDS_READY
    if (ctx.minutesLeft == 0) return SCHEDULED_SESSION_START

    // 3. Sunday Special Triggers
    if (ctx.isSunday) {
      return when {
        ctx.currentHour < 12 -> SUNDAY_MORNING
        ctx.currentHour < 17 -> SUNDAY_AFTERNOON
        else -> SUNDAY_EVENING
      }
    }

    // 4. Player engagement triggers
    if (ctx.isPracticeMode && ctx.practiceGamesPlayed >= 2) return PRACTICE_TO_REAL
    if (ctx.walletBalance < 30) return LOW_BALANCE
    if (ctx.daysInactive >= 3) return INACTIVE_REENGAGEMENT
    if (ctx.lastWonBirr > 0) return WINNER_STREAK
    if (ctx.hasCartelaTicket) return GAME_STARTING_SOON

    return UNPURCHASED_CARTELA
  }

  /**
   * Dispatches a native Android system notification to the user's notification shade.
   */
  fun showNotification(context: Context, pushMessage: BingoPushMessage) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Instant notifications for YEME BINGO sessions, tickets, and winnings."
        enableVibration(true)
        enableLights(true)
      }
      notificationManager.createNotificationChannel(channel)
    }

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      pushMessage.id.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("${pushMessage.titleAm} ${pushMessage.emoji}")
      .setContentText(pushMessage.bodyAm)
      .setStyle(NotificationCompat.BigTextStyle().bigText(pushMessage.bodyAm))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setDefaults(NotificationCompat.DEFAULT_ALL)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    notificationManager.notify(pushMessage.id.hashCode(), notification)
  }
}
