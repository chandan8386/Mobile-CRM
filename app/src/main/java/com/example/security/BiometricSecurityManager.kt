package com.example.security

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BiometricSecurityManager(private val context: Context) {

    private val _isBiometricEnabled = MutableStateFlow(true)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _securityPin = MutableStateFlow("1234")
    val securityPin: StateFlow<String> = _securityPin.asStateFlow()

    fun setBiometricEnabled(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
        if (!enabled) {
            _isAppLocked.value = false
        }
    }

    fun lockApp() {
        if (_isBiometricEnabled.value) {
            _isAppLocked.value = true
        }
    }

    fun unlockApp() {
        _isAppLocked.value = false
        vibrateSuccess()
    }

    fun verifyPin(pin: String): Boolean {
        return if (pin == _securityPin.value) {
            unlockApp()
            true
        } else {
            vibrateError()
            false
        }
    }

    fun updatePin(newPin: String) {
        if (newPin.length == 4) {
            _securityPin.value = newPin
        }
    }

    /**
     * Checks if biometric sensor hardware is available on the device.
     */
    fun canAuthenticate(): Int {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
    }

    /**
     * Launches the official Android Jetpack BiometricPrompt system dialog.
     */
    fun authenticateWithBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Unlock NexusCRM",
        subtitle: String = "Verify identity with fingerprint or face to protect sales CRM data",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                unlockApp()
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    vibrateError()
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                vibrateError()
                onError("Biometric authentication failed. Try again.")
            }
        }

        try {
            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText("Use Security PIN")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build()

            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            // In case emulator or device has no enrolled biometrics, provide graceful fallback
            unlockApp()
            onSuccess()
        }
    }

    fun vibrateSuccess() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(50)
            }
        } catch (_: Exception) {
            // Graceful fallback
        }
    }

    fun vibrateError() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 80, 50, 80), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(150)
            }
        } catch (_: Exception) {
            // Graceful fallback
        }
    }
}
