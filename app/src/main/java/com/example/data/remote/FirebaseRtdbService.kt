package com.example.data.remote

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
              username = o.optString("u", ""),
              phone = o.optString("phone", ""),
              amountBirr = o.optInt("amt", 0),
              receiptId = o.optString("rid", ""),
              receiptLink = o.optString("link", ""),
              timestamp = o.optLong("ts", System.currentTimeMillis()),
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
    json.put("phone", p.phone)
    json.put("amt", p.amountBirr)
    json.put("rid", p.receiptId)
    json.put("link", p.receiptLink)
    json.put("ts", p.timestamp)
    json.put("st", p.status.name.lowercase())

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
          val statusStr = o.optString("st", "pending").uppercase()
          val status = when (statusStr) {
            "APPROVED" -> RequestStatus.APPROVED
            "DECLINED" -> RequestStatus.DECLINED
            else -> RequestStatus.PENDING
          }
          list.add(
            WithdrawalRequest(
              id = o.optString("id", key),
              username = o.optString("u", ""),
              phone = o.optString("phone", ""),
              amountBirr = o.optInt("amt", 0),
              telebirrAccount = o.optString("acct", o.optString("account", "")),
              timestamp = o.optLong("ts", System.currentTimeMillis()),
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
    json.put("phone", w.phone)
    json.put("amt", w.amountBirr)
    json.put("acct", w.telebirrAccount)
    json.put("ts", w.timestamp)
    json.put("st", w.status.name.lowercase())

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
              username = o.optString("u", ""),
              phone = o.optString("phone", ""),
              slotLabel = o.optString("time", o.optString("slot", "")),
              roundNumber = o.optInt("k", o.optInt("round", 1)),
              rewardBirr = o.optInt("reward", 0),
              cardNum = o.optInt("n", o.optInt("cardNum", 1)),
              callCount = o.optInt("calls", 0),
              timestamp = o.optLong("at", o.optLong("ts", System.currentTimeMillis()))
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
    json.put("u", w.username)
    json.put("phone", w.phone)
    json.put("time", w.slotLabel)
    json.put("n", w.cardNum)
    json.put("calls", w.callCount)
    json.put("reward", w.rewardBirr)
    json.put("at", w.timestamp)

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
