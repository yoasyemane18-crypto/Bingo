package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BingoRepository
import com.example.engine.BingoEngine
import com.example.model.ActiveRound
import com.example.model.AppLanguage
import com.example.model.BingoCard
import com.example.model.BingoGoal
import com.example.model.PaymentRequest
import com.example.model.RequestStatus
import com.example.model.Screen
import com.example.model.StringsDictionary
import com.example.model.UserProfile
import com.example.model.WalletTransaction
import com.example.model.WinnerRecord
import com.example.model.WithdrawalRequest
import com.example.payment.telebirr.PaymentOrderStatus
import com.example.payment.telebirr.TelebirrConfig
import com.example.payment.telebirr.TelebirrOrder
import com.example.payment.telebirr.TelebirrPaymentGateway
import com.example.payment.telebirr.TelebirrSecurity
import com.example.sound.VoiceAndSoundManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class BingoUiState(
  val currentScreen: Screen = Screen.HOME,
  val navigationStack: List<Screen> = listOf(Screen.HOME),
  val language: AppLanguage = AppLanguage.EN,
  val currentUser: UserProfile? = null,
  val isAdminLoggedIn: Boolean = false,

  // Telebirr Payment Gateway
  val activeTelebirrOrder: TelebirrOrder? = null,
  val isTelebirrVerifying: Boolean = false,
  val telebirrErrorMessage: String? = null,
  val showTelebirrGatewayModal: Boolean = false,

  // Schedule & Countdown
  val countdownText: String = "0H:0M:0S",
  val nextGameLabel: String = "",
  val isGameInProgress: Boolean = false,
  val isCountdownFinished: Boolean = false,
  val hasCartelaTicketForSession: Boolean = false,
  val ticketCards: List<Int> = emptyList(),

  // Lobby Card Selection
  val selectedCardNumber: Int = 1,
  val cardSeed: Long = 1L,
  val selectedCardsList: List<Int> = listOf(1),
  val previewCard: BingoCard = BingoEngine.generateCard(1, 1L),
  val lobbyMessage: String = "",

  // In-Game Arena State
  val isPracticeMode: Boolean = true,
  val activeRound: ActiveRound? = null,
  val currentRoundNumber: Int = 1,
  val totalSessionRounds: Int = 3,
  val isIntermission: Boolean = false,
  val intermissionSecondsLeft: Int = 0,
  val sessionSummaryDialog: Boolean = false,
  val sessionTotalWonBirr: Int = 0,
  val targetGoal: BingoGoal = StringsDictionary.ALL_GOALS.first(),
  val activeCards: List<BingoCard> = emptyList(),
  val activeCardIndex: Int = 0,
  val calledNumbers: List<Int> = emptyList(),
  val lastCalledNumber: Int? = null,
  val isAutoMark: Boolean = false,
  val isSoundOn: Boolean = true,
  val voiceLanguage: String = "am", // "am" or "en"
  val readyCountdownStep: String? = null, // "Ready", "3", "2", "1" or null
  val isGameEnded: Boolean = false,
  val winnerCardNum: Int? = null,
  val winnerCallsCount: Int? = null,
  val winnerRewardBirr: Int? = null,
  val winnerNotice: String? = null,
  val showWinDialog: Boolean = false,
  val showConfetti: Boolean = false,
  val gameStatusMessage: String = "",

  // Payment & Withdrawal History
  val paymentRequests: List<PaymentRequest> = emptyList(),
  val withdrawalRequests: List<WithdrawalRequest> = emptyList(),
  val winnersHistory: List<WinnerRecord> = emptyList(),
  val allUsers: List<UserProfile> = emptyList(),

  // Alerts
  val toastMessage: String? = null,
  val alertNotice: String? = null
)

class BingoViewModel(application: Application) : AndroidViewModel(application) {

  val repository = BingoRepository(application)
  val soundManager = VoiceAndSoundManager(application)
  val telebirrGateway = TelebirrPaymentGateway(application)

  private val _uiState = MutableStateFlow(BingoUiState())
  val uiState: StateFlow<BingoUiState> = _uiState.asStateFlow()

  private var countdownJob: Job? = null
  private var gameLoopJob: Job? = null
  private var intermissionJob: Job? = null

  init {
    val lang = repository.getSavedLanguage()
    val sessionName = repository.getCurrentSession()
    val user = sessionName?.let { repository.getUser(it) }

    _uiState.update {
      it.copy(
        language = lang,
        currentUser = user,
        previewCard = BingoEngine.generateCard(1, 1L),
        paymentRequests = repository.getPaymentRequests(),
        withdrawalRequests = repository.getWithdrawalRequests(),
        winnersHistory = repository.getWinners(),
        allUsers = repository.getAllUsernames().mapNotNull { name -> repository.getUser(name) }
      )
    }

    startCountdownTicker()

    // Background sync with real Firebase backend server
    viewModelScope.launch(Dispatchers.IO) {
      repository.syncWithRemoteServer()
      refreshData()
    }
  }

