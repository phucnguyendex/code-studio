package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.viewmodel.EditorViewModel
import kotlinx.coroutines.delay

@Composable
fun LockScreen(viewModel: EditorViewModel) {
    val isViet = viewModel.isVietnamese
    val themeColors = ThemeRegistry.getTheme(viewModel.editorTheme)
    val context = LocalContext.current

    var enteredPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    
    var biometricAvailable by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val biometricManager = BiometricManager.from(context)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> biometricAvailable = true
            else -> biometricAvailable = false
        }
    }
    
    fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            context as FragmentActivity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.unlockApp()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (isViet) "Mở khóa Code Studio" else "Unlock Code Studio")
            .setSubtitle(if (isViet) "Sử dụng sinh trắc học để mở khóa" else "Use your biometric credential to unlock")
            .setNegativeButtonText(if (isViet) "Sử dụng PIN" else "Use PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
    
    LaunchedEffect(biometricAvailable) {
        if (biometricAvailable) {
            showBiometricPrompt()
        }
    }

    LaunchedEffect(enteredPin) {
        if (enteredPin.length == viewModel.appLockPin.length) {
            if (enteredPin == viewModel.appLockPin) {
                viewModel.unlockApp()
            } else {
                errorMsg = if (isViet) "Mã PIN không đúng!" else "Incorrect PIN!"
                delay(1000)
                enteredPin = ""
                errorMsg = null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = themeColors.keyword,
                modifier = Modifier.size(64.dp).padding(bottom = 16.dp)
            )

            Text(
                text = if (isViet) "Nhập Mã PIN Để Mở Khóa" else "Enter PIN to Unlock",
                style = MaterialTheme.typography.titleLarge,
                color = themeColors.text,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = enteredPin,
                onValueChange = { 
                    if (it.length <= viewModel.appLockPin.length && it.all { char -> char.isDigit() }) {
                        enteredPin = it 
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                textStyle = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 24.sp, letterSpacing = 8.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = themeColors.text,
                    unfocusedTextColor = themeColors.text,
                    focusedBorderColor = themeColors.keyword,
                    unfocusedBorderColor = Color.DarkGray
                ),
                modifier = Modifier.fillMaxWidth(0.6f)
            )

            errorMsg?.let {
                Text(
                    text = it,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 16.dp),
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (biometricAvailable) {
                Spacer(modifier = Modifier.height(32.dp))
                IconButton(
                    onClick = { showBiometricPrompt() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric Unlock",
                        tint = themeColors.keyword,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Text(
                    text = if (isViet) "Sinh trắc học" else "Biometrics",
                    color = themeColors.text.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
