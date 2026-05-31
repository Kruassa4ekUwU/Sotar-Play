package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "developers")
data class Developer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val bio: String,
    val avatarColorHex: String = "#FF7214"
) : Serializable

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val category: String, // e.g. "Игры", "Софт", "Инструменты", "Соцсети", "Развлечения"
    val sizeMb: Double,
    val version: String,
    val developerId: Long,
    val developerName: String,
    val iconAccentColorHex: String = "#FF7214",
    val iconSymbol: String = "sports_esports", // gamepad, chat, etc.
    val downloadCount: Int = 0,
    val installStatus: String = "NOT_INSTALLED", // "NOT_INSTALLED", "DOWNLOADING", "INSTALLING", "INSTALLED"
    val downloadProgress: Int = 0,
    val ratingSum: Double = 0.0,
    val ratingCount: Int = 0
) : Serializable {
    fun getAverageRating(): Double {
        return if (ratingCount > 0) ratingSum / ratingCount else 0.0
    }
}

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appId: Long,
    val reviewerName: String,
    val reviewerEmail: String,
    val text: String,
    val rating: Int, // 1 to 5
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
