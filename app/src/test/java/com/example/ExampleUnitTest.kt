package com.example

import com.example.engine.BingoEngine
import com.example.model.StringsDictionary
import com.example.payment.telebirr.TelebirrConfig
import com.example.payment.telebirr.TelebirrOrder
import com.example.payment.telebirr.PaymentOrderStatus
import com.example.payment.telebirr.TelebirrSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testBingoCardGeneration() {
    val card = BingoEngine.generateCard(cardNum = 1, seed = 12345L)
    assertEquals(1, card.cardNum)
    assertEquals(5, card.grid.size)
    for (c in 0..4) {
      assertEquals(5, card.grid[c].size)
    }

    // Free space at [2][2] is 0
    assertEquals(0, card.grid[2][2])
    assertTrue(card.isMarked(2, 2))

    // B column is 1..15
    for (r in 0..4) {
      val b = card.grid[0][r]
      assertTrue("B column number $b out of range", b in 1..15)
    }

    // O column is 61..75
    for (r in 0..4) {
      val o = card.grid[4][r]
      assertTrue("O column number $o out of range", o in 61..75)
    }
  }

  @Test
  fun testCallingSequence() {
    val seq = BingoEngine.generateCallingSequence(seed = 9999L)
    assertEquals(75, seq.size)
    assertEquals((1..75).toSet(), seq.toSet())
  }

  @Test
  fun testAmharicNumerals() {
    assertEquals("አንድ", StringsDictionary.amharicNumber(1))
    assertEquals("አስር", StringsDictionary.amharicNumber(10))
    assertEquals("አስራ አምስት", StringsDictionary.amharicNumber(15))
    assertEquals("ሃያ አንድ", StringsDictionary.amharicNumber(21))
    assertEquals("ሰባ አምስት", StringsDictionary.amharicNumber(75))
  }

  @Test
  fun testAmharicBallPhoneticCalls() {
    fun callFor(n: Int): String {
      val letter = StringsDictionary.getBallLetter(n)
      val amL = StringsDictionary.AM_LETTERS[letter] ?: letter
      return "$amL ${StringsDictionary.amharicNumber(n)}"
    }

    assertEquals("ቢ አንድ", callFor(1))
    assertEquals("ቢ አስራ አምስት", callFor(15))
    assertEquals("አይ አስራ ስድስት", callFor(16))
    assertEquals("ኤን ሰላሳ አምስት", callFor(35))
    assertEquals("ጂ ሃምሳ", callFor(50))
    assertEquals("ኦ ሰባ አምስት", callFor(75))
  }

  @Test
  fun testAllGoalsConfigured() {
    val goals = StringsDictionary.ALL_GOALS
    assertEquals(17, goals.size)

    val anyLine = goals.first { it.id == "any_line" }
    val card = BingoEngine.generateCard(1, 42L)

    val marked = setOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(3, 0), Pair(4, 0))
    val markedCard = card.copy(markedCells = marked)
    val winCells = BingoEngine.checkWin(markedCard, anyLine)
    assertNotNull(winCells)
  }

  @Test
  fun testTelebirrMerchantConfiguration() {
    assertEquals("0939383425", TelebirrConfig.MERCHANT_PHONE)
    assertEquals("0939383425", TelebirrConfig.MERCHANT_ACCOUNT)
    assertEquals("Yemane", TelebirrConfig.MERCHANT_NAME)
    assertEquals(30, TelebirrConfig.BIRR_PER_CARTELA)
    assertEquals(30, TelebirrConfig.MIN_PAYMENT_BIRR)
    assertEquals("*127#", TelebirrConfig.USSD_TRANSFER_CODE)
  }

  @Test
  fun testTelebirrOrderCreationAndCryptographicSignature() {
    val orderId = "YEME-20261103-TEST01"
    val username = "Abebe1"
    val amountBirr = 60
    val timestamp = 1730660000000L

    val signature1 = TelebirrSecurity.generateOrderSignature(orderId, username, amountBirr, timestamp)
    val signature2 = TelebirrSecurity.generateOrderSignature(orderId, username, amountBirr, timestamp)

    assertEquals(signature1, signature2)
    assertEquals(64, signature1.length)

    val validOrder = TelebirrOrder(
      orderId = orderId,
      username = username,
      payerPhone = "0911223344",
      cartelaCount = 2,
      totalAmountBirr = amountBirr,
      timestamp = timestamp,
      status = PaymentOrderStatus.CREATED,
      securityToken = signature1
    )
    assertTrue(TelebirrSecurity.verifyOrderIntegrity(validOrder))

    val tamperedOrder = validOrder.copy(totalAmountBirr = 30)
    assertFalse(TelebirrSecurity.verifyOrderIntegrity(tamperedOrder))
  }

  @Test
  fun testTelebirrTransactionIdExtraction() {
    // 1. Raw transaction ID
    assertEquals("TB20261103A1B2", TelebirrSecurity.extractTransactionId("TB20261103A1B2"))

    // 2. Telebirr receipt URL
    val url = "https://transactioninfo.ethiotelecom.et/receipt/CL240899128"
    assertEquals("CL240899128", TelebirrSecurity.extractTransactionId(url))

    // 3. Receipt URL with trailing slash
    val urlSlash = "https://transactioninfo.ethiotelecom.et/receipt/CL240899128/"
    assertEquals("CL240899128", TelebirrSecurity.extractTransactionId(urlSlash))

    // 4. Typical Ethio Telecom SMS body
    val sms = "Dear customer, you have transferred 60.00 ETB to 0939383425 (Yemane). Transaction number: CL992837410. Thank you for using telebirr."
    assertEquals("CL992837410", TelebirrSecurity.extractTransactionId(sms))

    // 5. Empty / whitespace string returns null
    assertNull(TelebirrSecurity.extractTransactionId("   "))
  }

  @Test
  fun testPhoneNormalization() {
    assertEquals("0939383425", TelebirrSecurity.normalizePhone("0939383425"))
    assertEquals("0939383425", TelebirrSecurity.normalizePhone("+251939383425"))
    assertEquals("0939383425", TelebirrSecurity.normalizePhone("251939383425"))
    assertEquals("0939383425", TelebirrSecurity.normalizePhone("09 39 38 34 25"))
    assertEquals("0911223344", TelebirrSecurity.normalizePhone("0911223344"))
  }

  @Test
  fun testReceiverValidationAgainstAccountHolder() {
    // 1. Correct recipient in English SMS: Yemane (0939383425)
    val validSms1 = "Dear customer, you have transferred 60.00 ETB to Yemane (0939383425). Transaction number: CL240899128."
    val audit1 = TelebirrSecurity.validateRecipient(validSms1)
    assertTrue("Should validate recipient Yemane / 0939383425", audit1.isMatched)
    assertEquals("0939383425", audit1.detectedPhone)

    // 2. Correct recipient in Amharic SMS: ወደ 0939383425 (Yemane)
    val validSms2 = "ብር 30.00 ወደ 0939383425 (Yemane) በስኬት አስተላልፈዋል። የሂሳብ ማረጋገጫ ቁጥር CL11223344 ነው።"
    val audit2 = TelebirrSecurity.validateRecipient(validSms2)
    assertTrue("Should validate Amharic recipient format", audit2.isMatched)
    assertEquals("0939383425", audit2.detectedPhone)

    // 3. WRONG RECIPIENT: Transferred to different phone number
    val wrongPhoneSms = "Dear customer, you transferred 60.00 ETB to Abebe (0911223344). Transaction number: CL999888777."
    val audit3 = TelebirrSecurity.validateRecipient(wrongPhoneSms)
    assertFalse("Must reject transfers to non-account holder phone", audit3.isMatched)
    assertNotNull(audit3.failureReason)
    assertTrue(audit3.failureReason!!.contains("0911223344"))

    // 4. WRONG RECIPIENT: Transferred to different recipient name
    val wrongNameSms = "You transferred 30.00 ETB to Kebede (0912345678). Ref: TB88889999."
    val audit4 = TelebirrSecurity.validateRecipient(wrongNameSms)
    assertFalse("Must reject transfers to non-account holder name", audit4.isMatched)

    // 5. Short transaction ID or direct URL passes local recipient pre-check (deferred to online check)
    val shortTx = "CL240899128"
    val audit5 = TelebirrSecurity.validateRecipient(shortTx)
    assertTrue("Pure transaction ID should be deferred to online check", audit5.isMatched)
  }

  @Test
  fun testAmountExtraction() {
    assertEquals(60, TelebirrSecurity.extractAmount("transferred 60.00 ETB to Yemane"))
    assertEquals(60, TelebirrSecurity.extractAmount("transferred 60 ETB to Yemane"))
    assertEquals(90, TelebirrSecurity.extractAmount("ብር 90.00 ወደ 0939383425"))
    assertEquals(30, TelebirrSecurity.extractAmount("Amount: 30 ETB"))
    assertEquals(120, TelebirrSecurity.extractAmount("You paid 120 Birr"))
  }

  @Test
  fun testEthioTelecomReceiptHtmlParsing() {
    // 1. Valid HTML response
    val validHtml = """
      <html>
        <body>
          <div class="title">Ethio Telecom Electronic Receipt</div>
          <div class="row"><span>Transaction No:</span> <span>CL240899128</span></div>
          <div class="row"><span>Status:</span> <span>Success</span></div>
          <div class="row"><span>Credited Party:</span> <span>0939383425 (Yemane)</span></div>
          <div class="row"><span>Amount:</span> <span>60.00 ETB</span></div>
        </body>
      </html>
    """.trimIndent()

    val parsed1 = TelebirrSecurity.parseEthioTelecomReceiptHtml(validHtml)
    assertTrue(parsed1.isValid)
    assertTrue(parsed1.isStatusSuccess)
    assertEquals("CL240899128", parsed1.transactionId)
    assertEquals(60, parsed1.amountBirr)
    assertEquals("0939383425", parsed1.creditedPartyPhone)

    // 2. Receipt Not Found HTML
    val notFoundHtml = "<html><body><h1>404 Not Found</h1><p>Receipt not found in database.</p></body></html>"
    val parsed2 = TelebirrSecurity.parseEthioTelecomReceiptHtml(notFoundHtml)
    assertFalse(parsed2.isValid)
    assertNotNull(parsed2.failureReason)

    // 3. Failed / Cancelled transaction
    val failedHtml = """
      <div><span>Transaction No:</span> CL000111222</div>
      <div><span>Status:</span> Failed</div>
      <div><span>Credited Party:</span> 0939383425</div>
    """.trimIndent()
    val parsed3 = TelebirrSecurity.parseEthioTelecomReceiptHtml(failedHtml)
    assertFalse(parsed3.isValid)
    assertFalse(parsed3.isStatusSuccess)
  }

  @Test
  fun testTelebirrOrderReferenceFormat() {
    val ref = TelebirrSecurity.generateOrderReference()
    assertTrue("Order reference should start with YEME-", ref.startsWith("YEME-"))
    val parts = ref.split("-")
    assertEquals(3, parts.size)
    assertEquals(8, parts[1].length) // yyyyMMdd
    assertEquals(6, parts[2].length) // 6 hex chars
  }
}
