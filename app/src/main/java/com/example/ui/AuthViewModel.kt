package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DecryptedUser
import com.example.data.UserEntity
import com.example.data.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class AuthScreenState {
    REGISTRATION,
    SMS_VERIFICATION,
    DASHBOARD
}

sealed interface LookupState {
    object Idle : LookupState
    object Loading : LookupState
    data class Success(val user: DecryptedUser) : LookupState
    object NotFound : LookupState
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = UserRepository(db.userDao())

    // UI state streams
    val allEntities: StateFlow<List<UserEntity>> = repository.allUsersFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current Auth State
    var currentScreenState by mutableStateOf(AuthScreenState.REGISTRATION)
        private set

    // Registration inputs
    var firstNameInput by mutableStateOf("")
    var lastNameInput by mutableStateOf("")
    var phoneInput by mutableStateOf("")

    // Verification state
    var generatedOtp by mutableStateOf("")
        private set
    var otpInput by mutableStateOf("")
    var timerCountdown by mutableStateOf(60)
        private set
    private var timerJob: Job? = null

    // Pending details to commit after SMS verification succeeds
    private var pendingFirstName = ""
    private var pendingLastName = ""
    private var pendingPhone = ""
    var pendingSecureCode by mutableStateOf("")
        private set

    // Last registered code to show permanently on dashboard
    var lastRegisteredCode by mutableStateOf("")
        private set

    // Search and lookup state (using the 5-digit secure code)
    var searchPhoneInput by mutableStateOf("")
    var lookupState by mutableStateOf<LookupState>(LookupState.Idle)
        private set

    // Global messaging
    var systemToastMessage by mutableStateOf<String?>(null)
    var inputErrorMessage by mutableStateOf<String?>(null)

    // Inspection decrypt mode in the GUI
    var showPlainInDatabaseView by mutableStateOf(false)

    init {
        // Pre-populate with realistic secure sample accounts if DB is empty
        viewModelScope.launch {
            val users = db.userDao().getAllUsers()
            if (users.isEmpty()) {
                repository.registerUser("Ahmet", "Yılmaz", "5551234567", "12345")
                repository.registerUser("Zeynep", "Kaya", "5059876543", "54321")
            }
        }
    }

    /**
     * Starts the registration flow. Triggers in-memory state, generates the OTP code,
     * and triggers a mock system push notification.
     */
    fun startRegistration() {
        inputErrorMessage = null
        val normalizedPhone = phoneInput.replace(Regex("[^0-9]"), "")

        if (firstNameInput.isBlank()) {
            inputErrorMessage = "Lütfen adınızı giriniz."
            return
        }
        if (lastNameInput.isBlank()) {
            inputErrorMessage = "Lütfen soyadınızı giriniz."
            return
        }
        if (normalizedPhone.length < 10) {
            inputErrorMessage = "Geçersiz telefon numarası. En az 10 hane olmalıdır (Örn: 5551234567)."
            return
        }

        // Standardize formats
        pendingFirstName = firstNameInput.trim()
        pendingLastName = lastNameInput.trim().uppercase()
        pendingPhone = normalizedPhone
        pendingSecureCode = (10000 + Random.nextInt(90000)).toString() // Generate unique 5-digit access code (e.g. 74812)

        // Generate dynamic 6-digit OTP
        val otp = (100000 + Random.nextInt(900000)).toString()
        generatedOtp = otp
        otpInput = ""

        // Simulate SMS Broadcast notification
        triggerSystemPushToast("📞 [SMS ALINDI] Doğrulama Kodu: $otp")

        // Switch to Verification stage and tick timer
        currentScreenState = AuthScreenState.SMS_VERIFICATION
        startTimer()
    }

    /**
     * Verifies user OTP input. On success, persists encrypted data, and logs them in.
     */
    fun verifyOtp() {
        inputErrorMessage = null
        if (otpInput.trim() == generatedOtp) {
            viewModelScope.launch {
                try {
                    repository.registerUser(pendingFirstName, pendingLastName, pendingPhone, pendingSecureCode)
                    lastRegisteredCode = pendingSecureCode
                    currentScreenState = AuthScreenState.DASHBOARD
                    triggerSystemPushToast("🎉 Doğrulama Başarılı! E-Devlet Sorgu Kodunuz: $pendingSecureCode")
                    // Reset registration state
                    firstNameInput = ""
                    lastNameInput = ""
                    phoneInput = ""
                } catch (e: Exception) {
                    inputErrorMessage = "Veritabanı hatası oluştu: ${e.localizedMessage}"
                }
            }
        } else {
            inputErrorMessage = "Hatalı doğrulama kodu. Lütfen tekrar deneyin."
        }
    }

    /**
     * Resends SMS OTP code.
     */
    fun resendOtp() {
        val otp = (100000 + Random.nextInt(900000)).toString()
        generatedOtp = otp
        otpInput = ""
        triggerSystemPushToast("📞 [SMS ALINDI (YENİ)] Doğrulama Kodu: $otp")
        startTimer()
    }

    /**
     * Searches the local encrypted database using the 5-digit secure code.
     */
    fun executeLookup() {
        val trimmed = searchPhoneInput.trim()
        if (trimmed.isEmpty()) {
            lookupState = LookupState.Idle
            return
        }

        lookupState = LookupState.Loading
        viewModelScope.launch {
            delay(500) // Beautiful dynamic loading delay
            val result = repository.lookupUserBySecureCode(trimmed)
            if (result != null) {
                lookupState = LookupState.Success(result)
            } else {
                lookupState = LookupState.NotFound
            }
        }
    }

    /**
     * Clears results of lookup
     */
    fun clearLookup() {
        searchPhoneInput = ""
        lookupState = LookupState.Idle
    }

    /**
     * Erases SQLite entries to test first-launch empty state
     */
    fun clearDatabase() {
        viewModelScope.launch {
            repository.clearUsers()
            triggerSystemPushToast("🗑️ Veritabanı temizlendi.")
        }
    }

    /**
     * Navigates directly to the dashboard query screen
     */
    fun navigateToDashboard() {
        currentScreenState = AuthScreenState.DASHBOARD
    }

    /**
     * Deletes a user record by its ID.
     */
    fun deleteUser(id: Int) {
        viewModelScope.launch {
            repository.deleteUser(id)
        }
    }

    /**
     * Navigates back to the registration screen
     */
    fun resetToRegistration() {
        currentScreenState = AuthScreenState.REGISTRATION
        timerJob?.cancel()
        inputErrorMessage = null
        otpInput = ""
    }

    private fun startTimer() {
        timerCountdown = 60
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (timerCountdown > 0) {
                delay(1000)
                timerCountdown--
            }
        }
    }

    private fun triggerSystemPushToast(msg: String) {
        systemToastMessage = msg
    }

    fun dismissSystemToast() {
        systemToastMessage = null
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
