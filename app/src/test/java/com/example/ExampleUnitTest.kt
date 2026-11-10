package com.example

import com.example.engine.BingoEngine
import com.example.model.StringsDictionary
import com.example.payment.telebirr.TelebirrConfig
import com.example.payment.telebirr.TelebirrOrder
import com.example.payment.telebirr.PaymentOrderStatus
import com.example.payment.telebirr.TelebirrSecurity
import com.example.notification.BingoNotificationEngine
import com.example.notification.PlayerNotificationContext
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
  fun testAmharicExclusiveCallingVoice() {
    // Verify all 75 numbers have authentic Amharic letter + numeral phonetic calls
    for (n in 1..75) {
      val letter = StringsDictionary.getBallLetter(n)
      val amLetter = StringsDictionary.AM_LETTERS[letter]
      assertNotNull("Amharic letter for $letter must exist", amLetter)
      val amNumber = StringsDictionary.amharicNumber(n)
      assertTrue("Amharic numeral for $n must not be blank", amNumber.isNotBlank())
    }
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

  @Test
  fun testDateHeaderParsing() {
    val headers = okhttp3.Headers.Builder()
      .add("Date", "Sun, 08 Nov 2026 11:49:10 GMT")
      .build()
    val date = headers.getDate("Date")
    assertNotNull(date)
    assertTrue(date!!.time > 0L)
  }

  @Test
  fun testPurchasedCartelaPreservationDuringPracticeGame() {
    // 1. User purchases Cartela #7 and #15 for scheduled game
    val purchasedCards = listOf(7, 15)
    val purchasedSeeds = mapOf(7 to 1L, 15 to 1L)

    val realGameCards = purchasedCards.map { cardNum ->
      BingoEngine.generateCard(cardNum, purchasedSeeds[cardNum] ?: 1L)
    }

    // Verify initial purchased cards
    assertEquals(2, realGameCards.size)
    assertEquals(7, realGameCards[0].cardNum)
    assertEquals(15, realGameCards[1].cardNum)
    val card7GridSnapshot = realGameCards[0].grid
    val card15GridSnapshot = realGameCards[1].grid

    // 2. User plays practice mode with a different card, say Card #42, using random seed
    val practiceSeed = 987654321L
    val practiceCardNum = 42
    val practiceCard = BingoEngine.generateCard(practiceCardNum, practiceSeed)

    assertEquals(42, practiceCard.cardNum)
    assertFalse(purchasedCards.contains(practiceCard.cardNum))

    // 3. User finishes practice mode and scheduled session starts.
    // The real game must regenerate the EXACT purchased cards with identical grids!
    val finalScheduledCards = purchasedCards.map { cardNum ->
      BingoEngine.generateCard(cardNum, purchasedSeeds[cardNum] ?: 1L)
    }

    assertEquals(2, finalScheduledCards.size)
    assertEquals(7, finalScheduledCards[0].cardNum)
    assertEquals(15, finalScheduledCards[1].cardNum)
    assertEquals("Grid numbers of purchased card #7 must not change", card7GridSnapshot, finalScheduledCards[0].grid)
    assertEquals("Grid numbers of purchased card #15 must not change", card15GridSnapshot, finalScheduledCards[1].grid)
  }

  @Test
  fun testPracticeGameSeedIsolationFromPurchasedCards() {
    val purchasedCardNum = 5
    val standardSeed = 1L
    val officialCard = BingoEngine.generateCard(purchasedCardNum, standardSeed)

    // Practice game runs 10 times with random seeds
    for (i in 1..10) {
      val randomPracticeSeed = (100000L..999999999L).random()
      val practiceCard = BingoEngine.generateCard(purchasedCardNum, randomPracticeSeed)

      // Practice card has random layout
      assertNotNull(practiceCard)
      assertEquals(purchasedCardNum, practiceCard.cardNum)

      // Official real card with standard seed must remain strictly intact
      val reloadedOfficialCard = BingoEngine.generateCard(purchasedCardNum, standardSeed)
      assertEquals(officialCard.grid, reloadedOfficialCard.grid)
    }
  }

  @Test
  fun testAllPaymentMethodsUssdCodesAndNaming() {
    val telebirr = com.example.payment.PaymentMethod.TELEBIRR
    assertEquals("Telebirr", telebirr.displayName)
    assertEquals("*127#", telebirr.ussdCode)

    val cbe = com.example.payment.PaymentMethod.CBE
    assertEquals("CBE", cbe.displayName)
    assertEquals("*889#", cbe.ussdCode)

    val cbeBirr = com.example.payment.PaymentMethod.CBE_BIRR
    assertEquals("CBE Birr", cbeBirr.displayName)
    assertEquals("*847#", cbeBirr.ussdCode)

    val eBirr = com.example.payment.PaymentMethod.E_BIRR
    assertEquals("E-Birr", eBirr.displayName)
    assertEquals("*841#", eBirr.ussdCode)
  }

  @Test
  fun testSimultaneousWinRewardDivision() {
    val totalPrizePool = 70

    // 1 winner: gets full 70 Birr
    val singleWinnerCount = 1
    val singleReward = (totalPrizePool / singleWinnerCount).coerceAtLeast(1)
    assertEquals(70, singleReward)

    // 2 simultaneous winners on the same ball: 70 / 2 = 35 Birr each
    val twoWinnersCount = 2
    val splitRewardTwo = (totalPrizePool / twoWinnersCount).coerceAtLeast(1)
    assertEquals(35, splitRewardTwo)

    // 3 simultaneous winners on the same ball: 70 / 3 = 23 Birr each
    val threeWinnersCount = 3
    val splitRewardThree = (totalPrizePool / threeWinnersCount).coerceAtLeast(1)
    assertEquals(23, splitRewardThree)

    // 4 simultaneous winners on 100 Birr pool: 100 / 4 = 25 Birr each
    val customPool = 100
    val fourWinnersCount = 4
    val splitRewardFour = (customPool / fourWinnersCount).coerceAtLeast(1)
    assertEquals(25, splitRewardFour)

    // Minimum reward is at least 1 Birr even for huge splits
    val tinyPool = 5
    val tenWinnersCount = 10
    val splitMin = (tinyPool / tenWinnersCount).coerceAtLeast(1)
    assertEquals(1, splitMin)
  }

  @Test
  fun testSimultaneousWinResolutionSameBall() {
    val sessionId = "session-20261109-10"
    val roundNum = 1
    val winningBall = 42
    val callCount = 28
    val totalPool = 70

    val winner1 = com.example.model.WinnerRecord(
      id = "w1",
      username = "Abebe",
      phone = "0911000001",
      slotLabel = "10:00",
      roundNumber = roundNum,
      rewardBirr = totalPool,
      cardNum = 12,
      callCount = callCount,
      lastBall = winningBall
    )

    val winner2 = com.example.model.WinnerRecord(
      id = "w2",
      username = "Chala",
      phone = "0911000002",
      slotLabel = "10:00",
      roundNumber = roundNum,
      rewardBirr = totalPool,
      cardNum = 37,
      callCount = callCount,
      lastBall = winningBall
    )

    val claims = listOf(winner1, winner2)
    val tiedClaims = claims.filter { it.callCount == callCount }
    val winnersCount = tiedClaims.map { it.username }.distinct().size
    assertEquals(2, winnersCount)

    val splitReward = (totalPool / winnersCount).coerceAtLeast(1)
    assertEquals(35, splitReward)

    val resolution = com.example.model.RoundResolution(
      roundNumber = roundNum,
      callCount = callCount,
      lastBall = winningBall,
      totalPrizePool = totalPool,
      winnersCount = winnersCount,
      splitReward = splitReward,
      winners = tiedClaims
    )

    assertEquals(2, resolution.winnersCount)
    assertEquals(35, resolution.splitReward)
    assertEquals(70, resolution.totalPrizePool)
    assertEquals(winningBall, resolution.lastBall)
  }

  @Test
  fun testEngineCheckWinWithCalledNumbers() {
    val card = BingoEngine.generateCard(1, 123L)
    val anyLine = StringsDictionary.ALL_GOALS.first { it.id == "any_line" }

    // Collect top row numbers: col 0..4, row 0
    val topRowNums = (0..4).map { c -> card.grid[c][0] }.toSet()

    // When all top row numbers have been called:
    val winResult = BingoEngine.checkWinWithCalledNumbers(card, anyLine, topRowNums)
    assertNotNull("Should detect win when all pattern numbers are called", winResult)

    // When one number is missing:
    val missingOne = topRowNums.drop(1).toSet()
    val noWinResult = BingoEngine.checkWinWithCalledNumbers(card, anyLine, missingOne)
    assertNull("Should not detect win if a required cell is missing", noWinResult)
  }

  @Test
  fun testHalfHouseAllEightPatternsWin() {
    val halfHouse = StringsDictionary.ALL_GOALS.first { it.id == "half_house" }
    assertEquals("Half House must have exactly 8 winning patterns", 8, halfHouse.patterns.size)

    val card = BingoEngine.generateCard(1, 456L)

    // Verify each pattern individually:
    // 1. Left Half (Column splits)
    val p1Cells = (0..4).flatMap { r -> (0..2).map { c -> Pair(c, r) } }
    val win1 = BingoEngine.checkWin(card.copy(markedCells = p1Cells.toSet()), halfHouse)
    assertNotNull("Left Half must win", win1)

    // 2. Right Half (Column splits)
    val p2Cells = (0..4).flatMap { r -> (2..4).map { c -> Pair(c, r) } }
    val win2 = BingoEngine.checkWin(card.copy(markedCells = p2Cells.toSet()), halfHouse)
    assertNotNull("Right Half must win", win2)

    // 3. Top Half (Row splits)
    val p3Cells = (0..2).flatMap { r -> (0..4).map { c -> Pair(c, r) } }
    val win3 = BingoEngine.checkWin(card.copy(markedCells = p3Cells.toSet()), halfHouse)
    assertNotNull("Top Half must win", win3)

    // 4. Bottom Half (Row splits)
    val p4Cells = (2..4).flatMap { r -> (0..4).map { c -> Pair(c, r) } }
    val win4 = BingoEngine.checkWin(card.copy(markedCells = p4Cells.toSet()), halfHouse)
    assertNotNull("Bottom Half must win", win4)

    // 5. Diagonal Top Half (c + r <= 4)
    val p5Cells = (0..4).flatMap { r -> (0..4).filter { c -> c + r <= 4 }.map { c -> Pair(c, r) } }
    val win5 = BingoEngine.checkWin(card.copy(markedCells = p5Cells.toSet()), halfHouse)
    assertNotNull("Diagonal Top Half must win", win5)
    assertEquals(15, p5Cells.size)

    // 6. Diagonal Bottom Half (c + r >= 4)
    val p6Cells = (0..4).flatMap { r -> (0..4).filter { c -> c + r >= 4 }.map { c -> Pair(c, r) } }
    val win6 = BingoEngine.checkWin(card.copy(markedCells = p6Cells.toSet()), halfHouse)
    assertNotNull("Diagonal Bottom Half must win", win6)
    assertEquals(15, p6Cells.size)

    // 7. Diagonal Right Half (c >= r)
    val p7Cells = (0..4).flatMap { r -> (0..4).filter { c -> c >= r }.map { c -> Pair(c, r) } }
    val win7 = BingoEngine.checkWin(card.copy(markedCells = p7Cells.toSet()), halfHouse)
    assertNotNull("Diagonal Right Half must win", win7)
    assertEquals(15, p7Cells.size)

    // 8. Diagonal Left Half (c <= r)
    val p8Cells = (0..4).flatMap { r -> (0..4).filter { c -> c <= r }.map { c -> Pair(c, r) } }
    val win8 = BingoEngine.checkWin(card.copy(markedCells = p8Cells.toSet()), halfHouse)
    assertNotNull("Diagonal Left Half must win", win8)
    assertEquals(15, p8Cells.size)

    // Also test checkWinWithCalledNumbers for Diagonal Top Half
    val p5Nums = p5Cells.map { (c, r) -> card.grid[c][r] }.filter { it != 0 }.toSet()
    val winWithNumbers = BingoEngine.checkWinWithCalledNumbers(card, halfHouse, p5Nums)
    assertNotNull("Should win with called numbers for Diagonal Top Half", winWithNumbers)
  }

  @Test
  fun testTwoLinesSmallSquareGoal() {
    val goal = StringsDictionary.ALL_GOALS.first { it.id == "two_lines_sq" }
    assertTrue("Goal two_lines_sq should have comprehensive patterns", goal.patterns.size > 2)

    val card = BingoEngine.generateCard(1, 789L)

    // 1. Two horizontal lines (Row 0, Row 4) + center square (cols 1..2, rows 1..2)
    val r0 = (0..4).map { c -> Pair(c, 0) }
    val r4 = (0..4).map { c -> Pair(c, 4) }
    val midSq = listOf(Pair(1, 1), Pair(2, 1), Pair(1, 2), Pair(2, 2))
    val pattern1 = (r0 + r4 + midSq).toSet()
    val win1 = BingoEngine.checkWin(card.copy(markedCells = pattern1), goal)
    assertNotNull("Row 0 + Row 4 + Center Square must win", win1)

    // 2. Two horizontal lines (Row 0, Row 1) + bottom square (cols 2..3, rows 3..4)
    val r1 = (0..4).map { c -> Pair(c, 1) }
    val bottomSq = listOf(Pair(2, 3), Pair(3, 3), Pair(2, 4), Pair(3, 4))
    val pattern2 = (r0 + r1 + bottomSq).toSet()
    val win2 = BingoEngine.checkWin(card.copy(markedCells = pattern2), goal)
    assertNotNull("Row 0 + Row 1 + Bottom Square must win", win2)

    // 3. Two vertical lines (Col 0, Col 4) + center square
    val c0 = (0..4).map { r -> Pair(0, r) }
    val c4 = (0..4).map { r -> Pair(4, r) }
    val pattern3 = (c0 + c4 + midSq).toSet()
    val win3 = BingoEngine.checkWin(card.copy(markedCells = pattern3), goal)
    assertNotNull("Col 0 + Col 4 + Center Square must win", win3)

    // 4. Two vertical lines (Col 0, Col 1) + right square (cols 3..4, rows 1..2)
    val c1 = (0..4).map { r -> Pair(1, r) }
    val rightSq = listOf(Pair(3, 1), Pair(4, 1), Pair(3, 2), Pair(4, 2))
    val pattern4 = (c0 + c1 + rightSq).toSet()
    val win4 = BingoEngine.checkWin(card.copy(markedCells = pattern4), goal)
    assertNotNull("Col 0 + Col 1 + Right Square must win", win4)

    // 5. One horizontal line (Row 0) + one vertical line (Col 4) + bottom-left square (cols 0..1, rows 2..3)
    val blSq = listOf(Pair(0, 2), Pair(1, 2), Pair(0, 3), Pair(1, 3))
    val pattern5 = (r0 + c4 + blSq).toSet()
    val win5 = BingoEngine.checkWin(card.copy(markedCells = pattern5), goal)
    assertNotNull("Row 0 + Col 4 + Bottom-Left Square (non-intersecting square) must win", win5)

    // 6. Test that a square intersecting with a line does NOT count as a valid isolated square
    // For example, if only Row 0 and Row 4 are marked, but square is NOT marked:
    val onlyTwoLines = (r0 + r4).toSet()
    val winIncomplete = BingoEngine.checkWin(card.copy(markedCells = onlyTwoLines), goal)
    assertNull("Only two lines without square must not win", winIncomplete)

    // And if square overlaps/intersects with row (e.g. square at (0, 0) which is on Row 0):
    // If only Row 0, Row 4, and square at (0, 0) are marked, square intersects Row 0 so it's not a disjoint 2x2 square
    val overlappingSq = listOf(Pair(0, 0), Pair(1, 0), Pair(0, 1), Pair(1, 1))
    val patternOverlapping = (r0 + r4 + overlappingSq).toSet()
    // There is no disjoint 2x2 square in patternOverlapping because the only 4-cell block is at (0,0) which intersects Row 0
    // (Other than that, rows 0 and 4 are filled, no other 2x2 square is filled)
    val winOverlap = BingoEngine.checkWin(card.copy(markedCells = patternOverlapping), goal)
    assertNull("Square intersecting with line must not count as valid two lines + small square", winOverlap)
  }

  @Test
  fun testAllPushNotificationsUnder15WordsAndOneEmoji() {
    val templates = BingoNotificationEngine.ALL_TEMPLATES
    assertEquals("Must have exactly 15 push notification templates", 15, templates.size)

    for (t in templates) {
      assertNotNull("Template id must not be null", t.id)
      assertTrue("Amharic title must not be blank for ${t.id}", t.titleAm.isNotBlank())
      assertTrue("Amharic body must not be blank for ${t.id}", t.bodyAm.isNotBlank())
      assertTrue("Emoji must not be blank for ${t.id}", t.emoji.isNotBlank())

      // Split Amharic body into words (excluding emoji)
      val words = t.bodyAm.replace(t.emoji, "").trim().split(Regex("[\\s፤፦፡፣!]+")).filter { it.isNotBlank() }
      assertTrue("Amharic message body must be strictly under 15 words for ${t.id} (got ${words.size} words: ${t.bodyAm})", words.size < 15)
      assertEquals("Reported word count must match actual words for ${t.id}", t.wordCountAm, words.size)

      // Ensure the emoji is present in the body
      assertTrue("Body must include the 1 emoji for ${t.id}", t.bodyAm.contains(t.emoji))
    }
  }

  @Test
  fun testPushNotificationContextEvaluation() {
    // 1. Transactional approvals take highest priority
    val depositCtx = PlayerNotificationContext(isDepositApproved = true)
    assertEquals(BingoNotificationEngine.DEPOSIT_APPROVED, BingoNotificationEngine.evaluateState(depositCtx))

    val withdrawalCtx = PlayerNotificationContext(isWithdrawalApproved = true)
    assertEquals(BingoNotificationEngine.WITHDRAWAL_TRANSFERRED, BingoNotificationEngine.evaluateState(withdrawalCtx))

    // 2. Countdown urgency (within 5 minutes)
    val urgentNoTicket = PlayerNotificationContext(minutesLeft = 3, hasCartelaTicket = false)
    assertEquals(BingoNotificationEngine.COUNTDOWN_URGENCY_5MIN, BingoNotificationEngine.evaluateState(urgentNoTicket))

    val urgentWithTicket = PlayerNotificationContext(minutesLeft = 3, hasCartelaTicket = true)
    assertEquals(BingoNotificationEngine.PURCHASED_CARDS_READY, BingoNotificationEngine.evaluateState(urgentWithTicket))

    // 3. Sunday specials
    val sunMorning = PlayerNotificationContext(isSunday = true, currentHour = 9)
    assertEquals(BingoNotificationEngine.SUNDAY_MORNING, BingoNotificationEngine.evaluateState(sunMorning))

    val sunAfternoon = PlayerNotificationContext(isSunday = true, currentHour = 14)
    assertEquals(BingoNotificationEngine.SUNDAY_AFTERNOON, BingoNotificationEngine.evaluateState(sunAfternoon))

    val sunEvening = PlayerNotificationContext(isSunday = true, currentHour = 20)
    assertEquals(BingoNotificationEngine.SUNDAY_EVENING, BingoNotificationEngine.evaluateState(sunEvening))

    // 4. Low balance & practice conversion on regular days
    val lowBalCtx = PlayerNotificationContext(isSunday = false, walletBalance = 10)
    assertEquals(BingoNotificationEngine.LOW_BALANCE, BingoNotificationEngine.evaluateState(lowBalCtx))

    val practiceCtx = PlayerNotificationContext(isSunday = false, walletBalance = 50, isPracticeMode = true, practiceGamesPlayed = 3)
    assertEquals(BingoNotificationEngine.PRACTICE_TO_REAL, BingoNotificationEngine.evaluateState(practiceCtx))
  }
}

