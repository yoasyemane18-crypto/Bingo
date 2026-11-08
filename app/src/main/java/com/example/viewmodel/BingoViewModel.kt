package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BingoRepository
import com.example.engine.BingoEngine
import com.example.model.ActivityRecord
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
import kotlinx.coroutines.withContext
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
  val occupiedCartelas: Map<Int, String> = emptyMap(),
  val previewCard: BingoCard = BingoEngine.generateCard(1, 1L),
  val lobbyMessage: String = "",

  // In-Game Arena State
  val currentSessionId: String? = null,
  val activePlayerCount: Int = 1,
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
  val activitiesHistory: List<ActivityRecord> = emptyList(),

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
  private var hasAutoLaunchedForActiveSlot: String? = null

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
      refreshData(syncRemote = true)
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
    if (screen == Screen.PLAY_LOBBY) {
      val userSlot = _uiState.value.currentUser?.playingTime
      val targetSession = repository.getActiveSlotSession(userSlot) ?: repository.getNextSlot(userSlot)
      val sessionId = targetSession.sessionId
      val localOccupied = repository.getOccupiedCartelas(sessionId)
      _uiState.update { it.copy(occupiedCartelas = localOccupied) }
      viewModelScope.launch(Dispatchers.IO) {
        val remoteOccupied = repository.remoteService.fetchSessionOccupiedCartelas(sessionId)
        val merged = localOccupied + remoteOccupied
        if (merged.isNotEmpty()) {
          repository.saveOccupiedCartelas(sessionId, merged)
          _uiState.update { it.copy(occupiedCartelas = merged) }
        }
      }
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
    var tickerTick = 0
    // Trigger immediate time calibration on ticker launch
    repository.syncServerTime()

    countdownJob = viewModelScope.launch {
      while (true) {
        val userSlot = _uiState.value.currentUser?.playingTime
        val nextSlot = repository.getNextSlot(userSlot)
        val activeSession = repository.getActiveSlotSession(userSlot)
        // Use synchronized authoritative server time so all devices have identical countdown
        val now = repository.getSynchronizedTimeMs()

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

        // Periodically refresh occupied cartelas for this playing time session
        val targetSession = activeSession ?: nextSlot
        val sessionId = targetSession.sessionId
        if (tickerTick++ % 3 == 0) {
          viewModelScope.launch(Dispatchers.IO) {
            val remoteOccupied = repository.remoteService.fetchSessionOccupiedCartelas(sessionId)
            val localOccupied = repository.getOccupiedCartelas(sessionId)
            val merged = localOccupied + remoteOccupied
            if (merged.isNotEmpty()) {
              repository.saveOccupiedCartelas(sessionId, merged)
              _uiState.update { it.copy(occupiedCartelas = merged) }
            }
          }
        }

        // Periodically resync authoritative server time with Firebase / Google atomic clock
        if (tickerTick % 10 == 0) {
          repository.syncServerTime()
        }

        if (!isFinished) {
          hasAutoLaunchedForActiveSlot = null
        }

        // AUTOMATIC GAME START:
        // When the countdown finishes (00:00:00), the game starts automatically!
        // No manual clicking needed! All players on different devices start together!
        if (isFinished && _uiState.value.currentScreen != Screen.GAME && _uiState.value.currentUser != null) {
          if (hasAutoLaunchedForActiveSlot != sessionId) {
            hasAutoLaunchedForActiveSlot = sessionId
            startScheduledSession()
          }
        }

        // Sub-second phase alignment: sleep until the exact millisecond of the next synchronized second boundary
        val currentNow = repository.getSynchronizedTimeMs()
        val msIntoSecond = (currentNow % 1000L)
        val sleepMs = (1000L - msIntoSecond).coerceIn(100L, 1000L)
        delay(sleepMs)
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
    viewModelScope.launch(Dispatchers.IO) {
      repository.remoteService.saveUser(newUser)
      repository.remoteService.logActivity(
        type = "USER_REGISTERED",
        username = newUser.username,
        description = "New user registered: ${newUser.username} (${newUser.phone}, ${newUser.playingTime})"
      )
    }
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
    val user = _uiState.value.currentUser
    val occupied = _uiState.value.occupiedCartelas
    val owner = occupied[num]
    if (owner != null && owner != user?.username) {
      notifyOccupiedCard(num)
      return
    }

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

  fun notifyOccupiedCard(cardNum: Int) {
    val occupiedMsg = when (_uiState.value.language) {
      AppLanguage.EN -> "Occupied"
      AppLanguage.AM -> "ተይዟል"
      AppLanguage.TI -> "ተታሒዙ"
    }
    showToast(occupiedMsg)
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

  // --- Payments & Withdrawals (Telebirr, CBE, CBE Birr, E-Birr) ---

  fun submitPayment(amountBirr: Int, receiptRaw: String, paymentMethod: String = "telebirr") {
    val user = _uiState.value.currentUser ?: return
    if (amountBirr < BingoRepository.PAY_BIRR) {
      showAlert("Declined: Payment below ${BingoRepository.PAY_BIRR} Birr is not confirmed.")
      return
    }

    val methodObj = com.example.payment.PaymentMethod.fromCode(paymentMethod)

    viewModelScope.launch {
      val existingReceipts = repository.getPaymentRequests()
        .filter { it.status == RequestStatus.APPROVED }
        .map { it.receiptId }
        .toSet()

      val verification = telebirrGateway.verifyTransaction(
        receiptRawInput = receiptRaw,
        expectedAmountBirr = amountBirr,
        existingReceipts = existingReceipts,
        paymentMethod = methodObj.code
      )

      if (verification.isSuccess) {
        // System itself automatically APPROVES valid payment for Telebirr, CBE, CBE Birr, or E-Birr
        val request = PaymentRequest(
          id = UUID.randomUUID().toString().take(8),
          username = user.username,
          phone = user.phone,
          amountBirr = verification.verifiedAmountBirr,
          receiptId = verification.transactionId,
          receiptLink = receiptRaw.trim(),
          paymentMethod = methodObj.code,
          status = RequestStatus.APPROVED
        )
        repository.addPaymentRequest(request)

        // Instantly credit wallet balance
        val newBal = user.walletBalance + verification.verifiedAmountBirr
        val tx = WalletTransaction(
          id = UUID.randomUUID().toString(),
          label = "${methodObj.displayName} Deposit: ${verification.transactionId}",
          amount = verification.verifiedAmountBirr
        )
        val updatedUser = user.copy(
          walletBalance = newBal,
          transactions = user.transactions + tx
        )
        repository.saveUserLocalOnly(updatedUser)
        // Push immediately to Firebase RTDB
        viewModelScope.launch(Dispatchers.IO) {
          repository.remoteService.savePaymentRequest(request)
          repository.remoteService.saveUser(updatedUser)
          repository.remoteService.logActivity(
            type = "DEPOSIT_APPROVED",
            username = user.username,
            amountBirr = verification.verifiedAmountBirr,
            description = "Deposit auto-approved (${methodObj.displayName}): ${verification.verifiedAmountBirr} Birr by ${user.username} (Tx: ${verification.transactionId})",
            extra = methodObj.displayName
          )
        }

        _uiState.update { it.copy(currentUser = updatedUser) }
        refreshData()

        showAlert(
          "✔ ${methodObj.displayName} Payment Approved!\n\n" +
            "${verification.verifiedAmountBirr} Birr has been automatically verified and added to your wallet.\n" +
            "Transaction ID: ${verification.transactionId}\n" +
            "Receiver: ${methodObj.receiverAccount} (${methodObj.receiverName})\n" +
            "New Wallet Balance: $newBal Birr"
        )
      } else {
        // System itself automatically DECLINES invalid payment
        val safeId = TelebirrSecurity.extractTransactionId(receiptRaw) ?: UUID.randomUUID().toString().take(8)
        val declinedRequest = PaymentRequest(
          id = UUID.randomUUID().toString().take(8),
          username = user.username,
          phone = user.phone,
          amountBirr = amountBirr,
          receiptId = safeId,
          receiptLink = "${receiptRaw.trim()} [${verification.message}]",
          paymentMethod = methodObj.code,
          status = RequestStatus.DECLINED
        )
        repository.addPaymentRequest(declinedRequest)
        // Push declined payment to Firebase RTDB
        viewModelScope.launch(Dispatchers.IO) {
          repository.remoteService.savePaymentRequest(declinedRequest)
          repository.remoteService.logActivity(
            type = "DEPOSIT_DECLINED",
            username = user.username,
            amountBirr = amountBirr,
            description = "${methodObj.displayName} deposit auto-declined: $amountBirr Birr for ${user.username} (${verification.message})",
            extra = methodObj.displayName
          )
        }
        refreshData()

        showAlert(
          "❌ ${methodObj.displayName} Payment Declined\n\n" +
            verification.message
        )
      }
    }
  }

  fun submitWithdrawal(amountBirr: Int, account: String, paymentMethod: String = "telebirr") {
    val user = _uiState.value.currentUser ?: return
    if (amountBirr < BingoRepository.PAY_BIRR) {
      showAlert("Minimum withdrawal is ${BingoRepository.PAY_BIRR} birr.")
      return
    }
    if (amountBirr > user.walletBalance) {
      showAlert("You cannot withdraw more than your wallet balance.")
      return
    }
    if (account.trim().length < 8) {
      showAlert("Enter a valid account or phone number.")
      return
    }

    val methodObj = com.example.payment.PaymentMethod.fromCode(paymentMethod)

    viewModelScope.launch(Dispatchers.IO) {
      // Deduct immediately from wallet
      val newBal = user.walletBalance - amountBirr
      val tx = WalletTransaction(
        id = UUID.randomUUID().toString(),
        label = "Withdrawal via ${methodObj.displayName} to $account: -$amountBirr Birr",
        amount = -amountBirr,
        timestamp = System.currentTimeMillis()
      )
      val updatedUser = user.copy(
        walletBalance = newBal,
        transactions = user.transactions + tx
      )
      repository.saveUserLocalOnly(updatedUser)
      // Send directly to Firebase RTDB immediately
      repository.remoteService.saveUser(updatedUser)

      val req = WithdrawalRequest(
        id = UUID.randomUUID().toString().take(8),
        username = user.username,
        phone = user.phone,
        amountBirr = amountBirr,
        telebirrAccount = account,
        paymentMethod = methodObj.code,
        timestamp = System.currentTimeMillis()
      )
      repository.addWithdrawalRequest(req)
      // Send directly to Firebase RTDB immediately
      repository.remoteService.saveWithdrawalRequest(req)
      repository.remoteService.logActivity(
        type = "WITHDRAWAL_REQUEST",
        username = user.username,
        amountBirr = amountBirr,
        description = "Withdrawal requested via ${methodObj.displayName}: $amountBirr Birr to $account by ${user.username}",
        extra = "${methodObj.displayName}: $account"
      )

      withContext(Dispatchers.Main) {
        _uiState.update { it.copy(currentUser = updatedUser) }
        refreshData(syncRemote = true)
        showAlert("✔ Withdrawal requested. The admin will transfer $amountBirr birr to your ${methodObj.displayName} account: $account.")
      }
    }
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

  fun launchPaymentApp(method: com.example.payment.PaymentMethod = com.example.payment.PaymentMethod.TELEBIRR) {
    val order = _uiState.value.activeTelebirrOrder ?: return
    telebirrGateway.launchPaymentAppOrUssd(method, order)
  }

  fun launchTelebirrPaymentApp() {
    launchPaymentApp(com.example.payment.PaymentMethod.TELEBIRR)
  }

  fun copyPaymentDetails(method: com.example.payment.PaymentMethod = com.example.payment.PaymentMethod.TELEBIRR) {
    val order = _uiState.value.activeTelebirrOrder
    telebirrGateway.copyPaymentDetails(method, order)
    showToast("${method.displayName} details copied to clipboard.")
  }

  fun copyTelebirrDetails() {
    copyPaymentDetails(com.example.payment.PaymentMethod.TELEBIRR)
  }

  fun verifyPaymentGateway(receiptRawInput: String, paymentMethod: String = "telebirr") {
    val order = _uiState.value.activeTelebirrOrder ?: return
    val user = _uiState.value.currentUser ?: return
    val methodObj = com.example.payment.PaymentMethod.fromCode(paymentMethod)

    viewModelScope.launch {
      _uiState.update { it.copy(isTelebirrVerifying = true, telebirrErrorMessage = null) }

      val existingReceipts = repository.getPaymentRequests().map { it.receiptId }.toSet()
      val verification = telebirrGateway.verifyTransaction(
        receiptRawInput = receiptRawInput,
        expectedAmountBirr = order.totalAmountBirr,
        existingReceipts = existingReceipts,
        paymentMethod = paymentMethod
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
          paymentMethod = paymentMethod,
          status = RequestStatus.APPROVED
        )
        repository.addPaymentRequest(request)

        // Credit to wallet
        val newBal = user.walletBalance + verification.verifiedAmountBirr
        val tx = WalletTransaction(
          id = UUID.randomUUID().toString(),
          label = "${methodObj.displayName} Gateway: ${verification.transactionId}",
          amount = verification.verifiedAmountBirr
        )
        val updatedUser = user.copy(
          walletBalance = newBal,
          transactions = user.transactions + tx
        )
        repository.saveUserLocalOnly(updatedUser)
        // Push immediately to Firebase RTDB
        viewModelScope.launch(Dispatchers.IO) {
          repository.remoteService.savePaymentRequest(request)
          repository.remoteService.saveUser(updatedUser)
          repository.remoteService.logActivity(
            type = "DEPOSIT_APPROVED",
            username = user.username,
            amountBirr = verification.verifiedAmountBirr,
            description = "${methodObj.displayName} Gateway: ${verification.verifiedAmountBirr} Birr deposited by ${user.username} (Tx: ${verification.transactionId})"
          )
        }

        _uiState.update {
          it.copy(
            currentUser = updatedUser,
            isTelebirrVerifying = false,
            showTelebirrGatewayModal = false,
            activeTelebirrOrder = null
          )
        }
        refreshData()

        // Automatically complete cartela purchase for 3 games
        joinScheduledGame()
        showAlert("✔ ${methodObj.displayName} payment verified! ${verification.verifiedAmountBirr} birr added to your wallet. Cartela reserved for all 3 games!")
      } else {
        // Record declined payment in local repo and Firebase RTDB
        val declRequest = PaymentRequest(
          id = UUID.randomUUID().toString().take(8),
          username = user.username,
          phone = user.phone,
          amountBirr = order.totalAmountBirr,
          receiptId = if (verification.transactionId.isNotBlank()) verification.transactionId else "DEC-${System.currentTimeMillis() % 100000}",
          receiptLink = receiptRawInput.trim(),
          paymentMethod = paymentMethod,
          status = RequestStatus.DECLINED
        )
        repository.addPaymentRequest(declRequest)

        viewModelScope.launch(Dispatchers.IO) {
          repository.remoteService.savePaymentRequest(declRequest)
          repository.remoteService.logActivity(
            type = "DEPOSIT_DECLINED",
            username = user.username,
            amountBirr = order.totalAmountBirr,
            description = "${methodObj.displayName} Gateway: Deposit declined for ${user.username} (${verification.message})"
          )
        }

        _uiState.update {
          it.copy(
            isTelebirrVerifying = false,
            telebirrErrorMessage = verification.message
          )
        }
      }
    }
  }

  fun verifyTelebirrPayment(receiptRawInput: String) {
    verifyPaymentGateway(receiptRawInput, "telebirr")
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
    val now = repository.getSynchronizedTimeMs()
    val isSessionActive = activeSession != null
    val diffMs = if (isSessionActive) 0L else (nextSlot.startTime - now).coerceAtLeast(0L)
    val isCountdownFinished = isSessionActive || diffMs == 0L
    val targetSession = activeSession ?: nextSlot
    val sessionId = targetSession.sessionId

    // Check if any selected cartela is occupied by another player for this playing time
    val currentOccupied = _uiState.value.occupiedCartelas
    val conflictCard = selected.firstOrNull { num ->
      val owner = currentOccupied[num]
      owner != null && owner != user.username
    }
    if (conflictCard != null) {
      val occupiedWord = when (_uiState.value.language) {
        AppLanguage.EN -> "Occupied"
        AppLanguage.AM -> "ተይዟል"
        AppLanguage.TI -> "ተታሒዙ"
      }
      showToast(occupiedWord)
      showAlert(
        when (_uiState.value.language) {
          AppLanguage.EN -> "Cartela #$conflictCard is occupied by another player. Different players cannot buy the same cartela at the same playing time. Please select another cartela."
          AppLanguage.AM -> "ካርቴላ #$conflictCard በሌላ ተጫዋች ተይዟል (Occupied)። የተለያዩ ተጫዋቾች በተመሳሳይ የጨዋታ ሰዓት ተመሳሳይ ካርቴላ መግዛት አይችሉም። እባክዎ ሌላ ካርቴላ ይምረጡ።"
          AppLanguage.TI -> "ካርቴላ #$conflictCard ብኻልእ ተጻዋታይ ተታሒዙ እዩ (Occupied)። እባክኹም ካልእ ካርቴላ ሕረዩ።"
        }
      )
      return false
    }

    // If user has not purchased cartela ticket for this session yet:
    if (!_uiState.value.hasCartelaTicketForSession) {
      if (user.walletBalance < cost) {
        openTelebirrGatewayForGameEntry(selected.size)
        return false
      }

      // Deduct cartela fee (30 Birr per cartela covers all 3 consecutive games: 10 Birr per game)
      val newBal = user.walletBalance - cost
      val slotLabel = targetSession.slot.labelEn
      val tx = WalletTransaction(
        id = UUID.randomUUID().toString(),
        label = "Cartela ×${selected.size} for 3 Games (10 Birr/game - $slotLabel)",
        amount = -cost
      )
      val updatedUser = user.copy(
        walletBalance = newBal,
        transactions = user.transactions + tx
      )
      repository.saveUser(updatedUser)

      // Register player and claim cartelas in the shared session on Firebase RTDB
      repository.claimCartelas(sessionId, user.username, selected)
      viewModelScope.launch(Dispatchers.IO) {
        repository.remoteService.saveUser(updatedUser)
        repository.remoteService.registerPlayerInSession(sessionId, user.username, selected.size)
        repository.remoteService.claimSessionCartelas(sessionId, user.username, selected)
        repository.remoteService.logActivity(
          type = "CARTELA_PURCHASED",
          username = user.username,
          amountBirr = cost,
          description = "${user.username} purchased ${selected.size} cartela(s) #${selected.joinToString(",")} for $cost Birr"
        )
      }
      val updatedOccupied = _uiState.value.occupiedCartelas.toMutableMap()
      selected.forEach { updatedOccupied[it] = user.username }

      _uiState.update {
        it.copy(
          currentUser = updatedUser,
          hasCartelaTicketForSession = true,
          ticketCards = selected,
          occupiedCartelas = updatedOccupied
        )
      }
    }

    // STRICT CHECK: Can only start when countdown finishes at the playing time!
    if (!isCountdownFinished) {
      val targetLabel = (activeSession?.slot ?: nextSlot.slot).label(_uiState.value.language)
      showAlert(
        "✔ Cartela Purchased for $cost Birr (3 Games · 10 Birr per game)!\n\n" +
          "Game starts ONLY at your playing time:\n" +
          "⏰ $targetLabel (Ethiopian Time)\n\n" +
          "Starts in: ${_uiState.value.countdownText} when countdown reaches 00:00:00.\n" +
          "All players on different devices start together with identical numbers and target goals!\n" +
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
    } else if (_uiState.value.selectedCardsList.isNotEmpty()) {
      _uiState.value.selectedCardsList.sorted()
    } else {
      listOf(_uiState.value.selectedCardNumber.coerceIn(1, 500))
    }
    val slot = StringsDictionary.findSlot(user.playingTime) ?: StringsDictionary.SLOTS.first()
    val slotInfo = repository.getActiveSlotSession(user.playingTime) ?: repository.getNextSlot(user.playingTime)
    val sessionId = slotInfo.sessionId

    // If user hasn't manually purchased ticket before countdown reached 00:00:00:
    // Auto-enroll their selected cartela using wallet balance so they start with other players automatically
    if (!_uiState.value.hasCartelaTicketForSession) {
      val cost = selected.size * BingoRepository.PAY_BIRR
      if (user.walletBalance >= cost) {
        val newBal = user.walletBalance - cost
        val tx = WalletTransaction(
          id = UUID.randomUUID().toString(),
          label = "Cartela ×${selected.size} for 3 Games (10 Birr/game - ${user.playingTime})",
          amount = -cost
        )
        val updatedUser = user.copy(
          walletBalance = newBal,
          transactions = user.transactions + tx
        )
        repository.saveUser(updatedUser)
        repository.claimCartelas(sessionId, user.username, selected)
        viewModelScope.launch(Dispatchers.IO) {
          repository.remoteService.saveUser(updatedUser)
          repository.remoteService.claimSessionCartelas(sessionId, user.username, selected)
          repository.remoteService.registerPlayerInSession(sessionId, user.username, selected.size)
          repository.remoteService.logActivity(
            type = "CARTELA_PURCHASED",
            username = user.username,
            amountBirr = cost,
            description = "${user.username} auto-enrolled ${selected.size} cartela(s) #${selected.joinToString(",")} for $cost Birr"
          )
        }
        _uiState.update {
          it.copy(
            currentUser = updatedUser,
            hasCartelaTicketForSession = true,
            ticketCards = selected
          )
        }
      }
    }

    val cards = selected.map { cardNum ->
      BingoEngine.generateCard(cardNum, _uiState.value.cardSeed)
    }

    val slotLabel = user.playingTime
    val synchedNow = repository.getSynchronizedTimeMs()

    // Total purchased / playing cards in this playing time session:
    // User requested formula: 70% * (Total purchased cards in this slot) * 10 Birr per game
    val sessionOccupiedCards = repository.getOccupiedCartelas(sessionId).size.coerceAtLeast(selected.size).coerceAtLeast(1)
    val roundPrize = (sessionOccupiedCards * BingoRepository.BIRR_PER_GAME * BingoRepository.PRIZE_PERCENTAGE).toInt().coerceAtLeast(7)

    // Register user in the multiplayer session on Firebase & fetch players and total cards asynchronously
    viewModelScope.launch(Dispatchers.IO) {
      repository.remoteService.registerPlayerInSession(sessionId, user.username, selected.size)
      val count = repository.remoteService.fetchSessionPlayerCount(sessionId)
      val remoteTotalCards = repository.remoteService.fetchSessionOccupiedCartelasCount(sessionId)
      val actualCards = remoteTotalCards.coerceAtLeast(sessionOccupiedCards)
      val updatedPrize = (actualCards * BingoRepository.BIRR_PER_GAME * BingoRepository.PRIZE_PERCENTAGE).toInt().coerceAtLeast(7)
      _uiState.update {
        it.copy(
          activePlayerCount = count,
          winnerRewardBirr = updatedPrize
        )
      }
    }

    val firstRound = slotInfo.rounds.firstOrNull() ?: ActiveRound(
      1, "r1", synchedNow, synchedNow + 400000L, slotLabel
    )

    _uiState.update {
      it.copy(
        isPracticeMode = false,
        currentSessionId = sessionId,
        activeRound = firstRound,
        currentRoundNumber = 1,
        totalSessionRounds = 3,
        sessionTotalWonBirr = 0,
        isIntermission = false,
        sessionSummaryDialog = false,
        activeCards = cards,
        activeCardIndex = 0,
        winnerRewardBirr = roundPrize
      )
    }

    navigateTo(Screen.GAME)
    startRound(1)
  }

  fun startRound(roundNum: Int) {
    intermissionJob?.cancel()
    val isPractice = _uiState.value.isPracticeMode
    val user = _uiState.value.currentUser
    val slot = StringsDictionary.findSlot(user?.playingTime) ?: StringsDictionary.SLOTS.first()
    val dayIdx = repository.getEthiopianDayIndex()

    // Canonical synchronized seed and goal:
    // Every player on any device in this slot and round receives identical seed and goal!
    val seed = if (isPractice) {
      (1..999999999L).random()
    } else {
      repository.getCanonicalRoundSeed(slot.hourUtc, dayIdx, roundNum)
    }

    val goal = if (isPractice) {
      StringsDictionary.ALL_GOALS[((seed / 1000) % StringsDictionary.ALL_GOALS.size).toInt()]
    } else {
      repository.getCanonicalRoundGoal(slot.hourUtc, dayIdx, roundNum)
    }

    val cards = _uiState.value.activeCards.map { card ->
      card.copy(
        markedCells = setOf(Pair(2, 2)),
        winningCells = emptySet()
      )
    }

    val sessionId = _uiState.value.currentSessionId
    val localOccupiedCount = if (sessionId != null) repository.getOccupiedCartelas(sessionId).size else 0
    val totalPurchasedCards = localOccupiedCount.coerceAtLeast(_uiState.value.activeCards.size).coerceAtLeast(1)

    // Formula: Winner Reward = 70% * (purchased cards in that playing time) * 10 Birr per game
    val roundPrize = if (isPractice) {
      0
    } else {
      val roundPool = totalPurchasedCards * BingoRepository.BIRR_PER_GAME
      (roundPool * BingoRepository.PRIZE_PERCENTAGE).toInt().coerceAtLeast(7)
    }

    if (!isPractice && sessionId != null) {
      viewModelScope.launch(Dispatchers.IO) {
        val remoteCount = repository.remoteService.fetchSessionOccupiedCartelasCount(sessionId)
        if (remoteCount > totalPurchasedCards) {
          val remotePool = remoteCount * BingoRepository.BIRR_PER_GAME
          val updatedPrize = (remotePool * BingoRepository.PRIZE_PERCENTAGE).toInt().coerceAtLeast(7)
          _uiState.update { it.copy(winnerRewardBirr = updatedPrize) }
        }
      }
    }

    val slotInfo = if (!isPractice) {
      repository.getActiveSlotSession(user?.playingTime) ?: repository.getNextSlot(user?.playingTime)
    } else null
    val targetRound = slotInfo?.rounds?.getOrNull(roundNum - 1) ?: _uiState.value.activeRound

    _uiState.update {
      it.copy(
        currentRoundNumber = roundNum,
        activeRound = targetRound,
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
        winnerNotice = null,
        winnerRewardBirr = roundPrize,
        gameStatusMessage = ""
      )
    }

    launchGameLoop(seed, roundNum)
  }

  fun startIntermissionToNextRound() {
    intermissionJob?.cancel()
    val nextRound = _uiState.value.currentRoundNumber + 1
    val sessionId = _uiState.value.currentSessionId
    val synchedNow = repository.getSynchronizedTimeMs()
    val targetStart = synchedNow + 10000L // 10 seconds intermission

    _uiState.update {
      it.copy(
        isIntermission = true,
        intermissionSecondsLeft = 10,
        showWinDialog = false,
        gameStatusMessage = "Game ${_uiState.value.currentRoundNumber} of 3 complete! Game $nextRound starts in 10s..."
      )
    }
    soundManager.speak(
      "Game ${_uiState.value.currentRoundNumber} complete. Game $nextRound starting in 10 seconds.",
      "ጨዋታ ${_uiState.value.currentRoundNumber} ተጠናቋል። ጨዋታ $nextRound በ10 ሰከንድ ይጀምራል"
    )

    intermissionJob = viewModelScope.launch {
      // Synchronize exact shared start time on Firebase backend across all devices
      var canonicalStartMs = targetStart
      if (sessionId != null) {
        canonicalStartMs = withContext(Dispatchers.IO) {
          repository.remoteService.getOrCreateRoundStartTime(sessionId, nextRound, targetStart)
        }
      }

      while (true) {
        val now = repository.getSynchronizedTimeMs()
        val remainingMs = canonicalStartMs - now
        if (remainingMs <= 0) break
        val remainingSec = ((remainingMs + 999) / 1000).toInt()
        _uiState.update { it.copy(intermissionSecondsLeft = remainingSec.coerceAtLeast(1)) }
        delay(250L)
      }
      _uiState.update { it.copy(intermissionSecondsLeft = 0) }
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

  // --- Live Bingo Game Loop with Millisecond-Synchronized Ball Calling ---

  private fun launchGameLoop(seed: Long, roundNum: Int = _uiState.value.currentRoundNumber) {
    gameLoopJob?.cancel()
    val fullSequence = BingoEngine.generateCallingSequence(seed)
    val sessionId = _uiState.value.currentSessionId
    val isPractice = _uiState.value.isPracticeMode
    val myUsername = _uiState.value.currentUser?.username ?: ""

    gameLoopJob = viewModelScope.launch {
      // 1. Ready Countdown phase (Ready, 3, 2, 1) synchronized
      val steps = listOf("Ready", "3", "2", "1")
      for (s in steps) {
        _uiState.update { it.copy(readyCountdownStep = s) }
        soundManager.announceReady(s)
        delay(1000L)
      }
      _uiState.update { it.copy(readyCountdownStep = null) }

      // 2. Compute canonical start timestamp for ball drawing
      val ballBaseStartMs = repository.getSynchronizedTimeMs()

      // 3. Concurrent remote winner polling for real multiplayer
      val remotePollJob = if (!isPractice && sessionId != null) {
        launch(Dispatchers.IO) {
          while (!_uiState.value.isGameEnded && !_uiState.value.isIntermission) {
            delay(1500L)
            try {
              val remoteWinner = repository.remoteService.fetchRoundWinner(sessionId, roundNum)
              if (remoteWinner != null && remoteWinner.username.isNotBlank() && remoteWinner.username != myUsername) {
                // Another player called BINGO on their device!
                val winMsg = "${remoteWinner.username} called BINGO! (Card #${remoteWinner.cardNum})"
                val amharicWinMsg = "${remoteWinner.username} ቢንጎ ብሏል! (ካርድ #${remoteWinner.cardNum})"
                soundManager.speak(winMsg, amharicWinMsg)
                _uiState.update {
                  it.copy(
                    isGameEnded = true,
                    winnerNotice = winMsg,
                    gameStatusMessage = winMsg
                  )
                }
                delay(3000L)
                if (roundNum < 3) {
                  startIntermissionToNextRound()
                } else {
                  completeSession()
                }
                break
              }
            } catch (_: Exception) {}
          }
        }
      } else null

      // 4. Draw ball synchronously every 5000ms based on authoritative time
      for (index in fullSequence.indices) {
        if (_uiState.value.isGameEnded || _uiState.value.isIntermission) break

        val drawn = fullSequence[index]
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

        // Align next ball call exactly with authoritative millisecond grid
        val expectedNextCallTime = ballBaseStartMs + (index + 1) * BingoRepository.CALL_INTERVAL_MS
        val currentNow = repository.getSynchronizedTimeMs()
        val delayLeft = (expectedNextCallTime - currentNow).coerceIn(500L, BingoRepository.CALL_INTERVAL_MS)
        delay(delayLeft)
      }

      remotePollJob?.cancel()

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
    val sessionId = _uiState.value.currentSessionId

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

      val winnerRecord = WinnerRecord(
        id = UUID.randomUUID().toString().take(8),
        username = user.username,
        phone = user.phone,
        slotLabel = user.playingTime,
        roundNumber = currentRound,
        rewardBirr = reward,
        cardNum = winningCard.cardNum,
        callCount = calls
      )
      repository.addWinner(winnerRecord)

      // Post round winner, winner wallet, and activity directly to Firebase RTDB immediately
      viewModelScope.launch(Dispatchers.IO) {
        try {
          repository.remoteService.saveWinner(winnerRecord)
          repository.remoteService.saveUser(updatedUser)
          if (sessionId != null) {
            repository.remoteService.postRoundWinner(sessionId, currentRound, winnerRecord)
          }
          repository.remoteService.logActivity(
            type = "PLAYER_WIN",
            username = user.username,
            amountBirr = reward,
            description = "${user.username} won Round $currentRound (Card #${winningCard.cardNum}) - Reward: $reward Birr"
          )
        } catch (_: Exception) {}
      }

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

    // Automatically advance to intermission countdown after 3s celebration without requiring manual click
    viewModelScope.launch {
      delay(3000L)
      _uiState.update { it.copy(showWinDialog = false) }
      if (!_uiState.value.isPracticeMode && currentRound < 3) {
        startIntermissionToNextRound()
      } else if (!_uiState.value.isPracticeMode && currentRound >= 3) {
        completeSession()
      }
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
    viewModelScope.launch(Dispatchers.IO) {
      val list = repository.getPaymentRequests().toMutableList()
      val idx = list.indexOfFirst { it.id == requestId }
      if (idx < 0) return@launch
      val req = list[idx]
      val updatedReq = req.copy(status = RequestStatus.APPROVED, amountBirr = approvedAmount)
      list[idx] = updatedReq
      repository.savePaymentRequests(list)
      // Send directly to Firebase RTDB immediately
      repository.remoteService.savePaymentRequest(updatedReq)
      repository.remoteService.logActivity(
        type = "ADMIN_PAYMENT_APPROVED",
        username = req.username,
        amountBirr = approvedAmount,
        description = "Admin approved payment $requestId (+${approvedAmount} Birr to ${req.username})"
      )

      // Credit user's wallet
      val targetUser = repository.getUser(req.username) ?: repository.fetchRemoteUserBlocking(req.username)
      if (targetUser != null) {
        val newBal = targetUser.walletBalance + approvedAmount
        val tx = WalletTransaction(
          id = UUID.randomUUID().toString(),
          label = "Top-up approved: +$approvedAmount Birr",
          amount = approvedAmount,
          timestamp = System.currentTimeMillis()
        )
        val updated = targetUser.copy(
          walletBalance = newBal,
          transactions = targetUser.transactions + tx
        )
        repository.saveUserLocalOnly(updated)
        // Send directly to Firebase RTDB immediately
        repository.remoteService.saveUser(updated)
        withContext(Dispatchers.Main) {
          if (_uiState.value.currentUser?.username == req.username) {
            _uiState.update { it.copy(currentUser = updated) }
          }
        }
      }
      withContext(Dispatchers.Main) {
        refreshData(syncRemote = true)
        showToast("Payment approved: $approvedAmount birr credited to ${req.username}")
      }
    }
  }

  fun adminDeclinePayment(requestId: String) {
    viewModelScope.launch(Dispatchers.IO) {
      val list = repository.getPaymentRequests().toMutableList()
      val idx = list.indexOfFirst { it.id == requestId }
      if (idx < 0) return@launch
      val updatedReq = list[idx].copy(status = RequestStatus.DECLINED)
      list[idx] = updatedReq
      repository.savePaymentRequests(list)
      // Send directly to Firebase RTDB immediately
      repository.remoteService.savePaymentRequest(updatedReq)
      repository.remoteService.logActivity(
        type = "ADMIN_PAYMENT_DECLINED",
        username = updatedReq.username,
        amountBirr = updatedReq.amountBirr,
        description = "Admin declined payment $requestId (${updatedReq.username})"
      )
      withContext(Dispatchers.Main) {
        refreshData(syncRemote = true)
        showToast("Payment declined.")
      }
    }
  }

  fun adminApproveWithdrawal(requestId: String) {
    viewModelScope.launch(Dispatchers.IO) {
      val list = repository.getWithdrawalRequests().toMutableList()
      val idx = list.indexOfFirst { it.id == requestId }
      if (idx < 0) return@launch
      val updatedWd = list[idx].copy(status = RequestStatus.APPROVED)
      list[idx] = updatedWd
      repository.saveWithdrawalRequests(list)
      // Send directly to Firebase RTDB immediately
      repository.remoteService.saveWithdrawalRequest(updatedWd)
      repository.remoteService.logActivity(
        type = "ADMIN_WITHDRAWAL_APPROVED",
        username = updatedWd.username,
        amountBirr = updatedWd.amountBirr,
        description = "Admin approved withdrawal $requestId (${updatedWd.amountBirr} Birr to ${updatedWd.telebirrAccount})",
        extra = updatedWd.telebirrAccount
      )
      withContext(Dispatchers.Main) {
        refreshData(syncRemote = true)
        showToast("Withdrawal marked as paid.")
      }
    }
  }

  fun adminDeclineWithdrawal(requestId: String) {
    viewModelScope.launch(Dispatchers.IO) {
      val list = repository.getWithdrawalRequests().toMutableList()
      val idx = list.indexOfFirst { it.id == requestId }
      if (idx < 0) return@launch
      val req = list[idx]
      val updatedWd = req.copy(status = RequestStatus.DECLINED)
      list[idx] = updatedWd
      repository.saveWithdrawalRequests(list)
      // Send directly to Firebase RTDB immediately
      repository.remoteService.saveWithdrawalRequest(updatedWd)
      repository.remoteService.logActivity(
        type = "ADMIN_WITHDRAWAL_DECLINED",
        username = req.username,
        amountBirr = req.amountBirr,
        description = "Admin declined withdrawal $requestId and refunded ${req.amountBirr} Birr to ${req.username}"
      )

      // Refund user
      val targetUser = repository.getUser(req.username) ?: repository.fetchRemoteUserBlocking(req.username)
      if (targetUser != null) {
        val newBal = targetUser.walletBalance + req.amountBirr
        val tx = WalletTransaction(
          id = UUID.randomUUID().toString(),
          label = "Withdrawal refund: +${req.amountBirr} Birr",
          amount = req.amountBirr,
          timestamp = System.currentTimeMillis()
        )
        val updated = targetUser.copy(
          walletBalance = newBal,
          transactions = targetUser.transactions + tx
        )
        repository.saveUserLocalOnly(updated)
        // Send directly to Firebase RTDB immediately
        repository.remoteService.saveUser(updated)
        withContext(Dispatchers.Main) {
          if (_uiState.value.currentUser?.username == req.username) {
            _uiState.update { it.copy(currentUser = updated) }
          }
        }
      }
      withContext(Dispatchers.Main) {
        refreshData(syncRemote = true)
        showToast("Withdrawal declined and ${req.amountBirr} birr refunded.")
      }
    }
  }

  fun adminAdjustUserBalance(username: String, deltaBirr: Int) {
    viewModelScope.launch(Dispatchers.IO) {
      val targetUser = repository.getUser(username) ?: repository.fetchRemoteUserBlocking(username) ?: return@launch
      val newBal = (targetUser.walletBalance + deltaBirr).coerceAtLeast(0)
      val label = if (deltaBirr > 0) "Admin credit: +$deltaBirr Birr" else "Admin deduction: $deltaBirr Birr"
      val tx = WalletTransaction(
        id = UUID.randomUUID().toString(),
        label = label,
        amount = deltaBirr,
        timestamp = System.currentTimeMillis()
      )
      val updated = targetUser.copy(
        walletBalance = newBal,
        transactions = targetUser.transactions + tx
      )
      repository.saveUserLocalOnly(updated)
      // Send directly to Firebase RTDB immediately
      repository.remoteService.saveUser(updated)
      repository.remoteService.logActivity(
        type = if (deltaBirr > 0) "ADMIN_ADD_FUNDS" else "ADMIN_DEDUCT_FUNDS",
        username = username,
        amountBirr = deltaBirr,
        description = "Admin adjusted balance for $username: ${if (deltaBirr > 0) "+$deltaBirr" else "$deltaBirr"} Birr (New Balance: $newBal Birr)"
      )
      withContext(Dispatchers.Main) {
        if (_uiState.value.currentUser?.username == username) {
          _uiState.update { it.copy(currentUser = updated) }
        }
        refreshData(syncRemote = true)
        showToast("Balance updated for $username: $newBal birr")
      }
    }
  }

  fun adminDeleteUser(username: String) {
    viewModelScope.launch(Dispatchers.IO) {
      repository.deleteUser(username)
      // Delete directly from Firebase RTDB immediately
      repository.remoteService.deleteUser(username)
      repository.remoteService.logActivity(
        type = "ADMIN_DELETE_USER",
        username = username,
        amountBirr = 0,
        description = "Admin deleted user $username"
      )
      withContext(Dispatchers.Main) {
        refreshData(syncRemote = true)
        showToast("User $username deleted.")
      }
    }
  }

  fun refreshData(syncRemote: Boolean = false) {
    if (syncRemote) {
      viewModelScope.launch(Dispatchers.IO) {
        repository.syncWithRemoteServer()
        val currentSessionName = repository.getCurrentSession()
        val user = currentSessionName?.let { repository.getUser(it) }
        val activities = try { repository.remoteService.fetchActivities() } catch (_: Exception) { emptyList() }
        _uiState.update {
          it.copy(
            currentUser = user,
            paymentRequests = repository.getPaymentRequests(),
            withdrawalRequests = repository.getWithdrawalRequests(),
            winnersHistory = repository.getWinners(),
            allUsers = repository.getAllUsernames().mapNotNull { name -> repository.getUser(name) },
            activitiesHistory = activities.ifEmpty { it.activitiesHistory }
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