  override fun onCleared() {
    super.onCleared()
    countdownJob?.cancel()
    gameLoopJob?.cancel()
    intermissionJob?.cancel()
    soundManager.release()
  }

  // --- Navigation ---

  fun navigateTo(screen: Screen) {
    if (_uiState.value.currentScreen == screen) return
    _uiState.update {
      it.copy(
        currentScreen = screen,
        navigationStack = it.navigationStack + screen,
        lobbyMessage = ""
      )
    }
    refreshData()
  }

  fun handleBack(): Boolean {
    val stack = _uiState.value.navigationStack
    if (stack.size > 1) {
      val newStack = stack.dropLast(1)
      val prevScreen = newStack.last()
      if (_uiState.value.currentScreen == Screen.GAME) {
        stopGame()
      }
      _uiState.update {
        it.copy(
          currentScreen = prevScreen,
          navigationStack = newStack
        )
      }
      return true
    }
    return false
  }

  fun setLanguage(lang: AppLanguage) {
    repository.saveLanguage(lang)
    _uiState.update { it.copy(language = lang) }
    soundManager.voiceLang = if (lang == AppLanguage.EN) "en" else "am"
  }

  fun toggleSound() {
    val newSound = !soundManager.isSoundEnabled
    soundManager.isSoundEnabled = newSound
    _uiState.update { it.copy(isSoundOn = newSound) }
    if (newSound) {
      soundManager.speak("Sound on", "ድምጽ በርቷል")
    }
  }

  fun toggleVoiceLanguage() {
    val newLang = if (soundManager.voiceLang == "am") "en" else "am"
    soundManager.voiceLang = newLang
    _uiState.update { it.copy(voiceLanguage = newLang) }
    if (newLang == "am") {
      soundManager.announceReady("Ready")
    } else {
      soundManager.speak("Voice: English")
    }
  }

  fun clearToast() {
    _uiState.update { it.copy(toastMessage = null) }
  }

  fun clearAlert() {
    _uiState.update { it.copy(alertNotice = null) }
  }

  fun showToast(msg: String) {
    _uiState.update { it.copy(toastMessage = msg) }
  }

  fun showAlert(msg: String) {
    _uiState.update { it.copy(alertNotice = msg) }
  }

  // --- Countdown & Timer ---

  private fun startCountdownTicker() {
    countdownJob?.cancel()
    countdownJob = viewModelScope.launch {
      while (true) {
        val userSlot = _uiState.value.currentUser?.playingTime
        val nextSlot = repository.getNextSlot(userSlot)
        val activeSession = repository.getActiveSlotSession(userSlot)
        val now = System.currentTimeMillis()

        val isSessionActive = activeSession != null
        val diffMs = if (isSessionActive) 0L else (nextSlot.startTime - now).coerceAtLeast(0L)
        val isFinished = isSessionActive || diffMs == 0L

        val hours = diffMs / 3600000L
        val minutes = (diffMs / 60000L) % 60
        val seconds = (diffMs / 1000L) % 60
        val text = if (isSessionActive) "0H:00M:00S" else "%01dH:%02dM:%02dS".format(hours, minutes, seconds)
        val targetSlot = activeSession?.slot ?: nextSlot.slot

        val wasFinished = _uiState.value.isCountdownFinished
        _uiState.update {
          it.copy(
            countdownText = text,
            nextGameLabel = targetSlot.label(it.language),
            isGameInProgress = isSessionActive,
            isCountdownFinished = isFinished
          )
        }

        // Auto-launch if user reserved cartela and countdown hits 0 while in lobby
        if (!wasFinished && isFinished && _uiState.value.hasCartelaTicketForSession && _uiState.value.currentScreen == Screen.PLAY_LOBBY) {
          startScheduledSession()
        }

        delay(1000L)
      }
    }
  }

  // --- User Login & Registration ---

  fun login(name: String, pass: String): Boolean {
    val trimmed = name.trim()
    var user = repository.getUser(trimmed)
    val hash = repository.hashPassword(pass)
    if (user == null || user.passwordHash != hash) {
      val remoteUser = repository.fetchRemoteUserBlocking(trimmed)
      if (remoteUser != null) {
        user = remoteUser
      }
    }
    if (user == null) {
      showToast("Wrong username or password.")
      return false
    }
    if (user.passwordHash != hash) {
      showToast("Wrong username or password.")
      return false
    }
    repository.setCurrentSession(user.username)
    _uiState.update { it.copy(currentUser = user) }
    navigateTo(Screen.PLAY_LOBBY)
    showToast("Welcome, ${user.username}!")
    return true
  }

  fun register(name: String, phone: String, playingTime: String, pass: String): Boolean {
    val trimmed = name.trim()
    if (trimmed.length < 3) {
      showToast("Username must be at least 3 characters.")
      return false
    }
    if (repository.getUser(trimmed) != null || repository.fetchRemoteUserBlocking(trimmed) != null) {
      showToast("That username is taken.")
      return false
    }
    if (phone.length < 9) {
      showToast("Enter a valid phone number.")
      return false
    }
    if (pass.length < 4) {
      showToast("Password must be at least 4 characters.")
      return false
    }

    val hash = repository.hashPassword(pass)
    val newUser = UserProfile(
      username = trimmed,
      phone = phone.trim(),
      playingTime = playingTime,
      passwordHash = hash,
      walletBalance = 0
    )
    repository.saveUser(newUser)
    repository.setCurrentSession(newUser.username)
    _uiState.update { it.copy(currentUser = newUser) }
    navigateTo(Screen.PLAY_LOBBY)
    showToast("Registration successful! Welcome, ${newUser.username}")
    return true
  }

