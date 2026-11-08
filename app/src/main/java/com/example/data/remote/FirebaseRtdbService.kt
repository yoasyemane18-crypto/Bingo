package com.example.data.remote

import com.example.model.ActivityRecord
import com.example.model.CardOwnership
import com.example.model.PaymentRequest
import com.example.model.RequestStatus
import com.example.model.StringsDictionary
import com.example.model.UserProfile
import com.example.model.WalletTransaction
import com.example.model.WinnerRecord
import com.example.model.WithdrawalRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

object FirebaseRtdbConfig {
  const val BASE_URL = "https://wow-878fc-default-rtdb.firebaseio.com"
  const val API_KEY = "AIzaSyDrYOOePFsUCPHCPdNHwB05FPna-F_thvA"
  const val PROJECT_ID = "wow-878fc"
}

class FirebaseRtdbService {

  private val httpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(8, TimeUnit.SECONDS)
    .readTimeout(8, TimeUnit.SECONDS)
    .writeTimeout(8, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  fun fetchServerTimestamp(): Long? {
    val endpoints = listOf(
      "https://clients3.google.com/generate_204",
      "${FirebaseRtdbConfig.BASE_URL}/activities.json?shallow=true"
    )

    for (url in endpoints) {
      try {
        val request = Request.Builder()
          .url(url)
          .header("Cache-Control", "no-cache")
          .get()
          .build()

        val tStart = System.currentTimeMillis()
        httpClient.newCall(request).execute().use { response ->
          val tEnd = System.currentTimeMillis()
          val date = response.headers.getDate("Date")
          if (date != null) {
            val rtt = (tEnd - tStart).coerceAtLeast(0L)
            val networkLatency = rtt / 2
            return date.time + networkLatency
          }
        }
      } catch (_: Exception) {}
    }
    return null
  }

  fun fetchAllUsers(): Map<String, UserProfile> {
    val url = "${FirebaseRtdbConfig.BASE_URL}/users.json"
    val request = Request.Builder().url(url).get().build()
    val result = mutableMapOf<String, UserProfile>()

    try {
      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return emptyMap()
        val body = response.body?.string() ?: return emptyMap()
        if (body == "null" || body.isBlank()) return emptyMap()

        val root = JSONObject(body)
        val keys = root.keys()
        while (keys.hasNext()) {
          val key = keys.next()
          val userObj = root.optJSONObject(key) ?: continue
          val cleanUsername = try {
            URLDecoder.decode(key, "UTF-8")
          } catch (_: Exception) {
            key
          }

          val user = parseUserProfile(cleanUsername, userObj)
          if (user != null) {
            result[user.username] = user
          }
        }
      }
    } catch (_: Exception) {
    }
    return result
  }

