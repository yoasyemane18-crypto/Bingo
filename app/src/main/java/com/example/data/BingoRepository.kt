package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ActiveRound
import com.example.model.AppLanguage
import com.example.model.BingoGoal
import com.example.model.CardOwnership
import com.example.model.GameSlot
import com.example.model.PaymentRequest
import com.example.model.RequestStatus
import com.example.model.StringsDictionary
import com.example.model.UserProfile
import com.example.model.WalletTransaction
import com.example.model.WithdrawalRequest
import com.example.model.WinnerRecord
import com.example.data.remote.FirebaseRtdbService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

class BingoRepository(context: Context) {

  private val appContext: Context = context.applicationContext
  private val prefs: SharedPreferences =
    context.getSharedPreferences("yeme_bingo_prefs", Context.MODE_PRIVATE)
  private val usersBackupFile: File by lazy { File(appContext.filesDir, "yeme_bingo_users_backup.json") }

  val remoteService = FirebaseRtdbService()
  private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  init {
    ioScope.launch {
      try {
        syncWithRemoteServer()
      } catch (_: Exception) {}
    }
  }

  companion object {
    const val PAY_BIRR = 30
    const val BIRR_PER_GAME = 10
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

  private fun readBackupFile(): JSONObject {
    return try {
      if (!usersBackupFile.exists()) {
        JSONObject()
      } else {
        val text = usersBackupFile.readText()
        if (text.isBlank()) JSONObject() else JSONObject(text)
      }
    } catch (_: Exception) {
      JSONObject()
    }
  }

  private fun writeBackupFile(obj: JSONObject) {
    try {
      val parent = usersBackupFile.parentFile
      if (parent != null && !parent.exists()) {
        parent.mkdirs()
      }
      usersBackupFile.writeText(obj.toString())
    } catch (_: Exception) {
    }
  }

  fun syncWithRemoteServer() {
    try {
      // 1. Pull all remote users from Firebase RTDB and cache locally
      val remoteUsers = remoteService.fetchAllUsers()
      if (remoteUsers.isNotEmpty()) {
        val remoteUsernames = remoteUsers.keys.toSet()
        saveAllUsernames(remoteUsernames)
        remoteUsers.forEach { (_, user) ->
          saveUserLocalOnly(user)
        }
        cleanupUnregisteredLocalUsers(remoteUsernames)
      }

      // 2. Fetch and merge payment requests
      val remotePays = remoteService.fetchPaymentRequests()
      if (remotePays.isNotEmpty()) {
        val localPays = getPaymentRequests().toMutableList()
        val localIds = localPays.map { it.id }.toSet()
        remotePays.forEach { p ->
          if (!localIds.contains(p.id)) {
            localPays.add(p)
          }
        }
        val arr = JSONArray()
        localPays.forEach { p ->
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

      // 3. Fetch and merge withdrawal requests
      val remoteWds = remoteService.fetchWithdrawalRequests()
      if (remoteWds.isNotEmpty()) {
        val localWds = getWithdrawalRequests().toMutableList()
        val localIds = localWds.map { it.id }.toSet()
        remoteWds.forEach { w ->
          if (!localIds.contains(w.id)) {
            localWds.add(w)
          }
        }
        val arr = JSONArray()
        localWds.forEach { w ->
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

      // 4. Fetch admin hash
      val remoteAdmin = remoteService.fetchAdminHash()
      if (!remoteAdmin.isNullOrBlank()) {
        prefs.edit().putString("remote_admin_hash", remoteAdmin).apply()
      }
    } catch (_: Exception) {
    }
  }

  fun fetchRemoteUserBlocking(username: String): UserProfile? {
    return try {
      val remote = remoteService.fetchUser(username)
      if (remote != null) {
        saveUserLocalOnly(remote)
        val names = getAllUsernamesInternal().toMutableSet()
        names.add(remote.username)
        saveAllUsernames(names)
      }
      remote
    } catch (_: Exception) {
      null
    }
  }

  fun getRemoteAdminHash(): String? {
    return prefs.getString("remote_admin_hash", null)
  }

  fun getUser(username: String): UserProfile? {
    var raw = prefs.getString("user_$username", null)
    if (raw.isNullOrBlank()) {
      // Check internal disk backup file
      synchronized(usersBackupFile) {
        val backup = readBackupFile()
        if (backup.has(username)) {
          val backupObj = backup.optJSONObject(username)
          if (backupObj != null) {
            raw = backupObj.toString()
            // Restore to SharedPreferences immediately
            prefs.edit().putString("user_$username", raw).commit()
          }
        }
      }
    }
    if (raw.isNullOrBlank()) {
      // Async try pulling from remote
      ioScope.launch {
        fetchRemoteUserBlocking(username)
      }
      return null
    }

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
        username = json.optString("name", username),
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

  fun saveUserLocalOnly(user: UserProfile) {
    try {
      val json = JSONObject()
      json.put("name", user.username)
      json.put("phone", user.phone)
      json.put("time", user.playingTime)
      json.put("pw", user.passwordHash)
      json.put("wallet", user.walletBalance)

      val txArr = JSONArray()
      user.transactions.takeLast(50).forEach { tx ->
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

      // 1. Commit synchronously to SharedPreferences to prevent process termination data loss
      prefs.edit().putString("user_${user.username}", json.toString()).commit()

      // 2. Synchronize username index in both JSON array and Set
      val set = getAllUsernamesInternal().toMutableSet()
      set.add(user.username)
      saveAllUsernames(set)

      // 3. Persist to internal physical backup file
      synchronized(usersBackupFile) {
        val backup = readBackupFile()
        backup.put(user.username, json)
        writeBackupFile(backup)
      }
    } catch (_: Exception) {
    }
  }

  fun saveUser(user: UserProfile) {
    saveUserLocalOnly(user)
    // Synchronize to real Firebase backend server asynchronously
    ioScope.launch {
      try {
        remoteService.saveUser(user)
      } catch (_: Exception) {}
    }
  }

  fun deleteUserLocalOnly(username: String) {
    prefs.edit().remove("user_$username").commit()
    val set = getAllUsernamesInternal().toMutableSet()
    set.remove(username)
    saveAllUsernames(set)
    synchronized(usersBackupFile) {
      val backup = readBackupFile()
      backup.remove(username)
      writeBackupFile(backup)
    }
    if (getCurrentSession() == username) {
      setCurrentSession(null)
    }
  }

  fun deleteUser(username: String) {
    deleteUserLocalOnly(username)
    // Synchronize deletion to real Firebase backend server
    ioScope.launch {
      try {
        remoteService.deleteUser(username)
      } catch (_: Exception) {}
    }
  }

  private fun getAllUsernamesInternal(): Set<String> {
    val names = mutableSetOf<String>()

    // 1. Read from all_usernames_json
    val rawJson = prefs.getString("all_usernames_json", null)
    if (!rawJson.isNullOrBlank()) {
      try {
        val arr = JSONArray(rawJson)
        for (i in 0 until arr.length()) {
          val n = arr.getString(i)
          if (n.isNotBlank()) names.add(n)
        }
      } catch (_: Exception) {}
    }

    // 2. Read from legacy string set
    prefs.getStringSet("all_usernames", null)?.let {
      names.addAll(it)
    }

    // 3. Recover from all SharedPreferences keys
    prefs.all.keys.forEach { k ->
      if (k.startsWith("user_")) {
        val n = k.removePrefix("user_")
        if (n.isNotBlank()) names.add(n)
      }
    }

    // 4. Recover from disk backup file
    synchronized(usersBackupFile) {
      val backup = readBackupFile()
      val keys = backup.keys()
      while (keys.hasNext()) {
        val k = keys.next()
        if (k.isNotBlank()) names.add(k)
      }
    }

    return names
  }

  private fun saveAllUsernames(set: Set<String>) {
    val arr = JSONArray()
    set.forEach { arr.put(it) }
    prefs.edit()
      .putString("all_usernames_json", arr.toString())
      .putStringSet("all_usernames", HashSet(set))
      .commit()
  }

  fun getAllUsernames(): Set<String> {
    return getAllUsernamesInternal()
  }

  fun cleanupUnregisteredLocalUsers(validUsernames: Set<String>) {
    try {
      val allKeys = prefs.all.keys.toList()
      val editor = prefs.edit()
      var changed = false
      for (key in allKeys) {
        if (key.startsWith("user_")) {
          val username = key.removePrefix("user_")
          if (!validUsernames.contains(username)) {
            editor.remove(key)
            changed = true
          }
        }
      }
      if (changed) {
        editor.commit()
      }

      synchronized(usersBackupFile) {
        val backup = readBackupFile()
        val keys = backup.keys()
        val toRemove = mutableListOf<String>()
        while (keys.hasNext()) {
          val k = keys.next()
          if (!validUsernames.contains(k)) {
            toRemove.add(k)
          }
        }
        if (toRemove.isNotEmpty()) {
          toRemove.forEach { backup.remove(it) }
          writeBackupFile(backup)
        }
      }

      val currentSession = getCurrentSession()
      if (currentSession != null && !validUsernames.contains(currentSession)) {
        setCurrentSession(null)
      }
    } catch (_: Exception) {}
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
    prefs.edit().putString("all_payments", arr.toString()).commit()
    ioScope.launch {
      try {
        list.forEach { remoteService.savePaymentRequest(it) }
      } catch (_: Exception) {}
    }
  }

  fun addPaymentRequest(p: PaymentRequest) {
    val list = getPaymentRequests().toMutableList()
    list.add(p)
    savePaymentRequests(list)
    if (p.status == RequestStatus.APPROVED) {
      recordRedeemedReceipt(
        receiptId = p.receiptId,
        username = p.username,
        amountBirr = p.amountBirr,
        receiver = "Yemane",
        details = "Payment request: ${p.id}"
      )
    }
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
    prefs.edit().putString("all_withdrawals", arr.toString()).commit()
    ioScope.launch {
      try {
        list.forEach { remoteService.saveWithdrawalRequest(it) }
      } catch (_: Exception) {}
    }
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
    prefs.edit().putString("winners_history", arr.toString()).commit()
    ioScope.launch {
      try {
        remoteService.saveWinner(record)
      } catch (_: Exception) {}
    }
  }

  // --- Schedule Calculation (Ethiopian UTC+3 Time) ---

  // Ethiopian time day index: (time + 3h) / 24h
  fun getEthiopianDayIndex(timeMs: Long = System.currentTimeMillis()): Long {
    return (timeMs + 3 * 3600000L) / DAY_MS
  }

  // Calculates slot start time in epoch millis for offset day and Ethiopian hour (GMT+3)
  fun getSlotStartTime(dayOffset: Int, hour: Int): Long {
    val eatTz = TimeZone.getTimeZone("GMT+3")
    val cal = Calendar.getInstance(eatTz).apply {
      add(Calendar.DAY_OF_MONTH, dayOffset)
      set(Calendar.HOUR_OF_DAY, hour)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
  }

  data class SlotInfo(
    val slot: GameSlot,
    val sessionId: String,
    val startTime: Long,
    val rounds: List<ActiveRound>,
    val sessionEndTime: Long
  )

  fun getCanonicalRoundSeed(slotHour: Int, dayIndex: Long, roundNum: Int): Long {
    return (dayIndex * 100000L + slotHour * 1000L + roundNum)
  }

  fun getCanonicalRoundGoal(slotHour: Int, dayIndex: Long, roundNum: Int): BingoGoal {
    val seed = getCanonicalRoundSeed(slotHour, dayIndex, roundNum)
    val index = ((seed % StringsDictionary.ALL_GOALS.size + StringsDictionary.ALL_GOALS.size) % StringsDictionary.ALL_GOALS.size).toInt()
    return StringsDictionary.ALL_GOALS[index]
  }

  fun getSlotInfo(dayOffset: Int, slot: GameSlot): SlotInfo {
    val start = getSlotStartTime(dayOffset, slot.hourUtc)
    val dayIndex = getEthiopianDayIndex(start)
    val sessionId = "session_${slot.hourUtc}_d$dayIndex"
    val rounds = mutableListOf<ActiveRound>()
    var roundStart = start
    for (k in 1..ROUNDS_PER_SESSION) {
      val roundDuration = LEAD_COUNTDOWN_MS + 75 * CALL_INTERVAL_MS // ~380s
      val roundEnd = roundStart + roundDuration
      val roundId = "slot_${slot.hourUtc}_d${dayIndex}_r$k"
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
    return SlotInfo(slot, sessionId, start, rounds, sessionEnd)
  }

  fun getNextSlot(targetLabel: String? = null): SlotInfo {
    val targetSlot = StringsDictionary.findSlot(targetLabel)
    val now = System.currentTimeMillis()
    for (d in 0..2) {
      for (s in StringsDictionary.SLOTS) {
        if (targetSlot != null && s.hourUtc != targetSlot.hourUtc) continue
        val info = getSlotInfo(d, s)
        if (info.startTime > now) {
          return info
        }
      }
    }
    val fallbackSlot = targetSlot ?: StringsDictionary.SLOTS.first()
    return getSlotInfo(1, fallbackSlot)
  }

  fun getActiveSlotSession(targetLabel: String? = null): SlotInfo? {
    val targetSlot = StringsDictionary.findSlot(targetLabel)
    val now = System.currentTimeMillis()
    for (d in -1..1) {
      for (s in StringsDictionary.SLOTS) {
        if (targetSlot != null && s.hourUtc != targetSlot.hourUtc) continue
        val info = getSlotInfo(d, s)
        if (now in info.startTime..info.sessionEndTime) {
          return info
        }
      }
    }
    return null
  }

  fun getOccupiedCartelas(sessionId: String): Map<Int, String> {
    val raw = prefs.getString("occupied_cartelas_$sessionId", null) ?: return emptyMap()
    val map = mutableMapOf<Int, String>()
    try {
      val obj = JSONObject(raw)
      val keys = obj.keys()
      while (keys.hasNext()) {
        val k = keys.next()
        val num = k.toIntOrNull() ?: continue
        map[num] = obj.optString(k, "")
      }
    } catch (_: Exception) {}
    return map
  }

  fun saveOccupiedCartelas(sessionId: String, map: Map<Int, String>) {
    try {
      val obj = JSONObject()
      map.forEach { (num, owner) ->
        obj.put(num.toString(), owner)
      }
      prefs.edit().putString("occupied_cartelas_$sessionId", obj.toString()).apply()
    } catch (_: Exception) {}
  }

  fun claimCartelas(sessionId: String, username: String, cardNumbers: List<Int>) {
    val current = getOccupiedCartelas(sessionId).toMutableMap()
    cardNumbers.forEach { current[it] = username }
    saveOccupiedCartelas(sessionId, current)
    ioScope.launch {
      try {
        remoteService.claimSessionCartelas(sessionId, username, cardNumbers)
      } catch (_: Exception) {}
    }
  }
}
