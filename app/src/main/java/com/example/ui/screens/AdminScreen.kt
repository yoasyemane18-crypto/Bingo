package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.model.ActivityRecord
import com.example.model.PaymentRequest
import com.example.model.RequestStatus
import com.example.model.UserProfile
import com.example.model.WinnerRecord
import com.example.model.WithdrawalRequest
import com.example.ui.components.LuxuryButton
import com.example.ui.theme.BingoBorderLight
import com.example.ui.theme.BingoBorderPurple
import com.example.ui.theme.BingoCardBg
import com.example.ui.theme.BingoGold
import com.example.ui.theme.BingoGoldDark
import com.example.ui.theme.BingoGoldLight
import com.example.ui.theme.BingoGreen
import com.example.ui.theme.BingoGreenLight
import com.example.ui.theme.BingoNight
import com.example.ui.theme.BingoPurpleDeep
import com.example.ui.theme.BingoRed
import com.example.ui.theme.BingoRedLight
import com.example.ui.theme.BingoSurface
import com.example.ui.theme.BingoTextDark
import com.example.ui.theme.BingoTextMuted
import com.example.ui.theme.BingoTextWhite
import com.example.ui.theme.PlayfairFontFamily

@Composable
fun AdminScreen(
  isAdminLoggedIn: Boolean,
  paymentRequests: List<PaymentRequest>,
  withdrawalRequests: List<WithdrawalRequest>,
  allUsers: List<UserProfile>,
  winnersHistory: List<WinnerRecord>,
  activitiesHistory: List<ActivityRecord> = emptyList(),
  onAdminLogin: (String, String) -> Unit,
  onAdminLogout: () -> Unit,
  onApprovePayment: (String, Int) -> Unit,
  onDeclinePayment: (String) -> Unit,
  onApproveWithdrawal: (String) -> Unit,
  onDeclineWithdrawal: (String) -> Unit,
  onAdjustBalance: (String, Int) -> Unit,
  onDeleteUser: (String) -> Unit,
  onRefreshData: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var emailInput by remember { mutableStateOf("") }
  var passwordInput by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var selectedTab by remember { mutableIntStateOf(0) }
  var userSearchQuery by remember { mutableStateOf("") }
  var userToDelete by remember { mutableStateOf<String?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(BingoNight)
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    if (!isAdminLoggedIn) {
      // Admin Login Form
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = BingoCardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, BingoGold.copy(alpha = 0.4f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("admin_login_card")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "Admin Login",
            fontFamily = PlayfairFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = BingoGoldLight
          )

          Spacer(modifier = Modifier.height(18.dp))

          OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            label = { Text("Admin Email") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("admin_email_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = BingoGold,
              unfocusedBorderColor = BingoBorderLight,
              focusedTextColor = BingoTextWhite,
              unfocusedTextColor = BingoTextWhite
            ),
            shape = RoundedCornerShape(12.dp)
          )

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            label = { Text("Password") },
            trailingIcon = {
              IconButton(
                onClick = { passwordVisible = !passwordVisible },
                modifier = Modifier.testTag("admin_password_visibility_toggle")
              ) {
                Icon(
                  imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = if (passwordVisible) "Hide password" else "Show password",
                  tint = BingoGoldLight
                )
              }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("admin_password_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = BingoGold,
              unfocusedBorderColor = BingoBorderLight,
              focusedTextColor = BingoTextWhite,
              unfocusedTextColor = BingoTextWhite
            ),
            shape = RoundedCornerShape(12.dp)
          )

          Spacer(modifier = Modifier.height(20.dp))

          LuxuryButton(
            text = "Login as Admin",
            onClick = { onAdminLogin(emailInput, passwordInput) },
            isGold = true,
            testTag = "admin_submit_login_button"
          )
        }
      }
    } else {
      // Admin Dashboard with Tabs
      val pendingPays = paymentRequests.filter { it.status == RequestStatus.PENDING }
      val pendingWds = withdrawalRequests.filter { it.status == RequestStatus.PENDING }

      val tabTitles = listOf(
        "Payments (${paymentRequests.size})",
        "Users (${allUsers.size})",
        "Withdrawals (${pendingWds.size})",
        "Winners",
        "Live Feed (${activitiesHistory.size})",
        "Logout"
      )

      ScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = BingoSurface,
        contentColor = BingoGold,
        edgePadding = 8.dp,
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
      ) {
        tabTitles.forEachIndexed { index, title ->
          Tab(
            selected = selectedTab == index,
            onClick = {
              if (index == 5) {
                onAdminLogout()
              } else {
                selectedTab = index
              }
            },
            text = {
              Text(
                text = title,
                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp,
                color = if (index == 5) BingoRedLight else if (selectedTab == index) BingoGold else BingoTextWhite
              )
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      when (selectedTab) {
        // Tab 0: Payments (Auto-Processed)
        0 -> {
          Text(
            text = "Payment Transactions (${paymentRequests.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = BingoGoldLight
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "⚡ System Auto-Verification Active: Official receiver 0939383425 (Yemane) and amount matching are verified automatically upon submission.",
            fontSize = 12.sp,
            color = BingoGreenLight
          )

          Spacer(modifier = Modifier.height(10.dp))

          if (paymentRequests.isEmpty()) {
            Text("No payments submitted yet.", color = BingoTextMuted, fontSize = 14.sp)
          } else {
            paymentRequests.reversed().forEach { p ->
              var editAmt by remember(p.id) { mutableStateOf(p.amountBirr.toString()) }

              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BingoCardBg),
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  when (p.status) {
                    RequestStatus.APPROVED -> BingoGreen.copy(alpha = 0.5f)
                    RequestStatus.DECLINED -> BingoRed.copy(alpha = 0.5f)
                    RequestStatus.PENDING -> BingoGold.copy(alpha = 0.5f)
                  }
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp)
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "${p.username} · ${p.phone}",
                      fontWeight = FontWeight.Bold,
                      fontSize = 16.sp,
                      color = BingoGold
                    )

                    // Status Badge
                    Surface(
                      color = when (p.status) {
                        RequestStatus.APPROVED -> BingoGreen.copy(alpha = 0.2f)
                        RequestStatus.DECLINED -> BingoRed.copy(alpha = 0.2f)
                        RequestStatus.PENDING -> BingoGold.copy(alpha = 0.2f)
                      },
                      shape = RoundedCornerShape(8.dp),
                      border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when (p.status) {
                          RequestStatus.APPROVED -> BingoGreen
                          RequestStatus.DECLINED -> BingoRed
                          RequestStatus.PENDING -> BingoGold
                        }
                      )
                    ) {
                      Text(
                        text = when (p.status) {
                          RequestStatus.APPROVED -> "✔ APPROVED"
                          RequestStatus.DECLINED -> "✖ DECLINED"
                          RequestStatus.PENDING -> "⏳ PENDING"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (p.status) {
                          RequestStatus.APPROVED -> BingoGreenLight
                          RequestStatus.DECLINED -> BingoRedLight
                          RequestStatus.PENDING -> BingoGoldLight
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(4.dp))

                  val method = com.example.payment.PaymentMethod.fromCode(p.paymentMethod)
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Surface(
                      color = BingoPurpleDeep,
                      shape = RoundedCornerShape(6.dp),
                      border = androidx.compose.foundation.BorderStroke(1.dp, BingoGold.copy(alpha = 0.5f))
                    ) {
                      Text(
                        text = method.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BingoGoldLight,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                    Text(
                      text = "Amount: ${p.amountBirr} Birr · Receipt: ${p.receiptId}",
                      fontSize = 14.sp,
                      color = BingoTextWhite
                    )
                  }

                  if (p.receiptLink.isNotEmpty()) {
                    Text(
                      text = p.receiptLink,
                      fontSize = 12.sp,
                      color = if (p.status == RequestStatus.DECLINED) BingoRedLight.copy(alpha = 0.8f) else BingoTextMuted
                    )
                  }

                  // Only show manual override actions if legacy pending
                  if (p.status == RequestStatus.PENDING) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      OutlinedTextField(
                        value = editAmt,
                        onValueChange = { editAmt = it },
                        label = { Text("Birr") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                          focusedTextColor = BingoTextWhite,
                          unfocusedTextColor = BingoTextWhite
                        )
                      )

                      Button(
                        onClick = {
                          val amt = editAmt.toIntOrNull() ?: p.amountBirr
                          onApprovePayment(p.id, amt)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BingoGreen),
                        shape = RoundedCornerShape(8.dp)
                      ) {
                        Text("Approve", color = Color.White, fontWeight = FontWeight.Bold)
                      }

                      Button(
                        onClick = { onDeclinePayment(p.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = BingoRed),
                        shape = RoundedCornerShape(8.dp)
                      ) {
                        Text("Decline", color = Color.White, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // Tab 1: Users
        1 -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Registered Players (${allUsers.size})",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = BingoGoldLight
            )

            Button(
              onClick = onRefreshData,
              colors = ButtonDefaults.buttonColors(containerColor = BingoGoldDark),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("Sync Cloud", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = userSearchQuery,
            onValueChange = { userSearchQuery = it },
            placeholder = { Text("Search username or phone", color = BingoTextMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = BingoTextWhite,
              unfocusedTextColor = BingoTextWhite
            )
          )

          Spacer(modifier = Modifier.height(12.dp))

          val filtered = allUsers.filter {
            it.username.contains(userSearchQuery, ignoreCase = true) ||
              it.phone.contains(userSearchQuery, ignoreCase = true)
          }

          if (filtered.isEmpty()) {
            Text("No users found.", color = BingoTextMuted, fontSize = 14.sp)
          } else {
            filtered.forEach { u ->
              var adjustInput by remember(u.username) { mutableStateOf("") }

              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BingoCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, BingoBorderPurple),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp)
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Text(
                    text = "${u.username} · ${u.phone}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BingoGold
                  )
                  Text(
                    text = "Playing Time: ${u.playingTime}",
                    fontSize = 13.sp,
                    color = BingoTextMuted
                  )
                  Text(
                    text = "Wallet: ${u.walletBalance} birr",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BingoTextWhite
                  )

                  Spacer(modifier = Modifier.height(8.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    OutlinedTextField(
                      value = adjustInput,
                      onValueChange = { adjustInput = it },
                      placeholder = { Text("birr") },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      modifier = Modifier.weight(1f),
                      singleLine = true,
                      colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BingoTextWhite,
                        unfocusedTextColor = BingoTextWhite
                      )
                    )

                    Button(
                      onClick = {
                        val v = adjustInput.toIntOrNull() ?: 0
                        if (v > 0) onAdjustBalance(u.username, v)
                        adjustInput = ""
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = BingoGreen),
                      shape = RoundedCornerShape(8.dp)
                    ) {
                      Text("+Add", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                      onClick = {
                        val v = adjustInput.toIntOrNull() ?: 0
                        if (v > 0) onAdjustBalance(u.username, -v)
                        adjustInput = ""
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = BingoGoldDark),
                      shape = RoundedCornerShape(8.dp)
                    ) {
                      Text("-Deduct", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  Button(
                    onClick = { userToDelete = u.username },
                    colors = ButtonDefaults.buttonColors(containerColor = BingoRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text("Delete User", color = Color.White, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }

          if (userToDelete != null) {
            AlertDialog(
              onDismissRequest = { userToDelete = null },
              title = {
                Text(
                  text = "Confirm Delete User",
                  fontWeight = FontWeight.Bold,
                  color = BingoGoldLight
                )
              },
              text = {
                Text(
                  text = "Are you sure you want to delete user '${userToDelete}'? Their wallet balance and account data will be permanently removed.",
                  color = BingoTextWhite
                )
              },
              confirmButton = {
                Button(
                  onClick = {
                    userToDelete?.let { onDeleteUser(it) }
                    userToDelete = null
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = BingoRed)
                ) {
                  Text("Confirm Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
              },
              dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                  Text("Cancel", color = BingoGold)
                }
              },
              containerColor = BingoCardBg
            )
          }
        }

        // Tab 2: Withdrawals
        2 -> {
          Text(
            text = "Pending Withdrawals (${pendingWds.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = BingoGoldLight
          )

          Spacer(modifier = Modifier.height(10.dp))

          if (pendingWds.isEmpty()) {
            Text("No pending withdrawals.", color = BingoTextMuted, fontSize = 14.sp)
          } else {
            pendingWds.forEach { w ->
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BingoCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, BingoBorderPurple),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp)
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Text(
                    text = "${w.username} · ${w.phone}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BingoGold
                  )
                  val wMethod = com.example.payment.PaymentMethod.fromCode(w.paymentMethod)
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Surface(
                      color = BingoPurpleDeep,
                      shape = RoundedCornerShape(6.dp),
                      border = androidx.compose.foundation.BorderStroke(1.dp, BingoGold.copy(alpha = 0.5f))
                    ) {
                      Text(
                        text = wMethod.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BingoGoldLight,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                    Text(
                      text = "Send ${w.amountBirr} birr to ${w.telebirrAccount}",
                      fontSize = 15.sp,
                      color = BingoTextWhite
                    )
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Button(
                      onClick = { onApproveWithdrawal(w.id) },
                      colors = ButtonDefaults.buttonColors(containerColor = BingoGreen),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f)
                    ) {
                      Text("Mark Paid", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                      onClick = { onDeclineWithdrawal(w.id) },
                      colors = ButtonDefaults.buttonColors(containerColor = BingoRed),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f)
                    ) {
                      Text("Decline & Refund", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }

        // Tab 3: Winners
        3 -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Real Game Winners (${winnersHistory.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = BingoGoldLight
              )
              Text(
                text = "Live Firebase backend registry",
                color = BingoTextMuted,
                fontSize = 12.sp
              )
            }

            Surface(
              shape = RoundedCornerShape(10.dp),
              color = BingoPurpleDeep,
              border = androidx.compose.foundation.BorderStroke(1.dp, BingoGold.copy(alpha = 0.6f)),
              modifier = Modifier
                .clickable { onRefreshData() }
                .testTag("admin_refresh_winners_btn")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = "Refresh",
                  tint = BingoGold,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Sync",
                  color = BingoGoldLight,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          if (winnersHistory.isEmpty()) {
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = BingoCardBg),
              border = androidx.compose.foundation.BorderStroke(1.dp, BingoBorderPurple),
              modifier = Modifier.fillMaxWidth()
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(24.dp),
                contentAlignment = Alignment.Center
              ) {
                Text("No winners recorded yet in Firebase backend.", color = BingoTextMuted, fontSize = 14.sp)
              }
            }
          } else {
            winnersHistory.forEach { rec ->
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BingoCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, BingoBorderPurple),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 5.dp)
                  .testTag("admin_winner_card_${rec.id}")
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "🏆 ${rec.username}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BingoGold
                      )
                      if (rec.phone.isNotBlank()) {
                        Text(
                          text = "Phone: ${rec.phone}",
                          color = BingoTextMuted,
                          fontSize = 12.sp
                        )
                      }
                    }

                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = BingoPurpleDeep,
                      border = androidx.compose.foundation.BorderStroke(1.dp, BingoGold)
                    ) {
                      Text(
                        text = "${rec.rewardBirr} ETB Won",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BingoGoldLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = BingoNight,
                      border = androidx.compose.foundation.BorderStroke(1.dp, BingoBorderLight)
                    ) {
                      Text(
                        text = "Game ${rec.roundNumber} of 3",
                        fontSize = 11.sp,
                        color = BingoGreenLight,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                      )
                    }

                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = BingoNight,
                      border = androidx.compose.foundation.BorderStroke(1.dp, BingoBorderLight)
                    ) {
                      Text(
                        text = "Cartela #${rec.cardNum}",
                        fontSize = 11.sp,
                        color = BingoTextWhite,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                      )
                    }

                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = BingoNight,
                      border = androidx.compose.foundation.BorderStroke(1.dp, BingoBorderLight)
                    ) {
                      Text(
                        text = "${rec.callCount} calls",
                        fontSize = 11.sp,
                        color = BingoTextMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(6.dp))

                  val timeFormatted = java.text.SimpleDateFormat("MMM dd, yyyy · HH:mm:ss", java.util.Locale.US)
                    .format(java.util.Date(rec.timestamp))
                  Text(
                    text = "Slot: ${rec.slotLabel} · $timeFormatted",
                    color = BingoTextMuted,
                    fontSize = 11.sp
                  )
                }
              }
            }
          }
        }

        // Tab 4: Live Server Activities (Firebase RTDB Realtime Feed)
        4 -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Live Activities Feed (${activitiesHistory.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = BingoGoldLight
              )
              Text(
                text = "Real-time sync directly with Firebase Server",
                color = BingoGreenLight,
                fontSize = 12.sp
              )
            }
            Button(
              onClick = onRefreshData,
              colors = ButtonDefaults.buttonColors(containerColor = BingoGold),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Refresh", color = BingoNight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          if (activitiesHistory.isEmpty()) {
            Text("No server activities logged yet.", color = BingoTextMuted, fontSize = 14.sp)
          } else {
            activitiesHistory.forEach { act ->
              val badgeColor = when (act.type) {
                "DEPOSIT_REQUEST", "DEPOSIT_APPROVED" -> BingoGreenLight
                "WITHDRAWAL_REQUEST", "WITHDRAWAL_APPROVED" -> BingoGold
                "ADMIN_ADJUST", "ADMIN_DECLINE" -> BingoRedLight
                "WINNER" -> BingoGoldLight
                "REGISTRATION" -> BingoBorderPurple
                else -> BingoBorderLight
              }
              Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BingoCardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = act.type.replace('_', ' '),
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                      color = badgeColor
                    )
                    Text(
                      text = act.formatTime(),
                      color = BingoTextMuted,
                      fontSize = 11.sp
                    )
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "${act.username} · ${act.description}",
                    color = BingoTextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                  )
                  if (act.amountBirr > 0) {
                    Text(
                      text = "Amount: ${act.amountBirr} Birr",
                      color = BingoGold,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  }
                  if (act.extra.isNotEmpty()) {
                    Text(
                      text = act.extra,
                      color = BingoTextMuted,
                      fontSize = 11.sp
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(40.dp))
  }
}
