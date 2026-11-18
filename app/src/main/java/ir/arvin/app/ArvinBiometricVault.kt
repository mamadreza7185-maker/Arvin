package ir.arvin.app

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Opt-in device-only biometric credential vault.
 *
 * A per-operation authenticated Android Keystore AES-GCM key protects the password.
 * The password is never written to SharedPreferences in plaintext, never placed in
 * an Intent, and never put into a URL or an analytics event. Biometric success only
 * unlocks the locally encrypted password; server login is still mandatory.
 */
class ArvinBiometricVault(private val activity: FragmentActivity) {
    companion object {
        private const val KEY_ALIAS = "arvin.biometric.password.v1"
        private const val PREFS = "arvin.login.local.v1"
        private const val PREF_NAME = "saved_username"
        private const val PREF_REMEMBER = "remember_username"
        private const val PREF_BIO_USER = "biometric_username"
        private const val PREF_IV = "biometric_iv"
        private const val PREF_CIPHER = "biometric_cipher"
    }

    private val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val keystore by lazy {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    }

    fun rememberedUsername(): String {
        return if (rememberedUsernameEnabled()) prefs.getString(PREF_NAME, "").orEmpty() else ""
    }

    fun rememberedUsernameEnabled(): Boolean = prefs.getBoolean(PREF_REMEMBER, false)

    fun saveRememberUsername(username: String, enabled: Boolean) {
        prefs.edit()
            .putBoolean(PREF_REMEMBER, enabled)
            .apply {
                if (enabled) putString(PREF_NAME, username.trim())
                else remove(PREF_NAME)
            }.apply()
    }

    fun biometricAvailable(): Boolean =
        BiometricManager.from(activity)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    fun hasBiometricCredentials(): Boolean {
        return !prefs.getString(PREF_BIO_USER, "").isNullOrBlank() &&
            !prefs.getString(PREF_IV, "").isNullOrBlank() &&
            !prefs.getString(PREF_CIPHER, "").isNullOrBlank()
    }

    fun biometricUsername(): String = prefs.getString(PREF_BIO_USER, "").orEmpty()

    fun forgetAll() {
        clearBiometricOnly()
        prefs.edit().remove(PREF_NAME).remove(PREF_REMEMBER).apply()
    }

    fun clearBiometricOnly() {
        prefs.edit().remove(PREF_BIO_USER).remove(PREF_IV).remove(PREF_CIPHER).apply()
        try { if (keystore.containsAlias(KEY_ALIAS)) keystore.deleteEntry(KEY_ALIAS) }
        catch (_: Exception) { /* A stale key will be replaced at next setup. */ }
    }

    private fun key(): SecretKey {
        val existing = keystore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
        )
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0, KeyProperties.AUTH_BIOMETRIC_STRONG
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setInvalidatedByBiometricEnrollment(true)
        }
        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    private fun prompt(
        title: String,
        cipher: Cipher,
        complete: (Cipher) -> Unit,
        error: (String) -> Unit
    ) {
        val biometricPrompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    val authorizedCipher = result.cryptoObject?.cipher
                    if (authorizedCipher != null) complete(authorizedCipher)
                    else error("تأیید اثر انگشت انجام شد، اما رمزگذاری امن در دسترس نیست.")
                }

                override fun onAuthenticationError(
                    errorCode: Int, errString: CharSequence
                ) {
                    error(
                        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_CANCELED
                        ) "ورود با اثر انگشت لغو شد." else
                            "اثر انگشت تأیید نشد: $errString"
                    )
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle("سامانه امن پخش آروین")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("ورود با رمز عبور")
            .build()
        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    fun enable(
        username: String,
        password: String,
        done: (Boolean, String?) -> Unit
    ) {
        if (!biometricAvailable()) {
            done(false, "برای ورود با اثر انگشت، ابتدا آن را در تنظیمات امنیتی گوشی فعال کنید.")
            return
        }
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            prompt(
                "فعال‌سازی ورود با اثر انگشت",
                cipher,
                complete = { authorizedCipher ->
                    try {
                        val encoded = authorizedCipher.doFinal(
                            password.toByteArray(StandardCharsets.UTF_8)
                        )
                        prefs.edit()
                            .putString(PREF_BIO_USER, username.trim())
                            .putString(PREF_IV,
                                Base64.encodeToString(authorizedCipher.iv, Base64.NO_WRAP))
                            .putString(PREF_CIPHER,
                                Base64.encodeToString(encoded, Base64.NO_WRAP))
                            .apply()
                        done(true, null)
                    } catch (_: Exception) {
                        done(false, "ذخیره امن رمز عبور انجام نشد.")
                    }
                },
                error = { done(false, it) }
            )
        } catch (_: Exception) {
            done(false, "فعال‌سازی اثر انگشت ممکن نیست؛ ورود با رمز عبور همچنان فعال است.")
        }
    }

    fun login(
        onUnlocked: (String, String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!hasBiometricCredentials()) {
            onFailure("ابتدا با رمز عبور وارد شوید و گزینه ورود با اثر انگشت را فعال کنید.")
            return
        }
        if (!biometricAvailable()) {
            onFailure("اثر انگشت در تنظیمات گوشی فعال نیست. با رمز عبور وارد شوید.")
            return
        }
        try {
            val iv = Base64.decode(prefs.getString(PREF_IV, ""), Base64.NO_WRAP)
            val encrypted = Base64.decode(prefs.getString(PREF_CIPHER, ""), Base64.NO_WRAP)
            val username = biometricUsername()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            prompt(
                "ورود با اثر انگشت",
                cipher,
                complete = { authorizedCipher ->
                    try {
                        val secret = authorizedCipher.doFinal(encrypted)
                        onUnlocked(username, String(secret, StandardCharsets.UTF_8))
                        secret.fill(0)
                    } catch (_: Exception) {
                        clearBiometricOnly()
                        onFailure("اطلاعات ورود امن نامعتبر شده است؛ دوباره با رمز عبور وارد شوید.")
                    }
                },
                error = onFailure
            )
        } catch (_: Exception) {
            clearBiometricOnly()
            onFailure("اثر انگشت گوشی تغییر کرده است؛ برای فعال‌سازی مجدد با رمز عبور وارد شوید.")
        }
    }
}
