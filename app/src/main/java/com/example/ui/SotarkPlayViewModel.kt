package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
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
    private val repository: AppRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AppRepository(db)
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }
    }

    // Main App Browsing States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Все")
    val selectedCategory = _selectedCategory.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val appsList: StateFlow<List<AppEntity>> = combine(
        _searchQuery,
        _selectedCategory
    ) { query, category ->
        Pair(query, category)
    }.flatMapLatest { (query, category) ->
        if (query.isNotEmpty()) {
            repository.searchApps(query)
        } else if (category != "Все") {
            repository.getAppsByCategory(category)
        } else {
            repository.allApps
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // App Detail States
    private val _currentAppId = MutableStateFlow<Long?>(null)
    val currentAppId = _currentAppId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val appDetailState: StateFlow<DetailUiState> = _currentAppId.flatMapLatest { appId ->
        if (appId == null) {
            MutableStateFlow(DetailUiState.Loading)
        } else {
            combine(
                repository.getAppById(appId),
                repository.getReviewsForApp(appId)
            ) { app, reviews ->
                if (app != null) {
                    val developer = repository.getDeveloperById(app.developerId)
                    DetailUiState.Success(app, reviews, developer)
                } else {
                    DetailUiState.Error("Приложение не найдено")
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DetailUiState.Loading
    )

    // Developer Profile States
    private val _currentDeveloperId = MutableStateFlow<Long?>(null)
    val currentDeveloperId = _currentDeveloperId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val developerProfileState: StateFlow<DevProfileUiState> = _currentDeveloperId.flatMapLatest { devId ->
        if (devId == null) {
            MutableStateFlow(DevProfileUiState.Loading)
        } else {
            val devFlow = MutableStateFlow<Developer?>(null)
            viewModelScope.launch {
                devFlow.value = repository.getDeveloperById(devId)
            }
            combine(
                devFlow,
                repository.getAppsByDeveloper(devId)
            ) { developer, apps ->
                if (developer != null) {
                    DevProfileUiState.Success(developer, apps)
                } else {
                    DevProfileUiState.Error("Разработчик не найден")
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DevProfileUiState.Loading
    )

    // Simulated "Active Running App" State
    private val _runningApp = MutableStateFlow<AppEntity?>(null)
    val runningApp = _runningApp.asStateFlow()

    // Interactive States for "Running Apps"
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

    // Search & Category Setters
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun loadAppDetail(appId: Long) {
        _currentAppId.value = appId
    }

    fun loadDeveloperProfile(devId: Long) {
        _currentDeveloperId.value = devId
    }

    // Installations Actions
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

    // Submit user review
    fun submitReview(appId: Long, name: String, email: String, text: String, rating: Int) {
        viewModelScope.launch {
            repository.addReview(appId, name, email, text, rating)
        }
    }

    // Upload/Publish Dynamic Application
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
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.publishApp(
                title = title,
                description = description,
                category = category,
                sizeMb = sizeMb,
                version = version,
                developerName = developerName,
                developerEmail = developerEmail,
                developerBio = developerBio,
                iconAccentColorHex = accentColorHex,
                iconSymbol = symbolName
            )
            onSuccess()
        }
    }

    // Running App Simulations Management
    fun openRunningApp(app: AppEntity) {
        _runningApp.value = app
        // Reset interactive simulation states on launch
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

    fun closeRunningApp() {
        _runningApp.value = null
    }

    // Mini Apps Interaction Methods
    fun tapClicker() {
        _clickerCount.value += 1
    }

    fun measureWeather() {
        _weatherWindSpeed.value = (2..25).random() + (0..9).random() / 10.0
        val warnings = listOf("Магнитный шторм: 4 балла", "Идеальная стабильность", "Солнечная вспышка класс X", "Метеоритный дождь")
        _weatherPhaseText.value = warnings.random()
    }

    fun sendChatMessage(msg: String) {
        if (msg.trim().isEmpty()) return
        val current = _chatMessages.value.toMutableList()
        current.add(msg to true) // user message
        _chatMessages.value = current

        // AI/Bot Answer simulator
        viewModelScope.launch {
            delay(1000)
            val botAnswers = listOf(
                "Ого, крутая мысль!",
                "Я зашифровал это сообщение в блокчейн.",
                "Полностью согласен с тобой, друг.",
                "ChatSphere работает без цензуры по всему космосу!",
                "Ха-ха! Отличный юмор.",
                "Спасибо за ответ!",
                "Тут так круто, рад пообщаться."
            )
            val reply = botAnswers.random()
            val final = _chatMessages.value.toMutableList()
            final.add(reply to false)
            _chatMessages.value = final
        }
    }

    fun addPaintPath(path: Pair<List<androidx.compose.ui.geometry.Offset>, androidx.compose.ui.graphics.Color>) {
        val current = _paintPaths.value.toMutableList()
        current.add(path)
        _paintPaths.value = current
    }

    fun clearPaintCanvas() {
        _paintPaths.value = emptyList()
    }
}