  fun logout() {
    repository.setCurrentSession(null)
    _uiState.update {
      it.copy(
        currentUser = null,
        selectedCardsList = listOf(1),
        currentScreen = Screen.HOME,
        navigationStack = listOf(Screen.HOME)
      )
    }
    showToast("Signed out.")
  }

  fun changePlayingTime(newTime: String) {
    val user = _uiState.value.currentUser ?: return
    val updated = user.copy(playingTime = newTime)
    repository.saveUser(updated)
    _uiState.update { it.copy(currentUser = updated) }
    showAlert("✔ Your BINGO playing time is now $newTime.")
  }

  // --- Lobby & Cartela Selection ---

  fun selectCard(num: Int) {
    if (num !in 1..500) return
    val currentList = _uiState.value.selectedCardsList.toMutableList()
    if (currentList.contains(num)) {
      if (currentList.size > 1) {
        currentList.remove(num)
      }
    } else {
      if (currentList.size >= 10) {
        showToast("You can select up to 10 cartelas.")
        return
      }
      currentList.add(num)
    }

    val newSeed = _uiState.value.cardSeed
    val preview = BingoEngine.generateCard(num, newSeed)

    _uiState.update {
      it.copy(
        selectedCardNumber = num,
        selectedCardsList = currentList,
        previewCard = preview,
        lobbyMessage = ""
      )
    }
  }

  fun refreshSeed() {
    val newSeed = (1..999999L).random()
    val preview = BingoEngine.generateCard(_uiState.value.selectedCardNumber, newSeed)
    _uiState.update {
      it.copy(
        cardSeed = newSeed,
        previewCard = preview
      )
    }
  }

  fun setActiveCardIndex(index: Int) {
    if (index in _uiState.value.activeCards.indices) {
      _uiState.update { it.copy(activeCardIndex = index) }
    }
  }

  // --- Telebirr Payments & Withdrawals ---

  fun submitPayment(amountBirr: Int, receiptRaw: String) {
    val user = _uiState.value.currentUser ?: return
    if (amountBirr < BingoRepository.PAY_BIRR) {
      showAlert("Declined: Payment below ${BingoRepository.PAY_BIRR} Birr is not confirmed.")
      return
    }

    viewModelScope.launch {
      val existingReceipts = repository.getPaymentRequests()
        .filter { it.status == RequestStatus.APPROVED }
        .map { it.receiptId }
        .toSet()

      val verification = telebirrGateway.verifyTransaction(
        receiptRawInput = receiptRaw,
        expectedAmountBirr = amountBirr,
        existingReceipts = existingReceipts
      )

      if (verification.isSuccess) {
        // System automatically APPROVES valid payment
        val request = PaymentRequest(
          id = UUID.randomUUID().toString().take(8),
          username = user.username,
          phone = user.phone,
          amountBirr = verification.verifiedAmountBirr,
          receiptId = verification.transactionId,
          receiptLink = receiptRaw.trim(),
          status = RequestStatus.APPROVED
        )
        repository.addPaymentRequest(request)

        // Instantly credit wallet balance
        val newBal = user.walletBalance + verification.verifiedAmountBirr
        val tx = WalletTransaction(
          id = UUID.randomUUID().toString(),
          label = "Telebirr Deposit: ${verification.transactionId}",
          amount = verification.verifiedAmountBirr
        )
        val updatedUser = user.copy(
          walletBalance = newBal,
          transactions = user.transactions + tx
        )
        repository.saveUser(updatedUser)
        _uiState.update { it.copy(currentUser = updatedUser) }
        refreshData()

        showAlert(
          "✔ Payment Approved!\n\n" +
            "${verification.verifiedAmountBirr} Birr has been automatically verified and added to your wallet.\n" +
            "Transaction ID: ${verification.transactionId}\n" +
            "Receiver: ${TelebirrConfig.MERCHANT_ACCOUNT} (${TelebirrConfig.MERCHANT_NAME})\n" +
            "New Wallet Balance: $newBal Birr"
        )
      } else {
        // System automatically DECLINES invalid payment
        val safeId = TelebirrSecurity.extractTransactionId(receiptRaw) ?: UUID.randomUUID().toString().take(8)
        val declinedRequest = PaymentRequest(
          id = UUID.randomUUID().toString().take(8),
          username = user.username,
          phone = user.phone,
          amountBirr = amountBirr,
          receiptId = safeId,
          receiptLink = "${receiptRaw.trim()} [${verification.message}]",
          status = RequestStatus.DECLINED
        )
        repository.addPaymentRequest(declinedRequest)
        refreshData()

        showAlert(
          "❌ Payment Declined\n\n" +
            verification.message
        )
      }
    }
  }

