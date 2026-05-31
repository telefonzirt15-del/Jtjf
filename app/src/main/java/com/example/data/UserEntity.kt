package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "secure_users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val secureCode: String,        // 5-digit unique access code (e.g. "53891")
    val phoneHash: String,         // Blind index of phone number (SHA-256)
    val encryptedFirstName: String, // Encrypted first name (AES)
    val encryptedLastName: String,  // Encrypted last name (AES)
    val encryptedPhone: String,     // Encrypted phone number (AES)
    val timestamp: Long = System.currentTimeMillis()
)
