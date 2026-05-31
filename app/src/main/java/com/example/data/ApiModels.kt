package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─── Response models (from server) ───────────────────────

@JsonClass(generateAdapter = true)
data class AppResponse(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val category: String = "",
    @Json(name = "size_mb") val sizeMb: Double = 0.0,
    val version: String = "1.0.0",
    @Json(name = "developer_id") val developerId: Long = 0,
    @Json(name = "developer_name") val developerName: String = "",
    @Json(name = "icon_color") val iconColor: String = "#FF7214",
    @Json(name = "icon_symbol") val iconSymbol: String = "android",
    @Json(name = "download_count") val downloadCount: Int = 0,
    val rating: Double = 0.0,
    @Json(name = "rating_count") val ratingCount: Int = 0,
    @Json(name = "apk_url") val apkUrl: String? = null,
    @Json(name = "created_at") val createdAt: String = ""
) {
    fun toAppEntity(): AppEntity = AppEntity(
        id = id,
        title = title,
        description = description,
        category = category,
        sizeMb = sizeMb,
        version = version,
        developerId = developerId,
        developerName = developerName,
        iconAccentColorHex = iconColor,
        iconSymbol = iconSymbol,
        downloadCount = downloadCount,
        installStatus = "NOT_INSTALLED",
        downloadProgress = 0,
        ratingSum = rating * ratingCount,
        ratingCount = ratingCount,
        apkUrl = apkUrl
    )
}

@JsonClass(generateAdapter = true)
data class AppsListResponse(
    val apps: List<AppResponse> = emptyList(),
    val total: Int = 0
)

@JsonClass(generateAdapter = true)
data class ReviewResponse(
    val id: Long = 0,
    @Json(name = "app_id") val appId: Long = 0,
    @Json(name = "reviewer_name") val reviewerName: String = "",
    @Json(name = "reviewer_email") val reviewerEmail: String = "",
    val text: String = "",
    val rating: Int = 5,
    @Json(name = "created_at") val createdAt: String = ""
) {
    fun toReview(): Review = Review(
        id = id,
        appId = appId,
        reviewerName = reviewerName,
        reviewerEmail = reviewerEmail,
        text = text,
        rating = rating,
        timestamp = System.currentTimeMillis()
    )
}

@JsonClass(generateAdapter = true)
data class ReviewsListResponse(
    val reviews: List<ReviewResponse> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DeveloperResponse(
    val id: Long = 0,
    val name: String = "",
    val email: String = "",
    val bio: String = "",
    @Json(name = "avatar_color") val avatarColor: String = "#FF7214",
    val apps: List<AppResponse> = emptyList()
) {
    fun toDeveloper(): Developer = Developer(
        id = id,
        name = name,
        email = email,
        bio = bio,
        avatarColorHex = avatarColor
    )
}

@JsonClass(generateAdapter = true)
data class CategoriesResponse(
    val categories: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SuccessResponse(
    val success: Boolean = false
)
