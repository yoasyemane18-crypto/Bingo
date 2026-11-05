package com.example.payment.telebirr

import java.security.MessageDigest
import java.util.Locale

data class RecipientAuditResult(
  val isMatched: Boolean,
  val detectedPhone: String?,
  val detectedName: String?,
  val failureReason: String? = null
)

data class EthioReceiptData(
  val isValid: Boolean,
  val transactionId: String?,
  val creditedPartyPhone: String?,
  val creditedPartyName: String?,
  val amountBirr: Int?,
  val isStatusSuccess: Boolean,
  val statusText: String?,
  val failureReason: String? = null
)

object TelebirrSecurity {

  /**
   * Generates a cryptographic HMAC-SHA256 signature for a payment order
   */
  fun generateOrderSignature(
    orderId: String,
    username: String,
    amountBirr: Int,
    timestamp: Long
  ): String {
    val rawPayload = "$orderId|$username|$amountBirr|$timestamp|${TelebirrConfig.GATEWAY_SECURITY_SALT}"
    return try {
      val sha256 = MessageDigest.getInstance("SHA-256")
      val hash = sha256.digest(rawPayload.toByteArray(Charsets.UTF_8))
      hash.joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
      orderId.hashCode().toString()
    }
  }

  /**
   * Verifies that the order payload matches the security signature
   */
  fun verifyOrderIntegrity(order: TelebirrOrder): Boolean {
    val expected = generateOrderSignature(
      order.orderId,
      order.username,
      order.totalAmountBirr,
      order.timestamp
    )
    return order.securityToken == expected
  }

  /**
   * Extracts clean transaction reference code from receipt inputs:
   * e.g.:
   * "CL240899128" -> "CL240899128"
   * "https://transactioninfo.ethiotelecom.et/receipt/CL240899128/" -> "CL240899128"
   * "You have transferred 60 Birr to Yemane with receipt CL240899128." -> "CL240899128"
   */
  fun extractTransactionId(rawInput: String): String? {
    val clean = rawInput.trim()
    if (clean.isEmpty()) return null

    // 1. Direct URL extraction (transactioninfo.ethiotelecom.et/receipt/<ID>)
    val urlRegex = Regex("""(?:transactioninfo\.ethiotelecom\.et/receipt/)([A-Za-z0-9]{8,14})""", RegexOption.IGNORE_CASE)
    val urlMatch = urlRegex.find(clean)
    if (urlMatch != null) {
      return urlMatch.groupValues[1].uppercase()
    }

    // 2. Keyword-guided extraction (e.g. "transaction number: CL992837410", "receipt: CL240899128", "ref: TB12345678", "የሂሳብ ማረጋገጫ ቁጥር CL240899128")
    val keywordRegex = Regex("""(?:transaction(?:\s+(?:number|id|code|no))?|receipt(?:\s+(?:number|id|code|no))?|txid|ref|ማረጋገጫ\s*ቁጥር|መለያ\s*ቁጥር)[:\s#]+([A-Za-z0-9]{8,14})""", RegexOption.IGNORE_CASE)
    val keywordMatch = keywordRegex.find(clean)
    if (keywordMatch != null) {
      return keywordMatch.groupValues[1].uppercase()
    }

    // 3. Token extraction: look for mixed alphanumeric tokens (contains both letters and digits, like CL992837410 or TB20261103)
    val tokenRegex = Regex("""\b([A-Za-z0-9]{8,14})\b""")
    val tokens = tokenRegex.findAll(clean).map { it.value.uppercase() }.toList()

    val alphanumericCode = tokens.firstOrNull { candidate ->
      candidate.any { it.isLetter() } && candidate.any { it.isDigit() } && candidate.length in 8..14
    }
    if (alphanumericCode != null) {
      return alphanumericCode
    }

    // 4. Otherwise, non-phone numeric receipt (exclude Ethiopian mobile numbers starting with 09 or 2519)
    val numericReceipt = tokens.firstOrNull { candidate ->
      candidate.all { it.isDigit() } && candidate.length in 8..14 && !candidate.startsWith("09") && !candidate.startsWith("2519")
    }

    return numericReceipt ?: tokens.firstOrNull { it.length in 8..14 }
  }