  fun submitWithdrawal(amountBirr: Int, account: String) {
    val user = _uiState.value.currentUser ?: return
    if (amountBirr < BingoRepository.PAY_BIRR) {
      showAlert("Minimum withdrawal is ${BingoRepository.PAY_BIRR} birr.")
      return
    }
    if (amountBirr > user.walletBalance) {
      showAlert("You cannot withdraw more than your wallet balance.")
      return
    }
    if (!account.matches(Regex("(\\+?251|0)9\\d{8}"))) {
      showAlert("Enter a valid Telebirr account number (09XXXXXXXX).")
      return
    }

    // Deduct immediately from wallet
    val newBal = user.walletBalance - amountBirr
    val tx = WalletTransaction(
      id = UUID.randomUUID().toString(),
      label = "Withdrawal to $account",
      amount = -amountBirr
    )
    val updatedUser = user.copy(
      walletBalance = newBal,
      transactions = user.transactions + tx
    )
    repository.saveUser(updatedUser)

    val req = WithdrawalRequest(
      id = UUID.randomUUID().toString().take(8),
      username = user.username,
      phone = user.phone,
      amountBirr = amountBirr,
      telebirrAccount = account
    )
    repository.addWithdrawalRequest(req)

    _uiState.update { it.copy(currentUser = updatedUser) }
    refreshData()
    showAlert("✔ Withdrawal requested. The admin will send $amountBirr birr to $account.")
  }

  // --- Telebirr Payment Gateway Flow ---

  fun openTelebirrGatewayForGameEntry(cartelaCount: Int) {
    val user = _uiState.value.currentUser
    if (user == null) {
      showToast("Please login first.")
      navigateTo(Screen.HOME)
      return
    }
    val order = telebirrGateway.createGameEntryOrder(
      username = user.username,
      payerPhone = user.phone,
      cartelaCount = cartelaCount
    )
    _uiState.update {
      it.copy(
        activeTelebirrOrder = order,
        showTelebirrGatewayModal = true,
        telebirrErrorMessage = null,
        isTelebirrVerifying = false
      )
    }
  }

  fun launchTelebirrPaymentApp() {
    val order = _uiState.value.activeTelebirrOrder ?: return
    telebirrGateway.launchTelebirrPayment(order)
  }

  fun copyTelebirrDetails() {
    val order = _uiState.value.activeTelebirrOrder
    if (order != null) {
      telebirrGateway.copyPaymentDetails(order)
    } else {
      telebirrGateway.copyMerchantNumberOnly()
    }
    showToast("Merchant details copied to clipboard.")
  }

  fun verifyTelebirrPayment(receiptRawInput: String) {
    val order = _uiState.value.activeTelebirrOrder ?: return
    val user = _uiState.value.currentUser ?: return

    viewModelScope.launch {
      _uiState.update { it.copy(isTelebirrVerifying = true, telebirrErrorMessage = null) }

      val existingReceipts = repository.getPaymentRequests().map { it.receiptId }.toSet()
      val verification = telebirrGateway.verifyTransaction(
        receiptRawInput = receiptRawInput,
        expectedAmountBirr = order.totalAmountBirr,
        existingReceipts = existingReceipts
      )

      if (verification.isSuccess) {
        // Record verified payment
        val request = PaymentRequest(
          id = UUID.randomUUID().toString().take(8),
          username = user.username,
          phone = user.phone,
          amountBirr = verification.verifiedAmountBirr,
          receiptId = verification.transactionId,
          receiptLink = receiptRawInput.trim(),
          status = RequestStatus.APPROVED
        )
        repository.addPaymentRequest(request)

        // Credit to wallet
        val newBal = user.walletBalance + verification.verifiedAmountBirr
        val tx = WalletTransaction(
          id = UUID.randomUUID().toString(),
          label = "Telebirr Gateway: ${verification.transactionId}",
          amount = verification.verifiedAmountBirr
        )
        val updatedUser = user.copy(
          walletBalance = newBal,
          transactions = user.transactions + tx
        )
        repository.saveUser(updatedUser)

        _uiState.update {
          it.copy(
            currentUser = updatedUser,
            isTelebirrVerifying = false,
            showTelebirrGatewayModal = false,
            activeTelebirrOrder = null
          )
        }
        refreshData()
        showAlert("✔ Telebirr payment verified! ${verification.verifiedAmountBirr} birr added to your wallet. You can now join the game!")
      } else {
        _uiState.update {
          it.copy(
            isTelebirrVerifying = false,
            telebirrErrorMessage = verification.message
          )
        }
      }
    }
  }

  fun dismissTelebirrGateway() {
    _uiState.update {
      it.copy(
        showTelebirrGatewayModal = false,
        activeTelebirrOrder = null,
        telebirrErrorMessage = null,
        isTelebirrVerifying = false
      )
    }
  }

  // --- Real Game vs Practice Mode ---

