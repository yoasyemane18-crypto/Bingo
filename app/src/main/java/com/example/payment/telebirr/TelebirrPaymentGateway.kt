package com.example.payment.telebirr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.BingoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class TelebirrPaymentGateway(
  private val context: Context,
  private val repository: BingoRepository = BingoRepository(context)
) {

  private val httpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(6, TimeUnit.SECONDS)
    .readTimeout(6, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()

  /**
   * Initializes a new, signed payment order for game entry
   */
  fun createGameEntryOrder(
    username: String,
    payerPhone: String,
    cartelaCount: Int
  ): TelebirrOrder {
    val orderId = TelebirrSecurity.generateOrderReference()
    val totalBirr = cartelaCount * TelebirrConfig.BIRR_PER_CARTELA
    val timestamp = System.currentTimeMillis()
    val signature = TelebirrSecurity.generateOrderSignature(orderId, username, totalBirr, timestamp)

    return TelebirrOrder(
      orderId = orderId,
      username = username,
      payerPhone = payerPhone,
      cartelaCount = cartelaCount,
      totalAmountBirr = totalBirr,
      timestamp = timestamp,
      status = PaymentOrderStatus.CREATED,
      securityToken = signature
    )
  }

  /**
   * Securely verifies a Telebirr transaction receipt.
   *
   * Verifications performed:
   * 1. Format validity: Checks for valid Telebirr transaction ID or link (CL..., TB...).
   * 2. Anti-Repeat / Duplicate Prevention: Ensures the receipt has never been redeemed in the system.
   * 3. Receiver Verification: Confirms funds were sent to designated account holder Yemane (0939383425).
   * 4. Amount Rules: Minimum 30 Birr rule (30 Birr per cartela; below 30 Birr rejected).
   * 5. Online Handshake: Queries Ethio Telecom's electronic receipt portal when reachable.
   */
  suspend fun verifyTransaction(
    receiptRawInput: String,
    expectedAmountBirr: Int,
    existingReceipts: Set<String> = emptySet(),
    paymentMethod: String = "telebirr"
  ): TelebirrVerificationResponse = withContext(Dispatchers.IO) {
    val cleanInput = receiptRawInput.trim()
    val methodObj = com.example.payment.PaymentMethod.fromCode(paymentMethod)
    val methodName = methodObj.displayName
    val expectedAccount = methodObj.receiverAccount
    val expectedName = methodObj.receiverName

    // 1. Transaction ID extraction & syntax validity
    val transactionId = TelebirrSecurity.extractTransactionId(cleanInput)
    if (transactionId.isNullOrEmpty()) {
      return@withContext TelebirrVerificationResponse(
        isSuccess = false,
        transactionId = "",
        verifiedAmountBirr = 0,
        payerPhone = null,
        isInvalidReceipt = true,
        message = "Declined: Invalid $methodName receipt format. Please enter a valid transaction ID or official transfer confirmation message."
      )
    }

    // 2. Anti-Repeat / Duplicate check against system records
    if (existingReceipts.contains(transactionId) || repository.isReceiptRedeemed(transactionId)) {
      return@withContext TelebirrVerificationResponse(
        isSuccess = false,
        transactionId = transactionId,
        verifiedAmountBirr = 0,
        payerPhone = null,
        isDuplicate = true,
        isFraudSuspected = true,
        message = "Declined: Duplicate transaction! Receipt $transactionId has already been redeemed and cannot be reused."
      )
    }

    // 3. Minimum amount validation
    if (expectedAmountBirr < TelebirrConfig.MIN_PAYMENT_BIRR) {
      return@withContext TelebirrVerificationResponse(
        isSuccess = false,
        transactionId = transactionId,
        verifiedAmountBirr = expectedAmountBirr,
        message = "Declined: Payment of $expectedAmountBirr Birr is below the minimum required ${TelebirrConfig.MIN_PAYMENT_BIRR} Birr (30 Birr per cartela)."
      )
    }

    // 4. Strict input text verification (Receiver Account, Account Holder, and Amount)
    val (textPhone, textName) = TelebirrSecurity.extractReceiverDetails(cleanInput)
    val textAmount = TelebirrSecurity.extractAmount(cleanInput)

    // Check receiver account/phone in input text if present
    if (textPhone != null && textPhone != expectedAccount) {
      return@withContext TelebirrVerificationResponse(
        isSuccess = false,
        transactionId = transactionId,
        verifiedAmountBirr = 0,
        payerPhone = null,
        receiverName = textName ?: "Unknown",
        receiverAccount = textPhone,
        isWrongReceiver = true,
        isFraudSuspected = true,
        message = "Declined: Receipt shows payment transferred to account $textPhone, not to official merchant account $expectedAccount ($expectedName)."
      )
    }

    // Check receiver name in input text
    if (textName != null && !textName.contains(expectedName, ignoreCase = true) && !textName.contains("የማነ")) {
      return@withContext TelebirrVerificationResponse(
        isSuccess = false,
        transactionId = transactionId,
        verifiedAmountBirr = 0,
        payerPhone = null,
        receiverName = textName,
        receiverAccount = textPhone ?: "Unknown",
        isWrongReceiver = true,
        isFraudSuspected = true,
        message = "Declined: Receipt shows payment transferred to account holder $textName, not to $expectedName ($expectedAccount)."
      )
    }

    // Check amount in input text against the amount the user typed
    if (textAmount != null && textAmount != expectedAmountBirr) {
      return@withContext TelebirrVerificationResponse(
        isSuccess = false,
        transactionId = transactionId,
        verifiedAmountBirr = textAmount,
        isAmountMismatch = true,
        message = "Declined: Amount mismatch! You entered $expectedAmountBirr Birr, but the receipt shows $textAmount Birr."
      )
    }

    // 5. Method-specific Online or Offline recipient verification
    var onlineVerified = false
    var finalVerifiedAmount = textAmount ?: expectedAmountBirr

    if (methodObj == com.example.payment.PaymentMethod.TELEBIRR) {
      try {
        val receiptUrl = "${TelebirrConfig.ETHIO_TELECOM_RECEIPT_BASE_URL}$transactionId"
        val request = Request.Builder()
          .url(receiptUrl)
          .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) TelebirrVerifier/2.0")
          .build()

        httpClient.newCall(request).execute().use { response ->
          if (response.isSuccessful) {
            val responseBody = response.body?.string() ?: ""
            val onlineData = TelebirrSecurity.parseEthioTelecomReceiptHtml(responseBody)
            if (onlineData.isValid) {
              if (onlineData.creditedPartyPhone != null && onlineData.creditedPartyPhone != expectedAccount) {
                return@withContext TelebirrVerificationResponse(
                  isSuccess = false,
                  transactionId = transactionId,
                  verifiedAmountBirr = 0,
                  payerPhone = null,
                  receiverName = onlineData.creditedPartyName ?: "Unknown",
                  receiverAccount = onlineData.creditedPartyPhone,
                  isWrongReceiver = true,
                  isFraudSuspected = true,
                  message = "Declined: Receipt shows funds credited to account ${onlineData.creditedPartyPhone}, not to official account $expectedAccount ($expectedName)."
                )
              }
              if (onlineData.creditedPartyName != null &&
                !onlineData.creditedPartyName.contains(expectedName, ignoreCase = true) &&
                !onlineData.creditedPartyName.contains("የማነ")
              ) {
                return@withContext TelebirrVerificationResponse(
                  isSuccess = false,
                  transactionId = transactionId,
                  verifiedAmountBirr = 0,
                  payerPhone = null,
                  receiverName = onlineData.creditedPartyName,
                  receiverAccount = onlineData.creditedPartyPhone ?: expectedAccount,
                  isWrongReceiver = true,
                  isFraudSuspected = true,
                  message = "Declined: Receipt shows account holder is ${onlineData.creditedPartyName}, not $expectedName ($expectedAccount)."
                )
              }
              if (onlineData.amountBirr != null) {
                finalVerifiedAmount = onlineData.amountBirr
                if (finalVerifiedAmount < TelebirrConfig.MIN_PAYMENT_BIRR) {
                  return@withContext TelebirrVerificationResponse(
                    isSuccess = false,
                    transactionId = transactionId,
                    verifiedAmountBirr = finalVerifiedAmount,
                    message = "Declined: Receipt shows payment of $finalVerifiedAmount Birr, which is below the minimum required 30 Birr."
                  )
                }
              }
              onlineVerified = true
            }
          }
        }
      } catch (_: Exception) {}
    }

    // 6. If not verified online, verify recipient details in text
    if (!onlineVerified) {
      val confirmedInText = (textPhone == expectedAccount) ||
        (textName != null && (textName.contains(expectedName, ignoreCase = true) || textName.contains("የማነ"))) ||
        cleanInput.contains(expectedAccount) ||
        cleanInput.contains(expectedName, ignoreCase = true) ||
        cleanInput.contains("የማነ") ||
        cleanInput.contains("cbe", ignoreCase = true) ||
        cleanInput.contains("commercial bank", ignoreCase = true) ||
        cleanInput.contains("e-birr", ignoreCase = true) ||
        cleanInput.contains("ebirr", ignoreCase = true)

      if (!confirmedInText) {
        return@withContext TelebirrVerificationResponse(
          isSuccess = false,
          transactionId = transactionId,
          verifiedAmountBirr = 0,
          isWrongReceiver = true,
          message = "Declined: Cannot confirm transfer to account holder $expectedName ($expectedAccount). Please paste the full $methodName confirmation SMS or receipt."
        )
      }
    }

    val liveNote = if (onlineVerified) " (Live Ethio Telecom verified)" else " (Verified $methodName transfer to $expectedAccount)"

    TelebirrVerificationResponse(
      isSuccess = true,
      transactionId = transactionId,
      verifiedAmountBirr = finalVerifiedAmount,
      payerPhone = null,
      receiverName = expectedName,
      receiverAccount = expectedAccount,
      message = "✔ $methodName payment of $finalVerifiedAmount Birr to $expectedName ($expectedAccount) verified successfully$liveNote."
    )
  }



  /**
   * Attempts to launch the Telebirr application, or falls back to USSD code dialer / web link
   */
  fun launchTelebirrPayment(order: TelebirrOrder): Boolean {
    return launchPaymentAppOrUssd(com.example.payment.PaymentMethod.TELEBIRR, order)
  }

  /**
   * Attempts to launch the payment application for the selected provider (Telebirr, CBE, CBE Birr, E-Birr),
   * falling back to USSD dialing codes (*127#, *889#, *847#, *841#).
   */
  fun launchPaymentAppOrUssd(method: com.example.payment.PaymentMethod, order: TelebirrOrder?): Boolean {
    val pm = context.packageManager
    val packages = when (method) {
      com.example.payment.PaymentMethod.TELEBIRR -> listOf(TelebirrConfig.TELEBIRR_PACKAGE_NAME, TelebirrConfig.TELEBIRR_HUAWEI_PACKAGE, "cn.tydic.ethiopay")
      com.example.payment.PaymentMethod.CBE -> listOf("com.combanketh.mobilebanking", "et.com.cbe.cbebirr")
      com.example.payment.PaymentMethod.CBE_BIRR -> listOf("et.com.cbe.cbebirr", "com.combanketh.mobilebanking")
      com.example.payment.PaymentMethod.E_BIRR -> listOf("et.ebirr.mobile", "com.ebirr.android")
    }

    for (pkg in packages) {
      try {
        val launchIntent = pm.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
          launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          context.startActivity(launchIntent)
          return true
        }
      } catch (_: Exception) {}
    }

    // Fallback: Launch phone dialer with USSD code
    val ussdCode = when (method) {
      com.example.payment.PaymentMethod.TELEBIRR -> TelebirrConfig.USSD_TRANSFER_CODE
      com.example.payment.PaymentMethod.CBE -> "*889#"
      com.example.payment.PaymentMethod.CBE_BIRR -> "*847#"
      com.example.payment.PaymentMethod.E_BIRR -> "*841#"
    }

    return try {
      val dialUri = Uri.parse("tel:" + Uri.encode(ussdCode))
      val dialIntent = Intent(Intent.ACTION_DIAL, dialUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(dialIntent)
      true
    } catch (_: Exception) {
      false
    }
  }

  /**
   * Copies payment instructions and receiver account to clipboard for seamless manual transfer
   */
  fun copyPaymentDetails(order: TelebirrOrder) {
    copyPaymentDetails(com.example.payment.PaymentMethod.TELEBIRR, order)
  }

  fun copyPaymentDetails(method: com.example.payment.PaymentMethod, order: TelebirrOrder?) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val details = "${method.displayName} Payment:\n" +
      "Receiver: ${method.receiverName}\n" +
      "Account / Phone: ${method.receiverAccount}\n" +
      (if (order != null) "Amount: ${order.totalAmountBirr} Birr\nReference: ${order.orderId}\n" else "") +
      "Instruction: ${method.instruction}"

    val clip = ClipData.newPlainText("${method.displayName} Payment Info", details)
    clipboard.setPrimaryClip(clip)
  }

  /**
   * Copies just the merchant phone number for fast pasting into Telebirr
   */
  fun copyMerchantNumberOnly() {
    copyMerchantNumberOnly(com.example.payment.PaymentMethod.TELEBIRR)
  }

  fun copyMerchantNumberOnly(method: com.example.payment.PaymentMethod) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("${method.displayName} Number", method.receiverAccount)
    clipboard.setPrimaryClip(clip)
  }
}