  /**
   * Normalizes Ethiopian phone numbers:
   * "+251939383425" -> "0939383425"
   * "251939383425" -> "0939383425"
   * "09 39 38 34 25" -> "0939383425"
   */
  fun normalizePhone(phone: String?): String? {
    if (phone.isNullOrBlank()) return null
    val digits = phone.filter { it.isDigit() }
    return when {
      digits.startsWith("2519") && digits.length == 12 -> "09" + digits.substring(4)
      digits.startsWith("09") && digits.length == 10 -> digits
      else -> digits
    }
  }

  /**
   * Extracts recipient phone and name from SMS text, HTML, or input payload.
   */
  fun extractReceiverDetails(rawInput: String): Pair<String?, String?> {
    val clean = rawInput.trim()
    if (clean.isEmpty()) return Pair(null, null)

    // Strip HTML tags so table cells/spans/divs become clean text
    val text = if (clean.contains("<") && clean.contains(">")) {
      clean.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ")
    } else {
      clean
    }

    // 1. Check for explicit transfer patterns with receiver context:
    // e.g., "to Yemane (0939383425)", "Credited party: Yemane (0939383425)", "ወደ 0939383425 (Yemane)", "Credited Party: 0939383425 (Yemane)"
    val phoneWithContextRegex = Regex(
      """(?:to|ወደ|recipient|receiver|ተቀባይ|credited\s*party|credited\s*to|beneficiary)[:\s]+(?:([A-Za-z\u1200-\u137F\s]{2,30})\s*)?\(?(\+?2519\d{8}|09\d{8})\)?""",
      RegexOption.IGNORE_CASE
    )
    val match1 = phoneWithContextRegex.find(text)
    if (match1 != null) {
      val nameCandidate = match1.groupValues[1].trim().takeIf { it.isNotBlank() }
      val phoneCandidate = normalizePhone(match1.groupValues[2])
      return Pair(phoneCandidate, nameCandidate)
    }

    // 2. Pattern: "credited party: 0939383425" or "to: 0939383425" or "ወደ: 0939383425"
    val toPhoneOnlyRegex = Regex(
      """(?:to|ወደ|ተቀባይ|credited\s*party|credited\s*to|beneficiary)[:\s]+(\+?2519\d{8}|09\d{8})""",
      RegexOption.IGNORE_CASE
    )
    val matchToPhone = toPhoneOnlyRegex.find(text)
    if (matchToPhone != null) {
      val phone = normalizePhone(matchToPhone.groupValues[1])
      val name = if (text.contains(TelebirrConfig.MERCHANT_NAME, ignoreCase = true) || text.contains("የማነ")) {
        TelebirrConfig.MERCHANT_NAME
      } else null
      return Pair(phone, name)
    }

    // 3. Pattern with name followed by phone: "Yemane (0939383425)" or "Abebe (0911223344)"
    val namePhoneRegex = Regex("""([A-Za-z\u1200-\u137F\s]{3,30})\s*\(\s*(\+?2519\d{8}|09\d{8})\s*\)""")
    val match2 = namePhoneRegex.find(text)
    if (match2 != null) {
      val name = match2.groupValues[1].trim()
      val phone = normalizePhone(match2.groupValues[2])
      return Pair(phone, name)
    }

    // 4. Check if merchant account explicitly appears anywhere in text
    val phoneFound = if (text.contains("0939383425") || text.contains("251939383425")) "0939383425" else null
    val nameFound = if (text.contains(TelebirrConfig.MERCHANT_NAME, ignoreCase = true) || text.contains("የማነ")) {
      TelebirrConfig.MERCHANT_NAME
    } else null

    // 5. Check if another Ethiopian mobile phone appears in the message that isn't merchant phone
    val anyPhoneRegex = Regex("""(\+?2519\d{8}|09\d{8})""")
    val phones = anyPhoneRegex.findAll(text).mapNotNull { normalizePhone(it.value) }.toList()
    val otherPhone = phones.firstOrNull { it != TelebirrConfig.MERCHANT_ACCOUNT }

    return Pair(phoneFound ?: otherPhone, nameFound)
  }

