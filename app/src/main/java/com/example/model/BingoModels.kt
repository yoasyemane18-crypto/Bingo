package com.example.model

enum class AppLanguage(val code: String, val displayName: String, val shortLabel: String) {
  EN("en", "English", "EN"),
  AM("am", "አማርኛ", "አማ"),
  TI("ti", "ትግርኛ", "ትግ")
}

enum class Screen {
  HOME,
  SIGN_IN,
  REGISTER,
  PLAY_LOBBY,
  GAME,
  SCHEDULE,
  HOW_TO_PLAY,
  ABOUT_US,
  CONTACT_US,
  FAQS,
  ADMIN
}

data class BingoCell(
  val col: Int,
  val row: Int,
  val number: Int, // 0 for FREE space
  val isMarked: Boolean = false,
  val isWinning: Boolean = false
) {
  val isFree: Boolean get() = (col == 2 && row == 2) || number == 0
}

data class BingoCard(
  val cardNum: Int,
  val seed: Long,
  val grid: List<List<Int>>, // 5 columns x 5 rows
  val markedCells: Set<Pair<Int, Int>> = setOf(Pair(2, 2)), // col to row
  val winningCells: Set<Pair<Int, Int>> = emptySet()
) {
  fun isMarked(col: Int, row: Int): Boolean =
    (col == 2 && row == 2) || markedCells.contains(Pair(col, row))
}

data class GoalPattern(
  val cells: List<Pair<Int, Int>> // col to row coordinates
)

data class BingoGoal(
  val id: String,
  val nameEn: String,
  val nameAm: String,
  val nameTi: String,
  val patterns: List<GoalPattern>
) {
  fun name(lang: AppLanguage): String = when (lang) {
    AppLanguage.EN -> nameEn
    AppLanguage.AM -> nameAm
    AppLanguage.TI -> nameTi
  }
}

data class GameSlot(
  val hourUtc: Int, // EAT / GMT+3 hour (10, 12, 14, 16, 18, 20)
  val ethiopianHour: Int, // Ethiopian hour: 4, 6, 8, 10, 12, 2
  val labelEn: String,
  val labelAm: String,
  val labelTi: String
) {
  constructor(hourUtc: Int, labelEn: String, labelAm: String, labelTi: String) : this(
    hourUtc = hourUtc,
    ethiopianHour = when (hourUtc) {
      10 -> 4
      12 -> 6
      14 -> 8
      16 -> 10
      18 -> 12
      20 -> 2
      else -> 4
    },
    labelEn = labelEn,
    labelAm = labelAm,
    labelTi = labelTi
  )

  fun label(lang: AppLanguage): String = when (lang) {
    AppLanguage.EN -> labelEn
    AppLanguage.AM -> labelAm
    AppLanguage.TI -> labelTi
  }
}

data class CardOwnership(
  val cardNum: Int,
  val seed: Long,
  val dayEpoch: Long,
  val leftPlays: Int = 3
)

data class WalletTransaction(
  val id: String,
  val label: String,
  val amount: Int,
  val timestamp: Long = System.currentTimeMillis()
)

data class UserProfile(
  val username: String,
  val phone: String,
  val playingTime: String,
  val passwordHash: String,
  val walletBalance: Int = 0,
  val ownedCards: Map<String, CardOwnership> = emptyMap(),
  val transactions: List<WalletTransaction> = emptyList()
)

enum class RequestStatus {
  PENDING,
  APPROVED,
  DECLINED
}

data class PaymentRequest(
  val id: String,
  val username: String,
  val phone: String,
  val amountBirr: Int,
  val receiptId: String,
  val receiptLink: String,
  val paymentMethod: String = "telebirr",
  val timestamp: Long = System.currentTimeMillis(),
  val status: RequestStatus = RequestStatus.PENDING
)

data class WithdrawalRequest(
  val id: String,
  val username: String,
  val phone: String,
  val amountBirr: Int,
  val telebirrAccount: String,
  val paymentMethod: String = "telebirr",
  val timestamp: Long = System.currentTimeMillis(),
  val status: RequestStatus = RequestStatus.PENDING
)

data class WinnerRecord(
  val id: String,
  val username: String,
  val phone: String,
  val slotLabel: String,
  val roundNumber: Int,
  val rewardBirr: Int,
  val cardNum: Int,
  val callCount: Int,
  val lastBall: Int = 0,
  val totalWinnersCount: Int = 1,
  val totalRoundPrizePool: Int = rewardBirr,
  val timestamp: Long = System.currentTimeMillis()
)

data class RoundResolution(
  val roundNumber: Int,
  val callCount: Int,
  val lastBall: Int,
  val totalPrizePool: Int,
  val winnersCount: Int,
  val splitReward: Int,
  val winners: List<WinnerRecord> = emptyList(),
  val timestamp: Long = System.currentTimeMillis()
)

data class ActiveRound(
  val roundNumber: Int, // 1, 2, or 3
  val roundId: String,
  val startTime: Long,
  val endTime: Long,
  val slotLabel: String
)