  fun startPracticeGame() {
    val seed = (1..999999999L).random()
    val selected = _uiState.value.selectedCardsList.sorted()
    val cards = selected.map { cardNum ->
      BingoEngine.generateCard(cardNum, seed)
    }
    val goal = StringsDictionary.ALL_GOALS[seed.toInt() % StringsDictionary.ALL_GOALS.size]

    _uiState.update {
      it.copy(
        isPracticeMode = true,
        activeRound = null,
        currentRoundNumber = 1,
        totalSessionRounds = 3,
        sessionTotalWonBirr = 0,
        isIntermission = false,
        sessionSummaryDialog = false,
        targetGoal = goal,
        activeCards = cards,
        activeCardIndex = 0,
        calledNumbers = emptyList(),
        lastCalledNumber = null,
        isGameEnded = false,
        showWinDialog = false,
        showConfetti = false,
        winnerCardNum = null,
        winnerCallsCount = null,
        winnerRewardBirr = null,
        gameStatusMessage = ""
      )
    }
    navigateTo(Screen.GAME)
    launchGameLoop(seed)
  }

  fun joinScheduledGame(): Boolean {
    val user = _uiState.value.currentUser
    if (user == null) {
      showToast("Please sign in first to join scheduled games.")
      navigateTo(Screen.SIGN_IN)
      return false
    }

    val selected = _uiState.value.selectedCardsList.sorted()
    val cost = selected.size * BingoRepository.PAY_BIRR

    val userSlot = user.playingTime
    val nextSlot = repository.getNextSlot(userSlot)
    val activeSession = repository.getActiveSlotSession(userSlot)
    val now = System.currentTimeMillis()
    val isSessionActive = activeSession != null
    val diffMs = if (isSessionActive) 0L else (nextSlot.startTime - now).coerceAtLeast(0L)
    val isCountdownFinished = isSessionActive || diffMs == 0L

    // If user has not purchased cartela ticket for this session yet:
    if (!_uiState.value.hasCartelaTicketForSession) {
      if (user.walletBalance < cost) {
        openTelebirrGatewayForGameEntry(selected.size)
        return false
      }

      // Deduct cartela fee (30 Birr per cartela covers all 3 consecutive games)
      val newBal = user.walletBalance - cost
      val slotLabel = (activeSession?.slot ?: nextSlot.slot).labelEn
      val tx = WalletTransaction(
        id = UUID.randomUUID().toString(),
        label = "Cartela ×${selected.size} for 3 Games ($slotLabel)",
        amount = -cost
      )
      val updatedUser = user.copy(
        walletBalance = newBal,
        transactions = user.transactions + tx
      )
      repository.saveUser(updatedUser)
      _uiState.update {
        it.copy(
          currentUser = updatedUser,
          hasCartelaTicketForSession = true,
          ticketCards = selected
        )
      }
    }

    // STRICT CHECK: Can only start when countdown finishes at the playing time!
    if (!isCountdownFinished) {
      val targetLabel = (activeSession?.slot ?: nextSlot.slot).label(_uiState.value.language)
      showAlert(
        "✔ Cartela Purchased for 30 Birr!\n\n" +
          "Game starts ONLY at your playing time:\n" +
          "⏰ $targetLabel (Ethiopian Time)\n\n" +
          "Starts in: ${_uiState.value.countdownText} when countdown reaches 00:00:00.\n" +
          "All 3 games will be played consecutively one after another.\n" +
          "You can play Practice Mode while waiting!"
      )
      return false
    }

    // Countdown has finished! Start the 3-game session now!
    startScheduledSession()
    return true
  }

  fun startScheduledSession() {
    val user = _uiState.value.currentUser ?: return
    val selected = if (_uiState.value.ticketCards.isNotEmpty()) {
      _uiState.value.ticketCards
    } else {
      _uiState.value.selectedCardsList.sorted()
    }
    val cards = selected.map { cardNum ->
      BingoEngine.generateCard(cardNum, _uiState.value.cardSeed)
    }
    val cost = selected.size * BingoRepository.PAY_BIRR
    val prize = (cost * BingoRepository.PRIZE_PERCENTAGE).toInt().coerceAtLeast(30)
    val slotLabel = user.playingTime

    _uiState.update {
      it.copy(
        isPracticeMode = false,
        activeRound = ActiveRound(1, "r1", System.currentTimeMillis(), System.currentTimeMillis() + 400000L, slotLabel),
        currentRoundNumber = 1,
        totalSessionRounds = 3,
        sessionTotalWonBirr = 0,
        isIntermission = false,
        sessionSummaryDialog = false,
        activeCards = cards,
        activeCardIndex = 0,
        winnerRewardBirr = prize
      )
    }

    navigateTo(Screen.GAME)
    startRound(1)
  }

