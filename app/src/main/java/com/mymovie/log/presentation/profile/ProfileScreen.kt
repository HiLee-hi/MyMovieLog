package com.mymovie.log.presentation.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mymovie.log.domain.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit = {}
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val emailAuthUiState by viewModel.emailAuthUiState.collectAsStateWithLifecycle()

    var showLoginSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Remember whether the user was logged out when this screen first composed (prevents navigation if already logged in on app start)
    var wasLoggedOut by remember { mutableStateOf(currentUser == null) }

    // Detect login transition (null → non-null) — handles both email and Google sign-in
    LaunchedEffect(currentUser) {
        if (wasLoggedOut && currentUser != null) {
            onLoginSuccess()
        }
        wasLoggedOut = currentUser == null
    }

    // Auto-close BottomSheet only on sign-in success (keep it open while awaiting email verification after sign-up)
    LaunchedEffect(emailAuthUiState) {
        if (emailAuthUiState is EmailAuthUiState.Success) {
            sheetState.hide()
            showLoginSheet = false
            viewModel.resetEmailAuthState()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("프로필", fontWeight = FontWeight.Bold) })

        if (currentUser == null) {
            LoginPromptContent(
                onEmailLogin = { showLoginSheet = true },
                onGoogleLogin = viewModel::signInWithGoogle
            )
        } else {
            UserProfileContent(
                user = currentUser!!,
                onSignOut = viewModel::signOut
            )
        }
    }

    if (showLoginSheet) {
        EmailLoginBottomSheet(
            emailAuthUiState = emailAuthUiState,
            sheetState = sheetState,
            onDismiss = {
                showLoginSheet = false
                viewModel.resetEmailAuthState()
            },
            onSignIn = viewModel::signInWithEmail,
            onSignUp = viewModel::signUpWithEmail,
            onResetState = viewModel::resetEmailAuthState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmailLoginBottomSheet(
    emailAuthUiState: EmailAuthUiState,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onResetState: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoading = emailAuthUiState is EmailAuthUiState.Loading
    val isVerificationRequired = emailAuthUiState is EmailAuthUiState.EmailVerificationRequired
    val errorMessage = (emailAuthUiState as? EmailAuthUiState.Error)?.message

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        // Email verification prompt shown after sign-up
        if (isVerificationRequired) {
            EmailVerificationContent(
                onConfirmed = {
                    // Switch to sign-in mode after verification is confirmed
                    onResetState()
                    isSignUpMode = false
                }
            )
            return@ModalBottomSheet
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isSignUpMode) "회원가입" else "이메일로 로그인",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("이메일") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "비밀번호 숨기기" else "비밀번호 보기"
                        )
                    }
                },
                singleLine = true,
                enabled = !isLoading
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isSignUpMode) onSignUp(email, password)
                    else onSignIn(email, password)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isSignUpMode) "회원가입" else "로그인")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    isSignUpMode = !isSignUpMode
                    onResetState()  // clear any previous error when switching modes
                },
                enabled = !isLoading
            ) {
                Text(
                    text = if (isSignUpMode) "이미 계정이 있으신가요? 로그인" else "계정이 없으신가요? 회원가입",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun EmailVerificationContent(onConfirmed: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "이메일을 확인해주세요",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "가입하신 이메일로 인증 링크를 발송했습니다.\n링크를 클릭한 후 로그인해주세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onConfirmed,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("인증 완료 — 로그인하기")
        }
    }
}

@Composable
private fun LoginPromptContent(onEmailLogin: () -> Unit, onGoogleLogin: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("로그인하여 기록을 저장하세요", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onEmailLogin,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("이메일로 로그인")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onGoogleLogin,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Google로 로그인")
            }
        }
    }
}

@Composable
private fun UserProfileContent(user: UserProfile, onSignOut: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = "프로필 사진",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = user.displayName ?: "사용자",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(onClick = onSignOut) {
            Text("로그아웃")
        }
    }
}
