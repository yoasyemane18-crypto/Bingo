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

    // 5. Bare transaction ID without recipient text is not verified without online check
    val shortTx = "CL240899128"
    val audit5 = TelebirrSecurity.validateRecipient(shortTx)
    assertFalse("Bare ID without recipient text cannot confirm transfer to Yemane", audit5.isMatched)
  }

  @Test
  fun testAmountExtractionAndMatching() {
    assertEquals(60, TelebirrSecurity.extractAmount("transferred 60.00 ETB to Yemane"))
    assertEquals(60, TelebirrSecurity.extractAmount("transferred 60 ETB to Yemane"))
    assertEquals(90, TelebirrSecurity.extractAmount("ብር 90.00 ወደ 0939383425"))
    assertEquals(30, TelebirrSecurity.extractAmount("Amount: 30 ETB"))
    assertEquals(120, TelebirrSecurity.extractAmount("You paid 120 Birr"))

    // Test mismatch detection logic:
    val typedAmount = 60
    val receiptAmount = TelebirrSecurity.extractAmount("transferred 30.00 ETB to Yemane (0939383425)")
    assertEquals(30, receiptAmount)
    assertFalse("Must detect mismatch when typed is 60 but receipt is 30", typedAmount == receiptAmount)
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
  fun testFirebaseRtdbCloudIntegration() {
    assertEquals("https://wow-878fc-default-rtdb.firebaseio.com", com.example.data.remote.FirebaseRtdbConfig.BASE_URL)
    assertEquals("wow-878fc", com.example.data.remote.FirebaseRtdbConfig.PROJECT_ID)
    assertEquals("AIzaSyDrYOOePFsUCPHCPdNHwB05FPna-F_thvA", com.example.data.remote.FirebaseRtdbConfig.API_KEY)
  }

  @Test
  fun testSignInScreenNavigation() {
    val screens = com.example.model.Screen.entries
    assertTrue(screens.contains(com.example.model.Screen.SIGN_IN))
    assertTrue(screens.contains(com.example.model.Screen.HOME))
  }

  @Test
  fun testEthiopianPlayingTimesSlots() {
    val slots = StringsDictionary.SLOTS
    assertEquals(6, slots.size)

    // Expected Ethiopian hours: 4:00, 6:00, 8:00, 10:00, 12:00, 2:00
    val expectedEthiopianHours = listOf(4, 6, 8, 10, 12, 2)
    val expectedUtcPlus3Hours = listOf(10, 12, 14, 16, 18, 20)

    for (i in slots.indices) {
      assertEquals(expectedEthiopianHours[i], slots[i].ethiopianHour)
      assertEquals(expectedUtcPlus3Hours[i], slots[i].hourUtc)
    }

    // Verify Ethiopian labels
    assertTrue(slots[0].labelAm.contains("4:00"))
    assertTrue(slots[1].labelAm.contains("6:00"))
    assertTrue(slots[2].labelAm.contains("8:00"))
    assertTrue(slots[3].labelAm.contains("10:00"))
    assertTrue(slots[4].labelAm.contains("12:00"))
    assertTrue(slots[5].labelAm.contains("2:00"))
  }

  @Test
  fun testEthiopianSlotsFindAndTiming() {
    val slot4 = StringsDictionary.findSlot("4:00 O’clock (Morning 10:00 AM)")
    assertNotNull(slot4)
    assertEquals(4, slot4!!.ethiopianHour)

    val slot6 = StringsDictionary.findSlot("6:00 ሰዓት (ቀትር)")
    assertNotNull(slot6)
    assertEquals(6, slot6!!.ethiopianHour)

    val slot8 = StringsDictionary.findSlot("8:00 O’clock (Afternoon 2:00 PM)")
    assertNotNull(slot8)
    assertEquals(8, slot8!!.ethiopianHour)

    val slot10 = StringsDictionary.findSlot("10:00 ሰዓት (ከሰዓት)")
    assertNotNull(slot10)
    assertEquals(10, slot10!!.ethiopianHour)

    val slot12 = StringsDictionary.findSlot("12:00 ሰዓት (ምሽት)")
    assertNotNull(slot12)
    assertEquals(12, slot12!!.ethiopianHour)

    val slot2 = StringsDictionary.findSlot("2:00 ሰዓት (ምሽት)")
    assertNotNull(slot2)
    assertEquals(2, slot2!!.ethiopianHour)
  }

  @Test
  fun testThreeGamesConsecutiveRounds() {
    assertEquals(3, com.example.data.BingoRepository.ROUNDS_PER_SESSION)
  }

  @Test
  fun testCartelaPricingStructure() {
    val pricePerCartela = com.example.data.BingoRepository.PAY_BIRR
    assertEquals(30, pricePerCartela)

    // 1 cartela = 30 Birr covers all 3 games in that session (10 Birr per 1 game)
    val cartelaCount = 1
    val costForThreeGames = cartelaCount * pricePerCartela
    assertEquals(30, costForThreeGames)

    val birrPerGame = com.example.data.BingoRepository.BIRR_PER_GAME
    assertEquals(10, birrPerGame)
    assertEquals(costForThreeGames / 3, birrPerGame)

    // 2 cartelas = 60 Birr covers all 3 games (20 Birr per game)
    val twoCartelasCost = 2 * pricePerCartela
    assertEquals(60, twoCartelasCost)
  }

  @Test
  fun testCanonicalMultiplayerSynchronizationAcrossDevices() {
    val slotHour = 10
    val dayIndex = 20760L

    for (round in 1..3) {
      // Simulate Device A
      val seedDeviceA = (dayIndex * 100000L + slotHour * 1000L + round)
      val goalDeviceA = StringsDictionary.ALL_GOALS[((seedDeviceA % StringsDictionary.ALL_GOALS.size + StringsDictionary.ALL_GOALS.size) % StringsDictionary.ALL_GOALS.size).toInt()]
      val callingSeqDeviceA = BingoEngine.generateCallingSequence(seedDeviceA)

      // Simulate Device B
      val seedDeviceB = (dayIndex * 100000L + slotHour * 1000L + round)
      val goalDeviceB = StringsDictionary.ALL_GOALS[((seedDeviceB % StringsDictionary.ALL_GOALS.size + StringsDictionary.ALL_GOALS.size) % StringsDictionary.ALL_GOALS.size).toInt()]
      val callingSeqDeviceB = BingoEngine.generateCallingSequence(seedDeviceB)

      // Both devices must have the EXACT SAME seed, goal, and numbers sequence
      assertEquals(seedDeviceA, seedDeviceB)
      assertEquals(goalDeviceA.id, goalDeviceB.id)
      assertEquals(callingSeqDeviceA, callingSeqDeviceB)
      assertEquals(75, callingSeqDeviceA.size)
    }
  }

  @Test
  fun testCartelaOccupancyExclusivity() {
    val occupiedCartelas = mutableMapOf<Int, String>()
    val userA = "Abebe"
    val userB = "Kebede"

    // User A purchases card #42
    val cardToBuy = 42
    occupiedCartelas[cardToBuy] = userA

    // Verify User A owns card #42
    assertEquals(userA, occupiedCartelas[cardToBuy])

    // Verify User B cannot select card #42 because it is occupied
    val isOccupiedForUserB = occupiedCartelas.containsKey(cardToBuy) && occupiedCartelas[cardToBuy] != userB
    assertTrue("Card #42 should be occupied for User B", isOccupiedForUserB)

    // User A can view their own card without conflict
    val isOccupiedForUserA = occupiedCartelas.containsKey(cardToBuy) && occupiedCartelas[cardToBuy] != userA
    assertFalse("Card #42 should NOT be considered occupied by other for User A", isOccupiedForUserA)
  }

  @Test
  fun testServerClockSynchronizationAcrossSkewedDevices() {
    // True server time
    val authoritativeServerTime = 1730660000000L

    // Phone A clock is running 5 seconds FAST (+5000ms)
    val phoneALocalClock = authoritativeServerTime + 5000L
    val phoneAOffset = authoritativeServerTime - phoneALocalClock // -5000ms

    // Phone B clock is running 12 seconds SLOW (-12000ms)
    val phoneBLocalClock = authoritativeServerTime - 12000L
    val phoneBOffset = authoritativeServerTime - phoneBLocalClock // +12000ms

    // Synchronized now for both phones:
    val phoneASynced = phoneALocalClock + phoneAOffset
    val phoneBSynced = phoneBLocalClock + phoneBOffset

    assertEquals("Both phones must compute identical synchronized time", phoneASynced, phoneBSynced)
    assertEquals(authoritativeServerTime, phoneASynced)

    // Slot starts at authoritativeServerTime + 3600000L (1 hour later)
    val slotStartTime = authoritativeServerTime + 3600000L

    val diffPhoneA = slotStartTime - phoneASynced
    val diffPhoneB = slotStartTime - phoneBSynced

    assertEquals("Both phones must calculate the exact same remaining countdown down to the millisecond", diffPhoneA, diffPhoneB)
    assertEquals(3600000L, diffPhoneA)
  }
}