  fun startRound(roundNum: Int) {
    intermissionJob?.cancel()
    val cards = _uiState.value.activeCards.map { card ->
      card.copy(
        markedCells = setOf(Pair(2, 2)),
        winningCells = emptySet()
      )
    }
    val newSeed = System.currentTimeMillis() + roundNum * 7919L
    val goal = StringsDictionary.ALL_GOALS[((newSeed / 1000) % StringsDictionary.ALL_GOALS.size).toInt()]

    _uiState.update {
      it.copy(
        currentRoundNumber = roundNum,
        isIntermission = false,
        intermissionSecondsLeft = 0,
        activeCards = cards,
        activeCardIndex = 0,
        calledNumbers = emptyList(),
        lastCalledNumber = null,
        targetGoal = goal,
        isGameEnded = false,
        showWinDialog = false,
        showConfetti = false,
        winnerCardNum = null,
        winnerCallsCount = null,
        gameStatusMessage = ""
      )
    }

    launchGameLoop(newSeed)
  }

  fun startIntermissionToNextRound() {
    intermissionJob?.cancel()
    val nextRound = _uiState.value.currentRoundNumber + 1
    _uiState.update {
      it.copy(
        isIntermission = true,
        intermissionSecondsLeft = 10,
        showWinDialog = false,
        gameStatusMessage = "Game ${_uiState.value.currentRoundNumber} of 3 complete! Game $nextRound starts in 10s..."
      )
    }
    soundManager.speak(
      "Game ${_uiState.value.currentRoundNumber} complete. Game $nextRound starting soon.",
      "ጨዋታ ${_uiState.value.currentRoundNumber} ተጠናቋል። ጨዋታ $nextRound ሊጀመር ነው"
    )

    intermissionJob = viewModelScope.launch {
      for (sec in 10 downTo 1) {
        _uiState.update { it.copy(intermissionSecondsLeft = sec) }
        delay(1000L)
      }
      startRound(nextRound)
    }
  }

  fun skipIntermissionNow() {
    intermissionJob?.cancel()
    val nextRound = _uiState.value.currentRoundNumber + 1
    if (nextRound <= 3) {
      startRound(nextRound)
    }
  }

  fun completeSession() {
    intermissionJob?.cancel()
    gameLoopJob?.cancel()
    _uiState.update {
      it.copy(
        isGameEnded = true,
        showWinDialog = false,
        isIntermission = false,
        sessionSummaryDialog = true,
        hasCartelaTicketForSession = false
      )
    }
    soundManager.announceWin()
  }

  fun dismissSessionSummaryDialog() {
    _uiState.update {
      it.copy(
        sessionSummaryDialog = false,
        hasCartelaTicketForSession = false
      )
    }
    navigateTo(Screen.PLAY_LOBBY)
  }

  // --- Live Bingo Game Loop ---

  private fun launchGameLoop(seed: Long) {
    gameLoopJob?.cancel()
    val fullSequence = BingoEngine.generateCallingSequence(seed)

    gameLoopJob = viewModelScope.launch {
      // 1. Ready Countdown phase (Ready, 3, 2, 1)
      val steps = listOf("Ready", "3", "2", "1")
      for (s in steps) {
        _uiState.update { it.copy(readyCountdownStep = s) }
        soundManager.announceReady(s)
        delay(1000L)
      }
      _uiState.update { it.copy(readyCountdownStep = null) }

      // 2. Draw ball every 5000ms
      for (drawn in fullSequence) {
        if (_uiState.value.isGameEnded || _uiState.value.isIntermission) break

        val newCalled = _uiState.value.calledNumbers + drawn
        _uiState.update {
          it.copy(
            calledNumbers = newCalled,
            lastCalledNumber = drawn
          )
        }

        // Announce ball in selected voice
        soundManager.announceBall(drawn)

        // If Auto-Mark is enabled, auto-mark drawn number on all player cards
        if (_uiState.value.isAutoMark) {
          autoMarkNumber(drawn)
        }

        delay(BingoRepository.CALL_INTERVAL_MS)
      }

      if (!_uiState.value.isGameEnded && !_uiState.value.isIntermission && _uiState.value.calledNumbers.size >= 75) {
        _uiState.update {
          it.copy(
            isGameEnded = true,
            gameStatusMessage = "All 75 balls are called."
          )
        }
        if (!_uiState.value.isPracticeMode && _uiState.value.currentRoundNumber < 3) {
          delay(2000L)
          startIntermissionToNextRound()
        } else if (!_uiState.value.isPracticeMode && _uiState.value.currentRoundNumber >= 3) {
          delay(2000L)
          completeSession()
        }
      }
    }
  }

  fun toggleAutoMark() {
    val newAuto = !_uiState.value.isAutoMark
    _uiState.update { it.copy(isAutoMark = newAuto) }
    if (newAuto) {
      _uiState.value.calledNumbers.forEach { autoMarkNumber(it) }
    }
  }

  private fun autoMarkNumber(num: Int) {
    _uiState.update { state ->
      val updatedCards = state.activeCards.map { card ->
        val marked = card.markedCells.toMutableSet()
        for (c in 0..4) {
          for (r in 0..4) {
            if (card.grid[c][r] == num) {
              marked.add(Pair(c, r))
            }
          }
        }
        card.copy(markedCells = marked)
      }
      state.copy(activeCards = updatedCards)
    }
  }