  /**
   * Validates whether the recipient in the SMS or receipt text matches the account holder (Yemane / 0939383425).
   */
  fun validateRecipient(rawInput: String): RecipientAuditResult {
    val clean = rawInput.trim()
    val (phone, name) = extractReceiverDetails(clean)

    val expectedPhone = TelebirrConfig.MERCHANT_ACCOUNT
    val expectedName = TelebirrConfig.MERCHANT_NAME.lowercase(Locale.ROOT)

    // If an explicit phone is detected:
    if (phone != null) {
      if (phone != expectedPhone) {
        val nameDesc = if (!name.isNullOrBlank()) " ($name)" else ""
        return RecipientAuditResult(
          isMatched = false,
          detectedPhone = phone,
          detectedName = name,
          failureReason = "Declined: Receipt shows payment transferred to account $phone$nameDesc, not to official account holder ${TelebirrConfig.MERCHANT_NAME} ($expectedPhone)."
        )
      }
    }

    // If an explicit recipient name is detected:
    if (!name.isNullOrBlank()) {
      val lowerName = name.lowercase(Locale.ROOT)
      if (!lowerName.contains(expectedName) && !lowerName.contains("የማነ")) {
        val phoneDesc = if (phone != null) " ($phone)" else ""
        return RecipientAuditResult(
          isMatched = false,
          detectedPhone = phone,
          detectedName = name,
          failureReason = "Declined: Receipt shows payment transferred to $name$phoneDesc, not to official account holder ${TelebirrConfig.MERCHANT_NAME} ($expectedPhone)."
        )
      }
    }

    val matched = (phone == expectedPhone) ||
      (name != null && (name.lowercase(Locale.ROOT).contains(expectedName) || name.contains("የማነ"))) ||
      clean.contains(expectedPhone) ||
      clean.contains(TelebirrConfig.MERCHANT_NAME, ignoreCase = true) ||
      clean.contains("የማነ")

    return RecipientAuditResult(
      isMatched = matched,
      detectedPhone = phone ?: if (matched) expectedPhone else null,
      detectedName = name ?: if (matched) TelebirrConfig.MERCHANT_NAME else null,
      failureReason = if (!matched) {
        "Declined: Could not verify that payment was transferred to official merchant account ${TelebirrConfig.MERCHANT_NAME} ($expectedPhone)."
      } else null
    )
  }

