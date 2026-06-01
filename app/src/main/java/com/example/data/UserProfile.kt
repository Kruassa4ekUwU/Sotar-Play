package com.example.data

import android.content.Context
import android.net.Uri
import androidx.core.content.edit

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val bio: String = "",
    val avatarUri: String? = null // local URI or null = default VOC avatar
)

object UserProfileManager {

    private const val PREFS_NAME = "sotar_user_profile"
    private const val KEY_NAME = "name"
    private const val KEY_EMAIL = "email"
    private const val KEY_BIO = "bio"
    private const val KEY_AVATAR_URI = "avatar_uri"

    fun save(context: Context, profile: UserProfile) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_NAME, profile.name)
            putString(KEY_EMAIL, profile.email)
            putString(KEY_BIO, profile.bio)
            putString(KEY_AVATAR_URI, profile.avatarUri)
        }
    }

    fun load(context: Context): UserProfile {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return UserProfile(
            name = prefs.getString(KEY_NAME, "") ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            bio = prefs.getString(KEY_BIO, "") ?: "",
            avatarUri = prefs.getString(KEY_AVATAR_URI, null)
        )
    }

    fun isLoggedIn(context: Context): Boolean {
        val profile = load(context)
        return profile.name.isNotEmpty() && profile.email.isNotEmpty()
    }
}
