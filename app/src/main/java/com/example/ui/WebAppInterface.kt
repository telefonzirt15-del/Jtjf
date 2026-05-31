package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.example.data.AppDatabase
import com.example.data.UserRepository
import com.example.security.CryptoHelper
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

class WebAppInterface(
    private val context: Context,
    private val viewModel: AuthViewModel
) {
    private val db = AppDatabase.getDatabase(context)
    private val repository = UserRepository(db.userDao())

    @JavascriptInterface
    fun registerUser(firstName: String, lastName: String, phone: String, secureCode: String) {
        runBlocking {
            try {
                repository.registerUser(firstName, lastName, phone, secureCode)
                viewModel.lastRegisteredCode = secureCode
                // Update dynamic viewmodel toast
                viewModel.currentScreenState = AuthScreenState.DASHBOARD
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JavascriptInterface
    fun lookupUserBySecureCode(secureCode: String): String? {
        return runBlocking {
            try {
                val user = repository.lookupUserBySecureCode(secureCode)
                if (user != null) {
                    val json = JSONObject().apply {
                        put("id", user.id)
                        put("secureCode", user.secureCode)
                        put("firstName", user.firstName)
                        put("lastName", user.lastName)
                        put("phone", user.phone)
                        put("timestamp", user.timestamp)
                    }
                    json.toString()
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    @JavascriptInterface
    fun getAllUsersJson(): String {
        return runBlocking {
            try {
                val dbUsers = db.userDao().getAllUsers()
                val jsonArray = JSONArray()
                for (entity in dbUsers) {
                    val decryptedFirst = CryptoHelper.decrypt(entity.encryptedFirstName)
                    val decryptedLast = CryptoHelper.decrypt(entity.encryptedLastName)
                    val decryptedPhone = CryptoHelper.decrypt(entity.encryptedPhone)

                    val jsonObject = JSONObject().apply {
                        put("id", entity.id)
                        put("secureCode", entity.secureCode)
                        put("phoneHash", entity.phoneHash)
                        put("encryptedFirstName", entity.encryptedFirstName)
                        put("encryptedLastName", entity.encryptedLastName)
                        put("encryptedPhone", entity.encryptedPhone)
                        put("decryptedFirst", decryptedFirst)
                        put("decryptedLast", decryptedLast)
                        put("decryptedPhone", decryptedPhone)
                        put("timestamp", entity.timestamp)
                    }
                    jsonArray.put(jsonObject)
                }
                jsonArray.toString()
            } catch (e: Exception) {
                e.printStackTrace()
                "[]"
            }
        }
    }

    @JavascriptInterface
    fun deleteUser(id: Int) {
        runBlocking {
            try {
                repository.deleteUser(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JavascriptInterface
    fun clearDatabase() {
        runBlocking {
            try {
                repository.clearUsers()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JavascriptInterface
    fun showToastMessage(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
    }

    @JavascriptInterface
    fun dismissToast() {
        viewModel.dismissSystemToast()
    }

    @JavascriptInterface
    fun copyToClipboard(text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Sorgu Kodu", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Kod panoya kopyalandı!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
