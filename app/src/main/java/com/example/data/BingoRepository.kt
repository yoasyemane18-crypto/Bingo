package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ActiveRound
import com.example.model.AppLanguage
import com.example.model.CardOwnership
import com.example.model.GameSlot
import com.example.model.PaymentRequest
import com.example.model.RequestStatus
import com.example.model.StringsDictionary
import com.example.model.UserProfile
import com.example.model.WalletTransaction
import com.example.model.WithdrawalRequest
import com.example.model.WinnerRecord
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

class BingoRepository(context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("yeme_bingo_prefs", Context.MODE_PRIVATE)

  companion object {
    const val PAY_BIRR = 30
    const val PRIZE_PERCENTAGE = 0.7
    const val CALL_INTERVAL_MS = 5000L
    const val LEAD_COUNTDOWN_MS = 5000L
    const val ROUND_BREAK_MS = 20000L
    const val ROUNDS_PER_SESSION = 3
    const val DAY_MS = 86400000L
    const val ADMIN_HASH = "479784c9aa0ca8dfe80ddad12ace91cc49c717f96a92f6c8f5a5b14721bc2f8b"
  }

  // --- Session & Language ---

  fun getSavedLanguage(): AppLanguage {
    val code = prefs.getString("app_lang", AppLanguage.EN.code) ?: AppLanguage.EN.code
    return AppLanguage.entries.firstOrNull { it.code == code } ?: AppLanguage.EN
  }

  fun saveLanguage(language: AppLanguage) {
    prefs.edit().putString("app_lang", language.code).apply()
  }

  fun getCurrentSession(): String? {
    return prefs.getString("active_session", null)
  }

  fun setCurrentSession(username: String?) {
    if (username == null) {
      prefs.edit().remove("active_session").apply()
    } else {
      prefs.edit().putString("active_session", username).apply()
    }
  }

  // --- Users & Wallet ---

  fun getUser(username: String): UserProfile? {
    val raw = prefs.getString("user_$username", null) ?: return null
    return try {
      val json = JSONObject(raw)
      val txList = mutableListOf<WalletTransaction>()
      val txArr = json.optJSONArray("tx") ?: JSONArray()
      for (i in 0 until txArr.length()) {
        val t = txArr.getJSONObject(i)
        txList.add(
          WalletTransaction(
            id = t.optString("id", UUID.randomUUID().toString()),
            label = t.optString("l", ""),
            amount = t.optInt("a", 0),
            timestamp = t.optLong("ts", System.currentTimeMillis())
          )
        )
      }
      val cardsMap = mutableMapOf<String, CardOwnership>()
      val cardsObj = json.optJSONObject("cards") ?: JSONObject()
      val keys = cardsObj.keys()
      while (keys.hasNext()) {
        val k = keys.next()
        val c = cardsObj.getJSONObject(k)
        cardsMap[k] = CardOwnership(
          cardNum = c.optInt("cardNum", 1),
          seed = c.optLong("seed", 1L),
          dayEpoch = c.optLong("dayEpoch", 0L),
          leftPlays = c.optInt("leftPlays", 3)
        )
      }

      UserProfile(
        username = json.getString("name"),
        phone = json.optString("phone", ""),
        playingTime = json.optString("time", StringsDictionary.SLOTS.first().labelEn),
        passwordHash = json.optString("pw", ""),
        walletBalance = json.optInt("wallet", 0),
        ownedCards = cardsMap,
        transactions = txList
      )
    } catch (_: Exception) {
      null
    }
  }

  fun saveUser(user: UserProfile) {
    try {
      val json = JSONObject()
      json.put("name", user.username)
      json.put("phone", user.phone)
      json.put("time", user.playingTime)
      json.put("pw", user.passwordHash)
      json.put("wallet", user.walletBalance)

      val txArr = JSONArray()
      user.transactions.takeLast(25).forEach { tx ->
        val t = JSONObject()
        t.put("id", tx.id)
        t.put("l", tx.label)
        t.put("a", tx.amount)
        t.put("ts", tx.timestamp)
        txArr.put(t)
      }
      json.put("tx", txArr)

      val cardsObj = JSONObject()
      user.ownedCards.forEach { (k, v) ->
        val c = JSONObject()
        c.put("cardNum", v.cardNum)
        c.put("seed", v.seed)
        c.put("dayEpoch", v.dayEpoch)
        c.put("leftPlays", v.leftPlays)
        cardsObj.put(k, c)
      }
      json.put("cards", cardsObj)

      prefs.edit().putString("user_${user.username}", json.toString()).apply()

      // Also maintain all usernames list
      val set = getAllUsernames().toMutableSet()
      set.add(user.username)
      saveAllUsernames(set)
    } catch (_: Exception) {
    }
  }

  fun deleteUser(username: String) {
    prefs.edit().remove("user_$username").apply()
    val set = getAllUsernames().toMutableSet()
    set.remove(username)
    saveAllUsernames(set)
    if (getCurrentSession() == username) {
      setCurrentSession(null)
    }
  }

  fun getAllUsernames(): Set<String> {
    return prefs.getStringSet("all_usernames", emptySet()) ?: emptySet()
  }

  private fun saveAllUsernames(set: Set<String>) {
    prefs.edit().putStringSet("all_usernames", set).apply()
  }

  fun hashPassword(password: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest("sb:$password".toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
  }

  fun hashAdmin(email: String, pass: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest("sb:admin:${email.trim().lowercase()}:$pass".toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
  }

  // --- Payment Requests ---

  fun getPaymentRequests(): List<PaymentRequest> {
    val raw = prefs.getString("all_payments", "[]") ?: "[]"
    val list = mutableListOf<PaymentRequest>()
    try {
      val arr = JSONArray(raw)
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        list.add(
          PaymentRequest(
            id = o.getString("id"),
            username = o.getString("u"),
            phone = o.optString("phone", ""),
            amountBirr = o.getInt("amt"),
            receiptId = o.getString("rid"),
            receiptLink = o.optString("link", ""),
            timestamp = o.getLong("ts"),
            status = RequestStatus.valueOf(o.optString("st", RequestStatus.PENDING.name))
          )
        )
      }
    } catch (_: Exception) {
    }
    return list
  }

  fun savePaymentRequests(list: List<PaymentRequest>) {
    val arr = JSONArray()
    list.forEach { p ->
      val o = JSONObject()
      o.put("id", p.id)
      o.put("u", p.username)
      o.put("phone", p.phone)
      o.put("amt", p.amountBirr)
      o.put("rid", p.receiptId)
      o.put("link", p.receiptLink)
      o.put("ts", p.timestamp)
      o.put("st", p.status.name)
      arr.put(o)
    }
    prefs.edit().putString("all_payments", arr.toString()).apply()
  }

  fun addPaymentRequest(p: PaymentRequest) {
    val list = getPaymentRequests().toMutableList()
    list.add(p)
    savePaymentRequests(list)
    recordRedeemedReceipt(
      receiptId = p.receiptId,
      username = p.username,
      amountBirr = p.amountBirr,
      receiver = "Yemane",
      details = "Payment request: ${p.id}"
    )
  }

  // --- Anti-Duplicate / Redeemed Receipts Store ---

  fun isReceiptRedeemed(receiptId: String): Boolean {
    val cleanId = receiptId.trim().uppercase()
    if (cleanId.isEmpty()) return false
    val redeemedSet = getRedeemedReceiptIds()
    if (redeemedSet.contains(cleanId)) return true

    // Check existing payment requests that are not declined
    return getPaymentRequests().any {
      it.receiptId.trim().uppercase() == cleanId && it.status != RequestStatus.DECLINED
    }
  }

  fun getRedeemedReceiptIds(): Set<String> {
    return prefs.getStringSet("redeemed_receipt_ids", emptySet()) ?: emptySet()
  }

  fun recordRedeemedReceipt(
    receiptId: String,
    username: String,
    amountBirr: Int,
    receiver: String,
    details: String = ""
  ) {
    val cleanId = receiptId.trim().uppercase()
    if (cleanId.isEmpty()) return

    val currentSet = getRedeemedReceiptIds().toMutableSet()
    currentSet.add(cleanId)
    prefs.edit().putStringSet("redeemed_receipt_ids", currentSet).apply()

    // Store in audit history
    val rawLog = prefs.getString("redeemed_receipt_audit", "[]") ?: "[]"
    try {
      val arr = JSONArray(rawLog)
      val entry = JSONObject().apply {
        put("id", cleanId)
        put("u", username)
        put("amt", amountBirr)
        put("rec", receiver)
        put("ts", System.currentTimeMillis())
        put("det", details)
      }
      arr.put(entry)
      prefs.edit().putString("redeemed_receipt_audit", arr.toString()).apply()
    } catch (_: Exception) {
    }
  }

  // --- Withdrawal Requests ---

  fun getWithdrawalRequests(): List<WithdrawalRequest> {
    val raw = prefs.getString("all_withdrawals", "[]") ?: "[]"
    val list = mutableListOf<WithdrawalRequest>()
    try {
      val arr = JSONArray(raw)
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        list.add(
          WithdrawalRequest(
            id = o.getString("id"),
            username = o.getString("u"),
            phone = o.optString("phone", ""),
            amountBirr = o.getInt("amt"),
            telebirrAccount = o.getString("acct"),
            timestamp = o.getLong("ts"),
            status = RequestStatus.valueOf(o.optString("st", RequestStatus.PENDING.name))
          )
        )
      }
    } catch (_: Exception) {
    }
    return list
  }

  fun saveWithdrawalRequests(list: List<WithdrawalRequest>) {
    val arr = JSONArray()
    list.forEach { w ->
      val o = JSONObject()
      o.put("id", w.id)
      o.put("u", w.username)
      o.put("phone", w.phone)
      o.put("amt", w.amountBirr)
      o.put("acct", w.telebirrAccount)
      o.put("ts", w.timestamp)
      o.put("st", w.status.name)
      arr.put(o)
    }
    prefs.edit().putString("all_withdrawals", arr.toString()).apply()
  }

  fun addWithdrawalRequest(w: WithdrawalRequest) {
    val list = getWithdrawalRequests().toMutableList()
    list.add(w)
    saveWithdrawalRequests(list)
  }

  // --- Winners History (Last 24h) ---

  fun getWinners(): List<WinnerRecord> {
    val raw = prefs.getString("winners_history", "[]") ?: "[]"
    val list = mutableListOf<WinnerRecord>()
    val cutoff = System.currentTimeMillis() - DAY_MS
    try {
      val arr = JSONArray(raw)
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val ts = o.getLong("ts")
        if (ts >= cutoff) {
          list.add(
            WinnerRecord(
              id = o.getString("id"),
              username = o.getString("u"),
              phone = o.optString("phone", ""),
              slotLabel = o.optString("slot", ""),
              roundNumber = o.optInt("round", 1),
              rewardBirr = o.optInt("reward", 0),
              cardNum = o.getInt("cardNum"),
              callCount = o.getInt("calls"),
              timestamp = ts
            )
          )
        }
      }
    } catch (_: Exception) {
    }
    return list
  }

  fun addWinner(record: WinnerRecord) {
    val list = getWinners().filter { it.id != record.id }.toMutableList()
    list.add(record)
    val arr = JSONArray()
    list.forEach { r ->
      val o = JSONObject()
      o.put("id", r.id)
      o.put("u", r.username)
      o.put("phone", r.phone)
      o.put("slot", r.slotLabel)
      o.put("round", r.roundNumber)
      o.put("reward", r.rewardBirr)
      o.put("cardNum", r.cardNum)
      o.put("calls", r.callCount)
      o.put("ts", r.timestamp)
      arr.put(o)
    }
    prefs.edit().putString("winners_history", arr.toString()).apply()
  }

  // --- Schedule Calculation (Ethiopian UTC+3 Time) ---

  // Ethiopian time day index: (time + 3h) / 24h
  fun getEthiopianDayIndex(timeMs: Long = System.currentTimeMillis()): Long {
    return (timeMs + 3 * 3600000L) / DAY_MS
  }

  // Calculates slot start time in epoch millis for offset day and Ethiopian hour
  fun getSlotStartTime(dayOffset: Int, hour: Int): Long {
    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      timeInMillis = now + 3 * 3600000L // convert to Ethiopian UTC+3
      add(Calendar.DAY_OF_MONTH, dayOffset)
      set(Calendar.HOUR_OF_DAY, hour)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis - 3 * 3600000L // convert back to real UTC epoch
  }

  data class SlotInfo(
    val slot: GameSlot,
    val startTime: Long,
    val rounds: List<ActiveRound>,
    val sessionEndTime: Long
  )

  fun getSlotInfo(dayOffset: Int, slot: GameSlot): SlotInfo {
    val start = getSlotStartTime(dayOffset, slot.hourUtc)
    val rounds = mutableListOf<ActiveRound>()
    var roundStart = start
    for (k in 1..ROUNDS_PER_SESSION) {
      val roundDuration = LEAD_COUNTDOWN_MS + 75 * CALL_INTERVAL_MS // ~380s
      val roundEnd = roundStart + roundDuration
      val roundId = "slot_${slot.hourUtc}_d${getEthiopianDayIndex(start)}_r$k"
      rounds.add(
        ActiveRound(
          roundNumber = k,
          roundId = roundId,
          startTime = roundStart,
          endTime = roundEnd,
          slotLabel = slot.labelEn
        )
      )
      roundStart = roundEnd + ROUND_BREAK_MS
    }
    val sessionEnd = rounds.last().endTime
    return SlotInfo(slot, start, rounds, sessionEnd)
  }

  fun getNextSlot(targetLabel: String? = null): SlotInfo {
    val now = System.currentTimeMillis()
    for (d in 0..2) {
      for (s in StringsDictionary.SLOTS) {
        if (targetLabel != null && s.labelEn != targetLabel) continue
        val info = getSlotInfo(d, s)
        if (info.startTime > now) {
          return info
        }
      }
    }
    return getSlotInfo(1, StringsDictionary.SLOTS.first())
  }

  fun getActiveSlotSession(targetLabel: String? = null): SlotInfo? {
    val now = System.currentTimeMillis()
    for (d in -1..1) {
      for (s in StringsDictionary.SLOTS) {
        if (targetLabel != null && s.labelEn != targetLabel) continue
        val info = getSlotInfo(d, s)
        if (now in info.startTime..info.sessionEndTime) {
          return info
        }
      }
    }
    return null
  }
}
