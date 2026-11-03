package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.model.AppLanguage
import com.example.model.Screen
import com.example.ui.components.TelebirrPaymentDialog
import com.example.ui.components.YemeDrawer
import com.example.ui.components.YemeNoticeModal
import com.example.ui.components.YemeTopBar
import com.example.ui.screens.AboutUsScreen
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.ContactUsScreen
import com.example.ui.screens.FaqsScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HowToPlayScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.theme.BingoNight
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BingoViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private val viewModel: BingoViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        YemeBingoApp(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun YemeBingoApp(viewModel: BingoViewModel) {
  val uiState by viewModel.uiState.collectAsState()
  val scope = rememberCoroutineScope()
  var isDrawerOpen by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  // Listen to toast messages
  LaunchedEffect(uiState.toastMessage) {
    uiState.toastMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearToast()
    }
  }

  // Predictive / Standard Back handling
  BackHandler(enabled = isDrawerOpen || uiState.navigationStack.size > 1) {
    if (isDrawerOpen) {
      isDrawerOpen = false
    } else {
      viewModel.handleBack()
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(BingoNight)
      .safeDrawingPadding()
  ) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = BingoNight,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        YemeTopBar(
          currentLanguage = uiState.language,
          isDrawerOpen = isDrawerOpen,
          onLanguageClick = {
            val nextLang = when (uiState.language) {
              AppLanguage.EN -> AppLanguage.AM
              AppLanguage.AM -> AppLanguage.TI
              AppLanguage.TI -> AppLanguage.EN
            }
            viewModel.setLanguage(nextLang)
          },
          onToggleDrawer = { isDrawerOpen = !isDrawerOpen },
          onBrandClick = {
            isDrawerOpen = false
            viewModel.navigateTo(Screen.HOME)
          }
        )
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        when (uiState.currentScreen) {
          Screen.HOME -> HomeScreen(
            countdownText = uiState.countdownText,
            nextGameLabel = uiState.nextGameLabel,
            currentUser = uiState.currentUser,
            language = uiState.language,
            onLogin = { u, p -> viewModel.login(u, p) },
            onNavigate = { screen -> viewModel.navigateTo(screen) },
            onStartFreePlay = { viewModel.startPracticeGame() }
          )

          Screen.REGISTER -> RegisterScreen(
            language = uiState.language,
            onRegister = { u, ph, t, p -> viewModel.register(u, ph, t, p) },
            onNavigate = { screen -> viewModel.navigateTo(screen) }
          )

          Screen.PLAY_LOBBY -> LobbyScreen(
            countdownText = uiState.countdownText,
            nextGameLabel = uiState.nextGameLabel,
            currentUser = uiState.currentUser,
            language = uiState.language,
            selectedCardNumber = uiState.selectedCardNumber,
            selectedCardsList = uiState.selectedCardsList,
            previewCard = uiState.previewCard,
            paymentRequests = uiState.paymentRequests,
            withdrawalRequests = uiState.withdrawalRequests,
            onCardSelected = { num -> viewModel.selectCard(num) },
            onRefreshSeed = { viewModel.refreshSeed() },
            onStartGame = {
              if (uiState.currentUser != null) {
                viewModel.joinScheduledGame()
              } else {
                viewModel.startPracticeGame()
              }
            },
            onOpenTelebirrGateway = { count -> viewModel.openTelebirrGatewayForGameEntry(count) },
            onSubmitPayment = { amt, r -> viewModel.submitPayment(amt, r) },
            onSubmitWithdrawal = { amt, acct -> viewModel.submitWithdrawal(amt, acct) },
            onChangePlayingTime = { newTime -> viewModel.changePlayingTime(newTime) }
          )

          Screen.GAME -> GameScreen(
            language = uiState.language,
            targetGoal = uiState.targetGoal,
            activeRound = uiState.activeRound,
            rewardBirr = uiState.winnerRewardBirr,
            activeCards = uiState.activeCards,
            activeCardIndex = uiState.activeCardIndex,
            calledNumbers = uiState.calledNumbers,
            lastCalledNumber = uiState.lastCalledNumber,
            isAutoMark = uiState.isAutoMark,
            isSoundOn = uiState.isSoundOn,
            voiceLanguage = uiState.voiceLanguage,
            readyStep = uiState.readyCountdownStep,
            isGameEnded = uiState.isGameEnded,
            showWinDialog = uiState.showWinDialog,
            showConfetti = uiState.showConfetti,
            winnerCardNum = uiState.winnerCardNum,
            winnerCallsCount = uiState.winnerCallsCount,
            onCardTabSelected = { idx -> viewModel.setActiveCardIndex(idx) },
            onCellTapped = { cIdx, col, row -> viewModel.tapCell(cIdx, col, row) },
            onClaimBingo = { viewModel.claimBingo() },
            onToggleAutoMark = { viewModel.toggleAutoMark() },
            onToggleSound = { viewModel.toggleSound() },
            onToggleVoiceLang = { viewModel.toggleVoiceLanguage() },
            onBack = {
              viewModel.stopGame()
              viewModel.navigateTo(Screen.PLAY_LOBBY)
            },
            onDismissWinDialog = { viewModel.dismissWinDialog() }
          )

          Screen.SCHEDULE -> ScheduleScreen(
            language = uiState.language,
            nextGameLabel = uiState.nextGameLabel
          )

          Screen.HOW_TO_PLAY -> HowToPlayScreen(
            language = uiState.language
          )

          Screen.ABOUT_US -> AboutUsScreen(
            language = uiState.language
          )

          Screen.CONTACT_US -> ContactUsScreen(
            language = uiState.language
          )

          Screen.FAQS -> FaqsScreen(
            language = uiState.language
          )

          Screen.ADMIN -> AdminScreen(
            isAdminLoggedIn = uiState.isAdminLoggedIn,
            paymentRequests = uiState.paymentRequests,
            withdrawalRequests = uiState.withdrawalRequests,
            allUsers = uiState.allUsers,
            winnersHistory = uiState.winnersHistory,
            onAdminLogin = { e, p -> viewModel.adminLogin(e, p) },
            onAdminLogout = { viewModel.adminLogout() },
            onApprovePayment = { id, amt -> viewModel.adminApprovePayment(id, amt) },
            onDeclinePayment = { id -> viewModel.adminDeclinePayment(id) },
            onApproveWithdrawal = { id -> viewModel.adminApproveWithdrawal(id) },
            onDeclineWithdrawal = { id -> viewModel.adminDeclineWithdrawal(id) },
            onAdjustBalance = { u, delta -> viewModel.adminAdjustUserBalance(u, delta) },
            onDeleteUser = { u -> viewModel.adminDeleteUser(u) }
          )
        }

        // Drawer Overlay
        if (isDrawerOpen) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color.Black.copy(alpha = 0.5f))
              .clickable { isDrawerOpen = false }
          )

          YemeDrawer(
            currentScreen = uiState.currentScreen,
            username = uiState.currentUser?.username,
            currentLanguage = uiState.language,
            onLanguageSelected = { lang ->
              viewModel.setLanguage(lang)
            },
            onNavigate = { screen ->
              isDrawerOpen = false
              viewModel.navigateTo(screen)
            },
            onLogout = {
              isDrawerOpen = false
              viewModel.logout()
            },
            onClose = { isDrawerOpen = false }
          )
        }
      }
    }

    // Modal Notice Alert Dialog
    uiState.alertNotice?.let { notice ->
      YemeNoticeModal(
        title = "YEME BINGO",
        message = notice,
        onDismiss = { viewModel.clearAlert() }
      )
    }

    // Telebirr Payment Gateway Dialog
    if (uiState.showTelebirrGatewayModal && uiState.activeTelebirrOrder != null) {
      TelebirrPaymentDialog(
        order = uiState.activeTelebirrOrder!!,
        isVerifying = uiState.isTelebirrVerifying,
        errorMessage = uiState.telebirrErrorMessage,
        onOpenTelebirrApp = { viewModel.launchTelebirrPaymentApp() },
        onCopyDetails = { viewModel.copyTelebirrDetails() },
        onVerifyReceipt = { receipt -> viewModel.verifyTelebirrPayment(receipt) },
        onDismiss = { viewModel.dismissTelebirrGateway() }
      )
    }
  }
}
