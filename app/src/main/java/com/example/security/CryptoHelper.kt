package com.example.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {

    private const val KEY_ALGORITHM = "AES"
    private const val CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val IV_SIZE_BYTES = 16

    // Secure key representation derived from a stable, robust secret key
    // In production, this can be combined with Android KeyStore or user-specific PINs.
    private val secretKeySpec: SecretKeySpec by lazy {
        val masterSecret = "SecureAuthAndroidKimlikDogrulamaSistemi2026MasterKeyPhrase!"
        val md = MessageDigest.getInstance("SHA-256")
        val keyBytes = md.digest(masterSecret.toByteArray(Charsets.UTF_8))
        SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }

    /**
     * Encrypts a plain-text string using AES-256-CBC.
     * Generates a random 16-byte IV for every encryption action to ensure randomized ciphertexts.
     * Prepends the IV to the ciphertext, and returns a Base64-encoded URL-safe string.
     */
    fun encrypt(plainText: String?): String {
        if (plainText.isNullOrEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            
            // Generate a random IV
            val iv = ByteArray(IV_SIZE_BYTES)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            // Combine IV and cipher bytes
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            
            Base64.encodeToString(combined, Base64.NO_WRAP or Base64.URL_SAFE)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Decrypts a Base64 string that was encrypted using [encrypt].
     * Extracts the IV from the prepended portion and decrypts the trailing ciphertext.
     */
    fun decrypt(encryptedBase64: String?): String {
        if (encryptedBase64.isNullOrEmpty()) return ""
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP or Base64.URL_SAFE)
            if (combined.size < IV_SIZE_BYTES) return ""
            
            // Extract IV
            val iv = ByteArray(IV_SIZE_BYTES)
            System.arraycopy(combined, 0, iv, 0, IV_SIZE_BYTES)
            val ivSpec = IvParameterSpec(iv)
            
            // Extract encrypted bytes
            val encryptedBytesSize = combined.size - IV_SIZE_BYTES
            val encryptedBytes = ByteArray(encryptedBytesSize)
            System.arraycopy(combined, IV_SIZE_BYTES, encryptedBytes, 0, encryptedBytesSize)
            
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            "[Decoding Error / Bozuk Veri]"
        }
    }

    /**
     * Computes a SHA-256 hash of a string (useful for blind indexing on phone numbers).
     * This allows lookup functionality without storing the raw phone number search string.
     */
    fun hashSha256(input: String): String {
        return try {
            // Normalize spaces/punctuation to avoid mismatch in searching
            val normalized = input.replace(Regex("[^0-9]"), "")
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(normalized.toByteArray(Charsets.UTF_8))
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