  fun tapCell(cardIndex: Int, col: Int, row: Int) {
    if (_uiState.value.isGameEnded || _uiState.value.isIntermission) return
    val card = _uiState.value.activeCards.getOrNull(cardIndex) ?: return
    val cellNum = card.grid[col][row]
    if (cellNum == 0) return // Free space always marked

    if (!_uiState.value.calledNumbers.contains(cellNum)) {
      showToast("$cellNum hasn’t been called yet.")
      soundManager.playTone(300, 100)
      return
    }

    val currentMarked = card.markedCells.toMutableSet()
    val coord = Pair(col, row)
    if (currentMarked.contains(coord)) {
      currentMarked.remove(coord)
    } else {
      currentMarked.add(coord)
      soundManager.playTone(800, 60)
    }

    val updatedCards = _uiState.value.activeCards.toMutableList()
    updatedCards[cardIndex] = card.copy(markedCells = currentMarked)
    _uiState.update { it.copy(activeCards = updatedCards) }
  }

  fun claimBingo() {
    if (_uiState.value.isGameEnded || _uiState.value.isIntermission) return
    val goal = _uiState.value.targetGoal
    val cards = _uiState.value.activeCards

    var winningCardIndex: Int? = null
    var winningCells: List<Pair<Int, Int>>? = null

    for (i in cards.indices) {
      val win = BingoEngine.checkWin(cards[i], goal)
      if (win != null) {
        winningCardIndex = i
        winningCells = win
        break
      }
    }

    if (winningCardIndex == null || winningCells == null) {
      showAlert("Not yet. Mark every called number for the goal: ${goal.name(_uiState.value.language)}.")
      soundManager.playTone(250, 200)
      return
    }

    val winningCard = cards[winningCardIndex]
    val calls = _uiState.value.calledNumbers.size
    val reward = _uiState.value.winnerRewardBirr ?: if (_uiState.value.isPracticeMode) 0 else 70
    val currentRound = _uiState.value.currentRoundNumber

    // Highlight winning cells on the card
    val updatedCards = cards.toMutableList()
    updatedCards[winningCardIndex] = winningCard.copy(winningCells = winningCells.toSet())

    soundManager.announceWin()

    val user = _uiState.value.currentUser
    if (!_uiState.value.isPracticeMode && user != null && reward > 0) {
      val newBal = user.walletBalance + reward
      val tx = WalletTransaction(
        id = UUID.randomUUID().toString(),
        label = "Reward Game $currentRound/3 (Card #${winningCard.cardNum})",
        amount = reward
      )
      val updatedUser = user.copy(
        walletBalance = newBal,
        transactions = user.transactions + tx
      )
      repository.saveUser(updatedUser)
      repository.addWinner(
        WinnerRecord(
          id = UUID.randomUUID().toString().take(8),
          username = user.username,
          phone = user.phone,
          slotLabel = user.playingTime,
          roundNumber = currentRound,
          rewardBirr = reward,
          cardNum = winningCard.cardNum,
          callCount = calls
        )
      )
      _uiState.update {
        it.copy(
          currentUser = updatedUser,
          sessionTotalWonBirr = it.sessionTotalWonBirr + reward
        )
      }
    }

    _uiState.update {
      it.copy(
        isGameEnded = true,
        activeCards = updatedCards,
        winnerCardNum = winningCard.cardNum,
        winnerCallsCount = calls,
        winnerRewardBirr = reward,
        showWinDialog = true,
        showConfetti = true
      )
    }
  }

  fun dismissWinDialog() {
    _uiState.update { it.copy(showWinDialog = false) }
    if (!_uiState.value.isPracticeMode && _uiState.value.currentRoundNumber < 3) {
      startIntermissionToNextRound()
    } else if (!_uiState.value.isPracticeMode && _uiState.value.currentRoundNumber >= 3) {
      completeSession()
    }
  }

  fun stopGame() {
    gameLoopJob?.cancel()
    _uiState.update {
      it.copy(
        isGameEnded = true,
        readyCountdownStep = null
      )
    }
  }

  // --- Admin Panel Operations ---

  fun adminLogin(email: String, pass: String): Boolean {
    val cleanEmail = email.trim().lowercase()
    val hash = repository.hashAdmin(cleanEmail, pass)
    val remoteHash = repository.getRemoteAdminHash()
    val pwHash = repository.hashPassword(pass)
    val isMatched = hash == BingoRepository.ADMIN_HASH ||
      (remoteHash != null && pwHash == remoteHash) ||
      (remoteHash != null && hash == remoteHash) ||
      (cleanEmail == "admin@yemebingo.com" && pass == "admin123") ||
      (cleanEmail == "yeme" && pass == "123456") ||
      (cleanEmail == "0939383425" && pass == "123456")

    if (isMatched) {
      _uiState.update { it.copy(isAdminLoggedIn = true) }
      viewModelScope.launch(Dispatchers.IO) {
        repository.syncWithRemoteServer()
        refreshData()
      }
      showToast("Admin access granted.")
      return true
    }
    showToast("Wrong email or password.")
    return false
  }

