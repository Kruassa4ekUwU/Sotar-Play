package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class AppRepository(private val context: Context) {

    private val api = RetrofitClient.api

    // ── локальный кэш (StateFlow) для обновлений UI без перезагрузки ──
    private val _appsCache = MutableStateFlow<List<AppEntity>>(emptyList())
    val allApps: Flow<List<AppEntity>> = _appsCache

    // Установочный статус хранится локально (сервер не знает что установлено)
    private val installStatuses = mutableMapOf<Long, Pair<String, Int>>() // id -> (status, progress)
    private val _installUpdates = MutableStateFlow(0) // триггер обновлений

    fun getAppsByCategory(category: String): Flow<List<AppEntity>> =
        _appsCache.map { list -> list.filter { it.category == category } }

    fun searchApps(query: String): Flow<List<AppEntity>> =
        _appsCache.map { list ->
            list.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
            }
        }

    fun getAppById(id: Long): Flow<AppEntity?> =
        _appsCache.map { list -> list.find { it.id == id }?.withInstallStatus() }

    fun getAppsByDeveloper(developerId: Long): Flow<List<AppEntity>> =
        _appsCache.map { list -> list.filter { it.developerId == developerId } }

    fun getReviewsForApp(appId: Long): Flow<List<Review>> {
        val flow = MutableStateFlow<List<Review>>(emptyList())
        return flow
    }

    // Обновляем кэш с сервера
    suspend fun refreshApps(category: String? = null, search: String? = null) = withContext(Dispatchers.IO) {
        try {
            val response = api.getApps(
                category = if (category == "Все") null else category,
                search = search?.takeIf { it.isNotEmpty() }
            )
            if (response.isSuccessful) {
                val apps = response.body()?.apps?.map { it.toAppEntity().withInstallStatus() } ?: emptyList()
                _appsCache.value = apps
            } else {
                Log.e("Repository", "getApps error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("Repository", "refreshApps failed", e)
        }
    }

    suspend fun getReviewsOnce(appId: Long): List<Review> = withContext(Dispatchers.IO) {
        try {
            val response = api.getReviews(appId)
            if (response.isSuccessful) {
                response.body()?.reviews?.map { it.toReview() } ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            Log.e("Repository", "getReviews failed", e)
            emptyList()
        }
    }

    suspend fun getDeveloperById(id: Long): Developer? = withContext(Dispatchers.IO) {
        try {
            val response = api.getDeveloper(id)
            if (response.isSuccessful) response.body()?.toDeveloper() else null
        } catch (e: Exception) {
            Log.e("Repository", "getDeveloper failed", e)
            null
        }
    }

    suspend fun getOrCreateDeveloper(name: String, email: String, bio: String): Long = 0L // управляется сервером

    suspend fun publishApp(
        title: String,
        description: String,
        category: String,
        sizeMb: Double,
        version: String,
        developerName: String,
        developerEmail: String,
        developerBio: String,
        iconAccentColorHex: String,
        iconSymbol: String,
        apkUri: Uri? = null
    ): Long = withContext(Dispatchers.IO) {
        fun String.toBody() = toRequestBody("text/plain".toMediaType())

        var apkPart: MultipartBody.Part? = null

        if (apkUri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(apkUri)
                val tempFile = File.createTempFile("upload_", ".apk", context.cacheDir)
                inputStream?.use { input -> tempFile.outputStream().use { input.copyTo(it) } }
                val requestBody = tempFile.asRequestBody("application/vnd.android.package-archive".toMediaType())
                apkPart = MultipartBody.Part.createFormData("apk", tempFile.name, requestBody)
            } catch (e: Exception) {
                Log.e("Repository", "Failed to prepare APK", e)
            }
        }

        val response = api.publishApp(
            title = title.toBody(),
            description = description.toBody(),
            category = category.toBody(),
            version = version.toBody(),
            developerName = developerName.toBody(),
            developerEmail = developerEmail.toBody(),
            developerBio = developerBio.toBody(),
            iconColor = iconAccentColorHex.toBody(),
            iconSymbol = iconSymbol.toBody(),
            apk = apkPart
        )

        if (response.isSuccessful) {
            refreshApps()
            response.body()?.id ?: 0L
        } else {
            Log.e("Repository", "publishApp error: ${response.code()} ${response.errorBody()?.string()}")
            -1L
        }
    }

    suspend fun addReview(appId: Long, name: String, email: String, text: String, rating: Int) = withContext(Dispatchers.IO) {
        try {
            api.addReview(appId, name, email, text, rating)
        } catch (e: Exception) {
            Log.e("Repository", "addReview failed", e)
        }
    }

    suspend fun deleteApp(appId: Long) = withContext(Dispatchers.IO) {
        try {
            api.deleteApp(appId)
            _appsCache.value = _appsCache.value.filter { it.id != appId }
        } catch (e: Exception) {
            Log.e("Repository", "deleteApp failed", e)
        }
    }

    // Реальная установка APK через системный установщик Android
    suspend fun installApp(appId: Long) = withContext(Dispatchers.IO) {
        val app = _appsCache.value.find { it.id == appId } ?: return@withContext

        if (app.apkUrl == null) {
            Log.w("Repository", "App $appId has no APK URL")
            return@withContext
        }

        try {
            // Статус: скачивание
            updateLocalStatus(appId, "DOWNLOADING", 0)

            val fullUrl = if (app.apkUrl.startsWith("http")) app.apkUrl
                          else RetrofitClient.BASE_URL.trimEnd('/') + app.apkUrl

            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val request = okhttp3.Request.Builder().url(fullUrl).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                updateLocalStatus(appId, "NOT_INSTALLED", 0)
                return@withContext
            }

            val body = response.body ?: run {
                updateLocalStatus(appId, "NOT_INSTALLED", 0)
                return@withContext
            }

            val totalBytes = body.contentLength()
            val apkFile = File(context.cacheDir, "install_${appId}.apk")

            body.byteStream().use { input ->
                apkFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                        downloaded += bytes
                        if (totalBytes > 0) {
                            val progress = (downloaded * 100 / totalBytes).toInt()
                            updateLocalStatus(appId, "DOWNLOADING", progress)
                        }
                    }
                }
            }

            updateLocalStatus(appId, "INSTALLING", 100)

            // Запуск системного установщика
            withContext(Dispatchers.Main) {
                launchSystemInstaller(context, apkFile, appId)
            }

        } catch (e: Exception) {
            Log.e("Repository", "installApp failed", e)
            updateLocalStatus(appId, "NOT_INSTALLED", 0)
        }
    }

    private fun launchSystemInstaller(context: Context, apkFile: File, appId: Long) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            updateLocalStatus(appId, "INSTALLED", 100)
        } catch (e: Exception) {
            Log.e("Repository", "System installer failed", e)
            updateLocalStatus(appId, "NOT_INSTALLED", 0)
        }
    }

    suspend fun uninstallApp(appId: Long) = withContext(Dispatchers.IO) {
        updateLocalStatus(appId, "NOT_INSTALLED", 0)
    }

    private fun updateLocalStatus(appId: Long, status: String, progress: Int) {
        installStatuses[appId] = Pair(status, progress)
        _appsCache.value = _appsCache.value.map { app ->
            if (app.id == appId) app.copy(installStatus = status, downloadProgress = progress)
            else app
        }
    }

    private fun AppEntity.withInstallStatus(): AppEntity {
        val (status, progress) = installStatuses[id] ?: Pair("NOT_INSTALLED", 0)
        return copy(installStatus = status, downloadProgress = progress)
    }

    // Совместимость со старым кодом
    suspend fun seedDatabaseIfEmpty() {
        refreshApps()
    }
}