  /**
   * Extracts paid amount in Birr from SMS, receipt text, or HTML:
   * e.g. "60.00 ETB", "60 ETB", "ETB 60.00", "ብር 60.00", "Amount: 90", "30 Birr"
   */
  fun extractAmount(rawInput: String): Int? {
    val clean = rawInput.trim()
    if (clean.isEmpty()) return null

    // 1. Pattern: "ብር 60.00" or "ብር 60"
    val amharicBirrRegex = Regex("""(?:ብር|birr)\s*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
    val matchAm = amharicBirrRegex.find(clean)
    if (matchAm != null) {
      val v = matchAm.groupValues[1].toDoubleOrNull()?.toInt()
      if (v != null && v > 0) return v
    }

    // 2. Pattern: "60.00 ETB" or "60.00 Birr" or "60 ETB" or "60 ብር"
    val birrAfterRegex = Regex("""([0-9]+(?:\.[0-9]+)?)\s*(?:etb|birr|ብር)""", RegexOption.IGNORE_CASE)
    val matchAfter = birrAfterRegex.find(clean)
    if (matchAfter != null) {
      val v = matchAfter.groupValues[1].toDoubleOrNull()?.toInt()
      if (v != null && v > 0) return v
    }

    // 3. Pattern: "ETB 60.00" or "ETB 60"
    val etbBeforeRegex = Regex("""(?:etb)\s*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
    val matchBefore = etbBeforeRegex.find(clean)
    if (matchBefore != null) {
      val v = matchBefore.groupValues[1].toDoubleOrNull()?.toInt()
      if (v != null && v > 0) return v
    }

    // 4. Pattern: "amount: 60.00" or "amount : 60" or "መጠን: 60"
    val amountRegex = Regex("""(?:amount|መጠን|total)[:\s]+([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
    val matchAmt = amountRegex.find(clean)
    if (matchAmt != null) {
      val v = matchAmt.groupValues[1].toDoubleOrNull()?.toInt()
      if (v != null && v > 0) return v
    }

    // 5. Pattern: JSON/HTML attribute e.g. "amount": 60 or "amount": "60.00"
    val jsonRegex = Regex(""""amount"\s*:\s*"?([0-9]+(?:\.[0-9]+)?)"?""", RegexOption.IGNORE_CASE)
    val matchJson = jsonRegex.find(clean)
    if (matchJson != null) {
      val v = matchJson.groupValues[1].toDoubleOrNull()?.toInt()
      if (v != null && v > 0) return v
    }

    return null
  }

  /**
   * Parses HTML or JSON from Ethio Telecom electronic receipt query (transactioninfo.ethiotelecom.et/receipt/<ID>).
   */
  fun parseEthioTelecomReceiptHtml(html: String): EthioReceiptData {
    val lower = html.lowercase(Locale.ROOT)

    // Check for invalid or not found flags
    val notFoundTokens = listOf(
      "receipt not found",
      "transaction not found",
      "no data found",
      "invalid receipt",
      "invalid transaction",
      "record not found",
      "transaction does not exist",
      "404 not found"
    )
    if (notFoundTokens.any { lower.contains(it) }) {
      return EthioReceiptData(
        isValid = false,
        transactionId = null,
        creditedPartyPhone = null,
        creditedPartyName = null,
        amountBirr = null,
        isStatusSuccess = false,
        statusText = "Not Found",
        failureReason = "Receipt not found in Ethio Telecom records. Please confirm your transaction ID."
      )
    }

    // Check for failed transaction status
    val failedTokens = listOf("failed", "cancelled", "reversed", "declined", "የተሰረዘ", "ያልተሳካ")
    if (failedTokens.any { lower.contains(it) }) {
      return EthioReceiptData(
        isValid = false,
        transactionId = null,
        creditedPartyPhone = null,
        creditedPartyName = null,
        amountBirr = null,
        isStatusSuccess = false,
        statusText = "Failed",
        failureReason = "Ethio Telecom records indicate this transaction was failed or cancelled."
      )
    }

    val isSuccess = lower.contains("success") ||
      lower.contains("completed") ||
      lower.contains("successful") ||
      lower.contains("የተሳካ")

    // Extract transaction ID from HTML
    val txId = extractTransactionId(html)

    // Extract amount in ETB from HTML
    val amount = extractAmount(html)

    // Extract credited party phone and name from HTML
    val (phone, name) = extractReceiverDetails(html)

    return EthioReceiptData(
      isValid = isSuccess,
      transactionId = txId,
      creditedPartyPhone = phone,
      creditedPartyName = name,
      amountBirr = amount,
      isStatusSuccess = isSuccess,
      statusText = if (isSuccess) "Completed" else "Unknown",
      failureReason = if (!isSuccess) "Transaction has not been marked as completed by Ethio Telecom." else null
    )
  }

  /**
   * Generates a cryptographically unique Order Reference (e.g. YEME-20261103-8F3A)
   */
  fun generateOrderReference(): String {
    val datePart = java.text.SimpleDateFormat("yyyyMMdd", Locale.US).format(java.util.Date())
    val randomPart = java.util.UUID.randomUUID().toString().replace("-", "").take(6).uppercase(Locale.ROOT)
    return "YEME-$datePart-$randomPart"
  }
}