  fun fetchUser(username: String): UserProfile? {
    val encoded = try {
      URLEncoder.encode(username, "UTF-8")
    } catch (_: Exception) {
      username
    }
    val url = "${FirebaseRtdbConfig.BASE_URL}/users/$encoded.json"
    val request = Request.Builder().url(url).get().build()

    try {
      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null
        if (body == "null" || body.isBlank()) return null
        val userObj = JSONObject(body)
        return parseUserProfile(username, userObj)
      }
    } catch (_: Exception) {
      return null
    }
  }

  fun saveUser(user: UserProfile): Boolean {
    val encoded = try {
      URLEncoder.encode(user.username, "UTF-8")
    } catch (_: Exception) {
      user.username
    }
    val url = "${FirebaseRtdbConfig.BASE_URL}/users/$encoded.json"

    val json = JSONObject()
    json.put("name", user.username)
    json.put("phone", user.phone)
    json.put("time", user.playingTime)
    json.put("pw", user.passwordHash)
    json.put("wallet", user.walletBalance)
    json.put("credits", user.walletBalance)

    val txArr = JSONArray()
    user.transactions.takeLast(30).forEach { tx ->
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
      c.put("first", v.dayEpoch)
      c.put("left", v.leftPlays)
      cardsObj.put(k, c)
    }
    json.put("cards", cardsObj)

    val requestBody = json.toString().toRequestBody(jsonMediaType)
    val request = Request.Builder().url(url).put(requestBody).build()

    return try {
      httpClient.newCall(request).execute().use { response ->
        response.isSuccessful
      }
    } catch (_: Exception) {
      false
    }
  }

  fun deleteUser(username: String): Boolean {
    val encoded = try {
      URLEncoder.encode(username, "UTF-8")
    } catch (_: Exception) {
      username
    }
    val url = "${FirebaseRtdbConfig.BASE_URL}/users/$encoded.json"
    val request = Request.Builder().url(url).delete().build()

    return try {
      httpClient.newCall(request).execute().use { response ->
        response.isSuccessful
      }
    } catch (_: Exception) {
      false
    }
  }

  fun fetchPaymentRequests(): List<PaymentRequest> {
    val url = "${FirebaseRtdbConfig.BASE_URL}/pays.json"
    val request = Request.Builder().url(url).get().build()
    val list = mutableListOf<PaymentRequest>()

    try {
      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return emptyList()
        val body = response.body?.string() ?: return emptyList()
        if (body == "null" || body.isBlank()) return emptyList()

        val root = JSONObject(body)
        val keys = root.keys()
        while (keys.hasNext()) {
          val key = keys.next()
          val o = root.optJSONObject(key) ?: continue
          val statusStr = o.optString("st", "pending").uppercase()
          val status = when (statusStr) {
            "APPROVED" -> RequestStatus.APPROVED
            "DECLINED" -> RequestStatus.DECLINED
            else -> RequestStatus.PENDING
          }
          list.add(
            PaymentRequest(
              id = o.optString("id", key),
              username = o.optString("u", o.optString("username", "")),
              phone = o.optString("phone", ""),
              amountBirr = o.optInt("amt", o.optInt("amountBirr", 0)),
              receiptId = o.optString("rid", o.optString("receiptId", "")),
              receiptLink = o.optString("link", o.optString("receiptLink", "")),
              paymentMethod = o.optString("pm", o.optString("paymentMethod", "telebirr")),
              timestamp = o.optLong("ts", o.optLong("timestamp", System.currentTimeMillis())),
              status = status
            )
          )
        }
      }
    } catch (_: Exception) {
    }
    return list.sortedByDescending { it.timestamp }
  }

  fun savePaymentRequest(p: PaymentRequest): Boolean {
    val url = "${FirebaseRtdbConfig.BASE_URL}/pays/${p.id}.json"
    val json = JSONObject()
    json.put("id", p.id)
    json.put("u", p.username)
    json.put("username", p.username)
    json.put("phone", p.phone)
    json.put("amt", p.amountBirr)
    json.put("amountBirr", p.amountBirr)
    json.put("rid", p.receiptId)
    json.put("receiptId", p.receiptId)
    json.put("link", p.receiptLink)
    json.put("receiptLink", p.receiptLink)
    json.put("pm", p.paymentMethod)
    json.put("paymentMethod", p.paymentMethod)
    json.put("ts", p.timestamp)
    json.put("timestamp", p.timestamp)
    json.put("st", p.status.name.lowercase())
    json.put("status", p.status.name.lowercase())

    val requestBody = json.toString().toRequestBody(jsonMediaType)
    val request = Request.Builder().url(url).put(requestBody).build()

    return try {
      httpClient.newCall(request).execute().use { response ->
        response.isSuccessful
      }
    } catch (_: Exception) {
      false
    }
  }

  fun fetchWithdrawalRequests(): List<WithdrawalRequest> {
    val url = "${FirebaseRtdbConfig.BASE_URL}/wds.json"
    val request = Request.Builder().url(url).get().build()
    val list = mutableListOf<WithdrawalRequest>()

    try {
      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return emptyList()
        val body = response.body?.string() ?: return emptyList()
        if (body == "null" || body.isBlank()) return emptyList()

        val root = JSONObject(body)
        val keys = root.keys()
        while (keys.hasNext()) {
          val key = keys.next()
          val o = root.optJSONObject(key) ?: continue
          val statusStr = o.optString("st", o.optString("status", "pending")).uppercase()
          val status = when (statusStr) {
            "APPROVED" -> RequestStatus.APPROVED
            "DECLINED" -> RequestStatus.DECLINED
            else -> RequestStatus.PENDING
          }
          list.add(
            WithdrawalRequest(
              id = o.optString("id", key),
              username = o.optString("u", o.optString("username", "")),
              phone = o.optString("phone", ""),
              amountBirr = o.optInt("amt", o.optInt("amountBirr", 0)),
              telebirrAccount = o.optString("acct", o.optString("account", "")),
              paymentMethod = o.optString("pm", o.optString("paymentMethod", "telebirr")),
              timestamp = o.optLong("ts", o.optLong("timestamp", System.currentTimeMillis())),
              status = status
            )
          )
        }
      }
    } catch (_: Exception) {
    }
    return list.sortedByDescending { it.timestamp }
  }

  fun saveWithdrawalRequest(w: WithdrawalRequest): Boolean {
    val url = "${FirebaseRtdbConfig.BASE_URL}/wds/${w.id}.json"
    val json = JSONObject()
    json.put("id", w.id)
    json.put("u", w.username)
    json.put("username", w.username)
    json.put("phone", w.phone)
    json.put("amt", w.amountBirr)
    json.put("amountBirr", w.amountBirr)
    json.put("acct", w.telebirrAccount)
    json.put("account", w.telebirrAccount)
    json.put("pm", w.paymentMethod)
    json.put("paymentMethod", w.paymentMethod)
    json.put("ts", w.timestamp)
    json.put("timestamp", w.timestamp)
    json.put("st", w.status.name.lowercase())
    json.put("status", w.status.name.lowercase())

    val requestBody = json.toString().toRequestBody(jsonMediaType)
    val request = Request.Builder().url(url).put(requestBody).build()

    return try {
      httpClient.newCall(request).execute().use { response ->
        response.isSuccessful
      }
    } catch (_: Exception) {
      false
    }
  }

  fun fetchWinners(): List<WinnerRecord> {
    val url = "${FirebaseRtdbConfig.BASE_URL}/winners.json"
    val request = Request.Builder().url(url).get().build()
    val list = mutableListOf<WinnerRecord>()

    try {
      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return emptyList()
        val body = response.body?.string() ?: return emptyList()
        if (body == "null" || body.isBlank()) return emptyList()

        val root = JSONObject(body)
        val keys = root.keys()
        while (keys.hasNext()) {
          val key = keys.next()
          val o = root.optJSONObject(key) ?: continue
          list.add(
            WinnerRecord(
              id = o.optString("id", key),
              username = o.optString("u", o.optString("username", "")),
              phone = o.optString("phone", ""),
              slotLabel = o.optString("time", o.optString("slot", o.optString("slotLabel", ""))),
              roundNumber = o.optInt("k", o.optInt("round", o.optInt("roundNumber", 1))),
              rewardBirr = o.optInt("reward", o.optInt("rewardBirr", 0)),
              cardNum = o.optInt("n", o.optInt("cardNum", 1)),
              callCount = o.optInt("calls", o.optInt("callCount", 0)),
              timestamp = o.optLong("at", o.optLong("ts", o.optLong("timestamp", System.currentTimeMillis())))
            )
          )
        }
      }
    } catch (_: Exception) {
    }
    return list.sortedByDescending { it.timestamp }
  }

  fun saveWinner(w: WinnerRecord): Boolean {
    val url = "${FirebaseRtdbConfig.BASE_URL}/winners/${w.id}.json"
    val json = JSONObject()
    json.put("id", w.id)
    json.put("k", w.roundNumber)
    json.put("roundNumber", w.roundNumber)
    json.put("u", w.username)
    json.put("username", w.username)
    json.put("phone", w.phone)
    json.put("time", w.slotLabel)
    json.put("slotLabel", w.slotLabel)
    json.put("n", w.cardNum)
    json.put("cardNum", w.cardNum)
    json.put("calls", w.callCount)
    json.put("callCount", w.callCount)
    json.put("reward", w.rewardBirr)
    json.put("rewardBirr", w.rewardBirr)
    json.put("at", w.timestamp)
    json.put("timestamp", w.timestamp)

    val requestBody = json.toString().toRequestBody(jsonMediaType)
    val request = Request.Builder().url(url).put(requestBody).build()

    return try {
      httpClient.newCall(request).execute().use { response ->
        response.isSuccessful
      }
    } catch (_: Exception) {
      false
    }
  }

  fun claimSessionCartelas(sessionId: String, username: String, cardNumbers: List<Int>): Boolean {
    if (cardNumbers.isEmpty()) return true
    val url = "${FirebaseRtdbConfig.BASE_URL}/sessions/$sessionId/cartelas.json"
    val json = JSONObject()
    cardNumbers.forEach { num ->
      json.put(num.toString(), username)
    }
    val requestBody = json.toString().toRequestBody(jsonMediaType)
    val request = Request.Builder().url(url).patch(requestBody).build()
    return try {
      httpClient.newCall(request).execute().use { it.isSuccessful }
    } catch (_: Exception) {
      false
    }
  }

  fun fetchSessionOccupiedCartelas(sessionId: String): Map<Int, String> {
    val url = "${FirebaseRtdbConfig.BASE_URL}/sessions/$sessionId/cartelas.json"
    val request = Request.Builder().url(url).get().build()
    val map = mutableMapOf<Int, String>()
    return try {
      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return emptyMap()
        val body = response.body?.string() ?: return emptyMap()
        if (body == "null" || body.isBlank()) return emptyMap()
        val root = JSONObject(body)
        val keys = root.keys()
        while (keys.hasNext()) {
          val key = keys.next()
          val cardNum = key.toIntOrNull() ?: continue
          val owner = root.optString(key, "")
          if (owner.isNotBlank()) {
            map[cardNum] = owner
          }
        }
        map
      }
    } catch (_: Exception) {
      emptyMap()
    }
  }

  fun registerPlayerInSession(sessionId: String, username: String, cartelaCount: Int): Boolean {
    val encodedUser = try { URLEncoder.encode(username, "UTF-8") } catch (_: Exception) { username }
    val url = "${FirebaseRtdbConfig.BASE_URL}/sessions/$sessionId/players/$encodedUser.json"
    val json = JSONObject().apply {
      put("u", username)
      put("cards", cartelaCount)
      put("ts", System.currentTimeMillis())
    }
    val requestBody = json.toString().toRequestBody(jsonMediaType)
    val request = Request.Builder().url(url).put(requestBody).build()
    return try {
      httpClient.newCall(request).execute().use { it.isSuccessful }
    } catch (_: Exception) {
      false
    }
  }

  fun registerPlayerInSession(sessionId: String, username: String, cardNumbers: List<Int>): Boolean {
    val encodedUser = try { URLEncoder.encode(username, "UTF-8") } catch (_: Exception) { username }
    val url = "${FirebaseRtdbConfig.BASE_URL}/sessions/$sessionId/players/$encodedUser.json"
    val json = JSONObject().apply {
      put("u", username)
      put("cards", cardNumbers.size)
      val arr = JSONArray()
      cardNumbers.forEach { arr.put(it) }
      put("cartelas", arr)
      put("ts", System.currentTimeMillis())
    }
    val requestBody = json.toString().toRequestBody(jsonMediaType)
    val request = Request.Builder().url(url).put(requestBody).build()
    return try {
      httpClient.newCall(request).execute().use { it.isSuccessful }
    } catch (_: Exception) {
      false
    }
  }

  fun fetchSessionOccupiedCartelasCount(sessionId: String): Int {
    val occupied = fetchSessionOccupiedCartelas(sessionId)
    return occupied.size
  }

  fun fetchSessionPlayerCount(sessionId: String): Int {
    val url = "${FirebaseRtdbConfig.BASE_URL}/sessions/$sessionId/players.json"
    val request = Request.Builder().url(url).get().build()
    return try {
      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return 1
        val body = response.body?.string() ?: return 1
        if (body == "null" || body.isBlank()) return 1
        val root = JSONObject(body)
        root.length().coerceAtLeast(1)
      }
    } catch (_: Exception) {
      1
    }
  }

  /**
   * Shared countdown synchronization for rounds across all devices:
   * Sets or fetches the canonical epoch start timestamp for roundNum in sessionId.
   * If not already set on the server, initializes it to targetStartMs.
   * All players will sync to this exact millisecond!
   */
  fun getOrCreateRoundStartTime(sessionId: String, roundNum: Int, targetStartMs: Long): Long {
    val url = "${FirebaseRtdbConfig.BASE_URL}/sessions/$sessionId/round_$roundNum/start_time.json"
    try {
      // 1. Try to read existing server start time
      val getReq = Request.Builder().url(url).get().build()
      httpClient.newCall(getReq).execute().use { resp ->
        if (resp.isSuccessful) {
          val body = resp.body?.string()?.trim()
          if (!body.isNullOrBlank() && body != "null") {
            val existing = body.toLongOrNull()
            if (existing != null && existing > 0L) {
              return existing
            }
          }
        }
      }
    } catch (_: Exception) {}

    // 2. Put targetStartMs if not already created
    try {
      val putBody = targetStartMs.toString().toRequestBody(jsonMediaType)
      val putReq = Request.Builder().url(url).put(putBody).build()
      httpClient.newCall(putReq).execute().use { resp ->
        if (resp.isSuccessful) {
          return targetStartMs
        }
      }
    } catch (_: Exception) {}

    return targetStartMs
  }

  fun fetchRoundWinner(sessionId: String, roundNum: Int): WinnerRecord? {
    val url = "${FirebaseRtdbConfig.BASE_URL}/sessions/$sessionId/round_$roundNum/winner.json"
    val request = Request.Builder().url(url).get().build()
    return try {
      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null
        if (body == "null" || body.isBlank()) return null
        val o = JSONObject(body)
        WinnerRecord(
          id = o.optString("id", UUID.randomUUID().toString().take(8)),
          username = o.optString("u", ""),
          phone = o.optString("phone", ""),
          slotLabel = o.optString("time", ""),
          roundNumber = o.optInt("k", roundNum),
          rewardBirr = o.optInt("reward", 0),
          cardNum = o.optInt("n", 1),
          callCount = o.optInt("calls", 0),
          timestamp = o.optLong("at", System.currentTimeMillis())
        )
      }
    } catch (_: Exception) {
      null
    }
  }

  fun postRoundWinner(sessionId: String, roundNum: Int, winner: WinnerRecord): Boolean {
    val url = "${FirebaseRtdbConfig.BASE_URL}/sessions/$sessionId/round_$roundNum/winner.json"
    val json = JSONObject().apply {
      put("id", winner.id)
      put("u", winner.username)
      put("phone", winner.phone)
      put("time", winner.slotLabel)
      put("k", winner.roundNumber)
      put("reward", winner.rewardBirr)
      put("n", winner.cardNum)
      put("calls", winner.callCount)
      put("at", winner.timestamp)
    }
    val requestBody = json.toString().toRequestBody(jsonMediaType)
    val request = Request.Builder().url(url).put(requestBody).build()
    return try {
      httpClient.newCall(request).execute().use { it.isSuccessful }
    } catch (_: Exception) {
      false
    }
  }

  fun fetchAdminHash(): String? {
    val url = "${FirebaseRtdbConfig.BASE_URL}/admin.json"
    val request = Request.Builder().url(url).get().build()

    try {
      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null
        if (body == "null" || body.isBlank()) return null
        val root = JSONObject(body)
        return root.optString("hash", null)
      }
    } catch (_: Exception) {
      return null
    }
  }

  // --- Real-time Activity Logging to Firebase RTDB ---

  fun logActivity(
    type: String,
    username: String,
    amountBirr: Int = 0,
    description: String,
    extra: String = ""
  ): Boolean {
    val id = UUID.randomUUID().toString().take(12)
    val url = "${FirebaseRtdbConfig.BASE_URL}/activities/$id.json"
    val json = JSONObject()
    json.put("id", id)
    json.put("type", type)
    json.put("u", username)
    json.put("username", username)
    json.put("amt", amountBirr)
    json.put("amountBirr", amountBirr)
    json.put("desc", description)
    json.put("description", description)
    json.put("extra", extra)
    json.put("ts", System.currentTimeMillis())
    json.put("timestamp", System.currentTimeMillis())

    val requestBody = json.toString().toRequestBody(jsonMediaType)
    val request = Request.Builder().url(url).put(requestBody).build()

    return try {
      httpClient.newCall(request).execute().use { response ->
        response.isSuccessful
      }
    } catch (_: Exception) {
      false
    }
  }

  fun fetchActivities(): List<ActivityRecord> {
    val url = "${FirebaseRtdbConfig.BASE_URL}/activities.json?limitToLast=50"
    val request = Request.Builder().url(url).get().build()
    val list = mutableListOf<ActivityRecord>()

    try {
      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return emptyList()
        val body = response.body?.string() ?: return emptyList()
        if (body == "null" || body.isBlank()) return emptyList()

        val root = JSONObject(body)
        val keys = root.keys()
        while (keys.hasNext()) {
          val key = keys.next()
          val o = root.optJSONObject(key) ?: continue
          list.add(
            ActivityRecord(
              id = o.optString("id", key),
              type = o.optString("type", "ACTIVITY"),
              username = o.optString("username", o.optString("u", "")),
              amountBirr = o.optInt("amountBirr", o.optInt("amt", 0)),
              description = o.optString("description", o.optString("desc", "")),
              extra = o.optString("extra", ""),
              timestamp = o.optLong("timestamp", o.optLong("ts", System.currentTimeMillis()))
            )
          )
        }
      }
    } catch (_: Exception) {
    }
    return list.sortedByDescending { it.timestamp }
  }

  private fun parseUserProfile(fallbackName: String, json: JSONObject): UserProfile? {
    return try {
      val txList = mutableListOf<WalletTransaction>()
      val txArr = json.optJSONArray("tx") ?: JSONArray()
      for (i in 0 until txArr.length()) {
        val t = txArr.getJSONObject(i)
        txList.add(
          WalletTransaction(
            id = t.optString("id", UUID.randomUUID().toString()),
            label = t.optString("l", ""),
            amount = t.optInt("a", 0),
            timestamp = t.optLong("ts", t.optLong("at", System.currentTimeMillis()))
          )
        )
      }

      val cardsMap = mutableMapOf<String, CardOwnership>()
      val cardsObj = json.optJSONObject("cards") ?: JSONObject()
      val keys = cardsObj.keys()
      while (keys.hasNext()) {
        val k = keys.next()
        val c = cardsObj.getJSONObject(k)
        val extractedCardNum = c.optInt("cardNum", parseCardNumFromKey(k))
        cardsMap[k] = CardOwnership(
          cardNum = extractedCardNum,
          seed = c.optLong("seed", 1L),
          dayEpoch = c.optLong("first", c.optLong("dayEpoch", 0L)),
          leftPlays = c.optInt("left", c.optInt("leftPlays", 3))
        )
      }

      val wallet = if (json.has("wallet")) json.optInt("wallet", 0) else json.optInt("credits", 0)

      UserProfile(
        username = json.optString("name", fallbackName),
        phone = json.optString("phone", ""),
        playingTime = json.optString("time", StringsDictionary.SLOTS.first().labelEn),
        passwordHash = json.optString("pw", ""),
        walletBalance = wallet,
        ownedCards = cardsMap,
        transactions = txList
      )
    } catch (_: Exception) {
      null
    }
  }

  private fun parseCardNumFromKey(key: String): Int {
    val regex = Regex("c(\\d+)")
    val match = regex.find(key)
    return match?.groupValues?.get(1)?.toIntOrNull() ?: 1
  }
}
