package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppEntity
import com.example.data.AppRepository
import com.example.data.Developer
import com.example.data.Review
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    object Loading : DetailUiState
    data class Success(val app: AppEntity, val reviews: List<Review>, val developer: Developer?) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

sealed interface DevProfileUiState {
    object Loading : DevProfileUiState
    data class Success(val developer: Developer, val apps: List<AppEntity>) : DevProfileUiState
    data class Error(val message: String) : DevProfileUiState
}

class SotarkPlayViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)

    init {
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty() // загружает с сервера
        }
    }

    // Search & Category
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Все")
    val selectedCategory = _selectedCategory.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val appsList: StateFlow<List<AppEntity>> = combine(
        _searchQuery,
        _selectedCategory
    ) { query, category -> Pair(query, category) }
    .flatMapLatest { (query, category) ->
        if (query.isNotEmpty()) repository.searchApps(query)
        else if (category != "Все") repository.getAppsByCategory(category)
        else repository.allApps
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Detail
    private val _currentAppId = MutableStateFlow<Long?>(null)
    val currentAppId = _currentAppId.asStateFlow()

    private val _detailState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val appDetailState: StateFlow<DetailUiState> = _detailState.asStateFlow()

    // Developer profile
    private val _devProfileState = MutableStateFlow<DevProfileUiState>(DevProfileUiState.Loading)
    val developerProfileState: StateFlow<DevProfileUiState> = _devProfileState.asStateFlow()

    // Running app simulation
    private val _runningApp = MutableStateFlow<AppEntity?>(null)
    val runningApp = _runningApp.asStateFlow()

    private val _clickerCount = MutableStateFlow(0)
    val clickerCount = _clickerCount.asStateFlow()

    private val _weatherWindSpeed = MutableStateFlow(5.4)
    val weatherWindSpeed = _weatherWindSpeed.asStateFlow()

    private val _weatherPhaseText = MutableStateFlow("Новолуние")
    val weatherPhaseText = _weatherPhaseText.asStateFlow()

    private val _paintPaths = MutableStateFlow<List<Pair<List<androidx.compose.ui.geometry.Offset>, androidx.compose.ui.graphics.Color>>>(emptyList())
    val paintPaths = _paintPaths.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf(
            "Добро пожаловать в анонимную комнату ChatSphere!" to false,
            "Здесь нет цензуры и логов. Как дела?" to false
        )
    )
    val chatMessages = _chatMessages.asStateFlow()

    // Publish state
    private val _publishLoading = MutableStateFlow(false)
    val publishLoading = _publishLoading.asStateFlow()

    private val _publishError = MutableStateFlow<String?>(null)
    val publishError = _publishError.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            repository.refreshApps(
                category = _selectedCategory.value.takeIf { it != "Все" },
                search = query.takeIf { it.isNotEmpty() }
            )
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
        viewModelScope.launch {
            repository.refreshApps(category = category.takeIf { it != "Все" })
        }
    }

    fun loadAppDetail(appId: Long) {
        _currentAppId.value = appId
        _detailState.value = DetailUiState.Loading
        viewModelScope.launch {
            repository.getAppById(appId).collect { app ->
                if (app != null) {
                    val reviews = repository.getReviewsOnce(appId)
                    val developer = repository.getDeveloperById(app.developerId)
                    _detailState.value = DetailUiState.Success(app, reviews, developer)
                } else {
                    _detailState.value = DetailUiState.Error("Приложение не найдено")
                }
            }
        }
    }

    fun loadDeveloperProfile(devId: Long) {
        _devProfileState.value = DevProfileUiState.Loading
        viewModelScope.launch {
            val dev = repository.getDeveloperById(devId)
            if (dev != null) {
                repository.getAppsByDeveloper(devId).collect { apps ->
                    _devProfileState.value = DevProfileUiState.Success(dev, apps)
                }
            } else {
                _devProfileState.value = DevProfileUiState.Error("Разработчик не найден")
            }
        }
    }

    fun installApp(appId: Long) {
        viewModelScope.launch {
            repository.installApp(appId)
        }
    }

    fun uninstallApp(appId: Long) {
        viewModelScope.launch {
            repository.uninstallApp(appId)
        }
    }

    fun submitReview(appId: Long, name: String, email: String, text: String, rating: Int) {
        viewModelScope.launch {
            repository.addReview(appId, name, email, text, rating)
            loadAppDetail(appId) // обновить отзывы
        }
    }

    // Публикация с APK файлом
    fun publishNewApp(
        title: String,
        description: String,
        category: String,
        sizeMb: Double,
        version: String,
        developerName: String,
        developerEmail: String,
        developerBio: String,
        accentColorHex: String,
        symbolName: String,
        apkUri: Uri? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _publishLoading.value = true
            _publishError.value = null
            try {
                val appId = repository.publishApp(
                    title = title,
                    description = description,
                    category = category,
                    sizeMb = sizeMb,
                    version = version,
                    developerName = developerName,
                    developerEmail = developerEmail,
                    developerBio = developerBio,
                    iconAccentColorHex = accentColorHex,
                    iconSymbol = symbolName,
                    apkUri = apkUri
                )
                if (appId >= 0) {
                    onSuccess()
                } else {
                    val msg = "Не удалось опубликовать. Проверь подключение к серверу."
                    _publishError.value = msg
                    onError(msg)
                }
            } catch (e: Exception) {
                val msg = "Ошибка: ${e.message}"
                _publishError.value = msg
                onError(msg)
            } finally {
                _publishLoading.value = false
            }
        }
    }

    fun openRunningApp(app: AppEntity) {
        _runningApp.value = app
        _clickerCount.value = 0
        _weatherWindSpeed.value = (3..18).random() + (0..9).random() / 10.0
        val phases = listOf("Новолуние", "Растущая Луна", "Полнолуние", "Убывающая Луна")
        _weatherPhaseText.value = phases.random()
        _paintPaths.value = emptyList()
        _chatMessages.value = listOf(
            "Добро пожаловать в анонимную комнату ChatSphere!" to false,
            "Здесь нет цензуры и логов. Как дела?" to false
        )
    }

    fun closeRunningApp() { _runningApp.value = null }

    fun tapClicker() { _clickerCount.value += 1 }

    fun measureWeather() {
        _weatherWindSpeed.value = (2..25).random() + (0..9).random() / 10.0
        val warnings = listOf("Магнитный шторм: 4 балла", "Идеальная стабильность", "Солнечная вспышка класс X", "Метеоритный дождь")
        _weatherPhaseText.value = warnings.random()
    }

    fun sendChatMessage(msg: String) {
        if (msg.trim().isEmpty()) return
        val current = _chatMessages.value.toMutableList()
        current.add(msg to true)
        _chatMessages.value = current
        viewModelScope.launch {
            delay(1000)
            val botAnswers = listOf(
                "Ого, крутая мысль!", "Я зашифровал это сообщение в блокчейн.",
                "Полностью согласен!", "ChatSphere работает без цензуры по всему космосу!",
                "Ха-ха! Отличный юмор.", "Спасибо за ответ!", "Тут так круто, рад пообщаться."
            )
            val final = _chatMessages.value.toMutableList()
            final.add(botAnswers.random() to false)
            _chatMessages.value = final
        }
    }

    fun addPaintPath(path: Pair<List<androidx.compose.ui.geometry.Offset>, androidx.compose.ui.graphics.Color>) {
        val current = _paintPaths.value.toMutableList()
        current.add(path)
        _paintPaths.value = current
    }

    fun clearPaintCanvas() { _paintPaths.value = emptyList() }
}

    // ── User Profile ──────────────────────────────────────
    private val _userProfile = MutableStateFlow(com.example.data.UserProfile())
    val userProfile: StateFlow<com.example.data.UserProfile> = _userProfile.asStateFlow()

    fun loadUserProfile() {
        _userProfile.value = com.example.data.UserProfileManager.load(getApplication())
    }

    fun saveUserProfile(name: String, email: String, bio: String, avatarUri: android.net.Uri? = null) {
        val current = _userProfile.value
        val newProfile = current.copy(
            name = name,
            email = email,
            bio = bio,
            avatarUri = avatarUri?.toString() ?: current.avatarUri
        )
        com.example.data.UserProfileManager.save(getApplication(), newProfile)
        _userProfile.value = newProfile
    }

    fun isAppAuthor(app: com.example.data.AppEntity): Boolean {
        val profile = _userProfile.value
        return profile.email.isNotEmpty() && profile.email == app.developerName.let {
            // Check by developer email match
            profile.email
        } && app.developerName == profile.name
    }

    fun deleteAppIfAuthor(appId: Long, onSuccess: () -> Unit, onError: () -> Unit) {
        val app = (appDetailState.value as? DetailUiState.Success)?.app ?: run {
            onError(); return
        }
        val profile = _userProfile.value
        if (app.developerName == profile.name) {
            viewModelScope.launch {
                repository.deleteApp(appId)
                onSuccess()
            }
        } else {
            onError()
        }
    }
