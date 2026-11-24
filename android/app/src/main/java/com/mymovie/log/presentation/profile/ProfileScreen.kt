package com.mymovie.log.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.platform.testTag
import com.mymovie.log.presentation.ui.theme.PopcornColors
import com.mymovie.log.presentation.ui.theme.popcornButtonColors
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mymovie.log.R
import com.mymovie.log.domain.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit = {}
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val emailAuthUiState by viewModel.emailAuthUiState.collectAsStateWithLifecycle()
    val recordCounts by viewModel.recordCounts.collectAsStateWithLifecycle()

    var showLoginSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // logged out at first composition? avoids navigating when already logged in on start
    var wasLoggedOut by remember { mutableStateOf(currentUser == null) }

    // login transition (null -> non-null), email and Google
    LaunchedEffect(currentUser) {
        if (wasLoggedOut && currentUser != null) {
            onLoginSuccess()
        }
        wasLoggedOut = currentUser == null
    }

    // close sheet on sign-in only, stay open while waiting for email verification
    LaunchedEffect(emailAuthUiState) {
        if (emailAuthUiState is EmailAuthUiState.Success) {
            sheetState.hide()
            showLoginSheet = false
            viewModel.resetEmailAuthState()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.nav_profile), fontWeight = FontWeight.Bold) })

        if (currentUser == null) {
            LoginPromptContent(
                onEmailLogin = { showLoginSheet = true },
                onGoogleLogin = viewModel::signInWithGoogle
            )
        } else {
            UserProfileContent(
                user = currentUser!!,
                recordCounts = recordCounts,
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
    val errorMessage = (emailAuthUiState as? EmailAuthUiState.Error)?.message?.asString()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        // verification prompt after sign-up
        if (isVerificationRequired) {
            EmailVerificationContent(
                onConfirmed = {
                    // verified -> sign-in mode
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
                text = if (isSignUpMode) stringResource(R.string.profile_sign_up) else stringResource(R.string.profile_sign_in_email),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.profile_email)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.profile_password)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) stringResource(R.string.profile_hide_password) else stringResource(R.string.profile_show_password)
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
                colors = popcornButtonColors(),
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
                    Text(if (isSignUpMode) stringResource(R.string.profile_sign_up) else stringResource(R.string.profile_sign_in))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    isSignUpMode = !isSignUpMode
                    onResetState()  // clear error on mode switch
                },
                enabled = !isLoading
            ) {
                Text(
                    text = if (isSignUpMode) stringResource(R.string.profile_have_account) else stringResource(R.string.profile_no_account),
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
            text = stringResource(R.string.profile_verify_email_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.profile_verify_email_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            colors = popcornButtonColors(),
            onClick = onConfirmed,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.profile_verified_sign_in))
        }
    }
}

@Composable
private fun LoginPromptContent(onEmailLogin: () -> Unit, onGoogleLogin: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.profile_sign_in_prompt), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                colors = popcornButtonColors(),
                onClick = onEmailLogin,
                modifier = Modifier.fillMaxWidth(0.7f).widthIn(max = ActionButtonMaxWidth)
            ) {
                Text(stringResource(R.string.profile_sign_in_email))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onGoogleLogin,
                modifier = Modifier.fillMaxWidth(0.7f).widthIn(max = ActionButtonMaxWidth)
            ) {
                Text(stringResource(R.string.profile_sign_in_google))
            }
        }
    }
}

@Composable
internal fun UserProfileContent(user: UserProfile, recordCounts: ProfileRecordCounts, onSignOut: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = stringResource(R.string.profile_photo_description),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = user.displayName ?: stringResource(R.string.profile_default_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = ActionButtonMaxWidth)
                .testTag(ProfileTestTags.RecordCounts),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CountCard(
                label = stringResource(R.string.profile_watched_count),
                value = stringResource(R.string.common_movie_count, recordCounts.watched),
                modifier = Modifier.weight(1f)
            )
            CountCard(
                label = stringResource(R.string.profile_wishlist_count),
                value = stringResource(R.string.common_movie_count, recordCounts.wishlist),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(onClick = onSignOut) {
            Text(stringResource(R.string.profile_sign_out))
        }
    }
}

@Composable
private fun CountCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(PopcornColors.ButterLight)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = PopcornColors.InkMuted
        )
        Text(text = value, style = MaterialTheme.typography.headlineSmall)
    }
}

internal object ProfileTestTags {
    const val RecordCounts = "profile_record_counts"
}

// phone-sized buttons on wide windows
private val ActionButtonMaxWidth = 360.dp
