package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM secure_users ORDER BY timestamp DESC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM secure_users ORDER BY timestamp DESC")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT * FROM secure_users WHERE phoneHash = :phoneHash LIMIT 1")
    suspend fun getUserByPhoneHash(phoneHash: String): UserEntity?

    @Query("SELECT * FROM secure_users WHERE secureCode = :secureCode LIMIT 1")
    suspend fun getUserBySecureCode(secureCode: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM secure_users WHERE id = :id")
    suspend fun deleteUserById(id: Int)

    @Query("DELETE FROM secure_users")
    suspend fun clearAllUsers()
}