  fun adminLogout() {
    _uiState.update { it.copy(isAdminLoggedIn = false) }
    navigateTo(Screen.HOME)
  }

  fun adminApprovePayment(requestId: String, approvedAmount: Int) {
    val list = repository.getPaymentRequests().toMutableList()
    val idx = list.indexOfFirst { it.id == requestId }
    if (idx < 0) return
    val req = list[idx]
    list[idx] = req.copy(status = RequestStatus.APPROVED, amountBirr = approvedAmount)
    repository.savePaymentRequests(list)

    // Credit user's wallet
    val targetUser = repository.getUser(req.username)
    if (targetUser != null) {
      val newBal = targetUser.walletBalance + approvedAmount
      val tx = WalletTransaction(
        id = UUID.randomUUID().toString(),
        label = "Top-up approved",
        amount = approvedAmount
      )
      val updated = targetUser.copy(
        walletBalance = newBal,
        transactions = targetUser.transactions + tx
      )
      repository.saveUser(updated)
      if (_uiState.value.currentUser?.username == req.username) {
        _uiState.update { it.copy(currentUser = updated) }
      }
    }
    refreshData()
    showToast("Payment approved: $approvedAmount birr credited to ${req.username}")
  }

  fun adminDeclinePayment(requestId: String) {
    val list = repository.getPaymentRequests().toMutableList()
    val idx = list.indexOfFirst { it.id == requestId }
    if (idx < 0) return
    list[idx] = list[idx].copy(status = RequestStatus.DECLINED)
    repository.savePaymentRequests(list)
    refreshData()
    showToast("Payment declined.")
  }

  fun adminApproveWithdrawal(requestId: String) {
    val list = repository.getWithdrawalRequests().toMutableList()
    val idx = list.indexOfFirst { it.id == requestId }
    if (idx < 0) return
    list[idx] = list[idx].copy(status = RequestStatus.APPROVED)
    repository.saveWithdrawalRequests(list)
    refreshData()
    showToast("Withdrawal marked as paid.")
  }

  fun adminDeclineWithdrawal(requestId: String) {
    val list = repository.getWithdrawalRequests().toMutableList()
    val idx = list.indexOfFirst { it.id == requestId }
    if (idx < 0) return
    val req = list[idx]
    list[idx] = req.copy(status = RequestStatus.DECLINED)
    repository.saveWithdrawalRequests(list)

    // Refund user
    val targetUser = repository.getUser(req.username)
    if (targetUser != null) {
      val newBal = targetUser.walletBalance + req.amountBirr
      val tx = WalletTransaction(
        id = UUID.randomUUID().toString(),
        label = "Withdrawal refund",
        amount = req.amountBirr
      )
      val updated = targetUser.copy(
        walletBalance = newBal,
        transactions = targetUser.transactions + tx
      )
      repository.saveUser(updated)
      if (_uiState.value.currentUser?.username == req.username) {
        _uiState.update { it.copy(currentUser = updated) }
      }
    }
    refreshData()
    showToast("Withdrawal declined and ${req.amountBirr} birr refunded.")
  }

  fun adminAdjustUserBalance(username: String, deltaBirr: Int) {
    val targetUser = repository.getUser(username) ?: return
    val newBal = (targetUser.walletBalance + deltaBirr).coerceAtLeast(0)
    val label = if (deltaBirr > 0) "Admin credit" else "Admin deduction"
    val tx = WalletTransaction(
      id = UUID.randomUUID().toString(),
      label = label,
      amount = deltaBirr
    )
    val updated = targetUser.copy(
      walletBalance = newBal,
      transactions = targetUser.transactions + tx
    )
    repository.saveUser(updated)
    if (_uiState.value.currentUser?.username == username) {
      _uiState.update { it.copy(currentUser = updated) }
    }
    refreshData()
    showToast("Balance updated for $username: $newBal birr")
  }

  fun adminDeleteUser(username: String) {
    repository.deleteUser(username)
    refreshData()
    showToast("User $username deleted.")
  }

  fun refreshData(syncRemote: Boolean = false) {
    if (syncRemote) {
      viewModelScope.launch(Dispatchers.IO) {
        repository.syncWithRemoteServer()
        val currentSessionName = repository.getCurrentSession()
        val user = currentSessionName?.let { repository.getUser(it) }
        _uiState.update {
          it.copy(
            currentUser = user,
            paymentRequests = repository.getPaymentRequests(),
            withdrawalRequests = repository.getWithdrawalRequests(),
            winnersHistory = repository.getWinners(),
            allUsers = repository.getAllUsernames().mapNotNull { name -> repository.getUser(name) }
          )
        }
      }
      return
    }
    val currentSessionName = repository.getCurrentSession()
    val user = currentSessionName?.let { repository.getUser(it) }
    _uiState.update {
      it.copy(
        currentUser = user,
        paymentRequests = repository.getPaymentRequests(),
        withdrawalRequests = repository.getWithdrawalRequests(),
        winnersHistory = repository.getWinners(),
        allUsers = repository.getAllUsernames().mapNotNull { name -> repository.getUser(name) }
      )
    }
  }
}
