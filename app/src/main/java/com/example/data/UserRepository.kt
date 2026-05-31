package com.example.data

import com.example.security.CryptoHelper
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {

    // Reactive flow of raw encrypted entities
    val allUsersFlow: Flow<List<UserEntity>> = userDao.getAllUsersFlow()

    /**
     * Looks up a registered user by their 5-digit secure code.
     */
    suspend fun lookupUserBySecureCode(secureCode: String): DecryptedUser? {
        val entity = userDao.getUserBySecureCode(secureCode) ?: return null
        return DecryptedUser(
            id = entity.id,
            secureCode = entity.secureCode,
            firstName = CryptoHelper.decrypt(entity.encryptedFirstName),
            lastName = CryptoHelper.decrypt(entity.encryptedLastName),
            phone = CryptoHelper.decrypt(entity.encryptedPhone),
            timestamp = entity.timestamp
        )
    }

    /**
     * Looks up a registered user by phone number.
     * Computes the blind index hash of the requested search phone dynamically and queries Room.
     */
    suspend fun lookupUserByPhone(phone: String): DecryptedUser? {
        val hash = CryptoHelper.hashSha256(phone)
        val entity = userDao.getUserByPhoneHash(hash) ?: return null
        return DecryptedUser(
            id = entity.id,
            secureCode = entity.secureCode,
            firstName = CryptoHelper.decrypt(entity.encryptedFirstName),
            lastName = CryptoHelper.decrypt(entity.encryptedLastName),
            phone = CryptoHelper.decrypt(entity.encryptedPhone),
            timestamp = entity.timestamp
        )
    }

    /**
     * Encrypts and registers a new user into the Room database securely.
     */
    suspend fun registerUser(firstName: String, lastName: String, phone: String, secureCode: String) {
        val phoneHash = CryptoHelper.hashSha256(phone)
        val encryptedFirstName = CryptoHelper.encrypt(firstName)
        val encryptedLastName = CryptoHelper.encrypt(lastName)
        val encryptedPhone = CryptoHelper.encrypt(phone)

        val entity = UserEntity(
            secureCode = secureCode,
            phoneHash = phoneHash,
            encryptedFirstName = encryptedFirstName,
            encryptedLastName = encryptedLastName,
            encryptedPhone = encryptedPhone
        )
        userDao.insertUser(entity)
    }

    /**
     * Deletes user record by details
     */
    suspend fun deleteUser(id: Int) {
        userDao.deleteUserById(id)
    }

    /**
     * Clear all records
     */
    suspend fun clearUsers() {
        userDao.clearAllUsers()
    }
}

/**
 * Model helper class representing clean, decrypted user details (only mapped in-memory in VM/Repo scope, never persisted).
 */
data class DecryptedUser(
    val id: Int,
    val secureCode: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val timestamp: Long
)
