package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps ORDER BY id DESC")
    fun getAllApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE category = :category ORDER BY id DESC")
    fun getAppsByCategory(category: String): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchApps(query: String): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE id = :id LIMIT 1")
    fun getAppById(id: Long): Flow<AppEntity?>

    @Query("SELECT * FROM apps WHERE id = :id LIMIT 1")
    suspend fun getAppByIdSuspend(id: Long): AppEntity?

    @Query("SELECT * FROM apps WHERE developerId = :developerId ORDER BY id DESC")
    fun getAppsByDeveloper(developerId: Long): Flow<List<AppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppEntity): Long

    @Update
    suspend fun updateApp(app: AppEntity)

    @Query("UPDATE apps SET installStatus = :status, downloadProgress = :progress WHERE id = :appId")
    suspend fun updateInstallStatus(appId: Long, status: String, progress: Int)

    @Query("UPDATE apps SET downloadCount = downloadCount + 1 WHERE id = :appId")
    suspend fun incrementDownloadCount(appId: Long)

    @Query("UPDATE apps SET ratingSum = ratingSum + :rating, ratingCount = ratingCount + 1 WHERE id = :appId")
    suspend fun addRatingToApp(appId: Long, rating: Double)
}

@Dao
interface DeveloperDao {
    @Query("SELECT * FROM developers WHERE id = :id LIMIT 1")
    suspend fun getDeveloperById(id: Long): Developer?

    @Query("SELECT * FROM developers WHERE email = :email LIMIT 1")
    suspend fun getDeveloperByEmail(email: String): Developer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeveloper(developer: Developer): Long
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE appId = :appId ORDER BY timestamp DESC")
    fun getReviewsForApp(appId: Long): Flow<List<Review>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review): Long
}
