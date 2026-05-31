package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.AppEntity
import com.example.data.Developer
import com.example.data.Review
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SotarkPlayApp(
    viewModel: SotarkPlayViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val navController = rememberNavController()
    val runningApp by viewModel.runningApp.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.fillMaxSize()
            ) {
                // HOME CATALOG SCREEN
                composable("home") {
                    HomeScreen(
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = onToggleTheme,
                        onNavigateToDetail = { appId ->
                            viewModel.loadAppDetail(appId)
                            navController.navigate("detail/$appId")
                        },
                        onNavigateToPublish = {
                            navController.navigate("publish")
                        }
                    )
                }

                // APP DETAIL SCREEN
                composable(
                    route = "detail/{appId}",
                    arguments = listOf(navArgument("appId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val appId = backStackEntry.arguments?.getLong("appId") ?: 0L
                    AppDetailScreen(
                        appId = appId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToDev = { devId ->
                            viewModel.loadDeveloperProfile(devId)
                            navController.navigate("developer/$devId")
                        }
                    )
                }

                // DEVELOPER PROFILE SCREEN
                composable(
                    route = "developer/{devId}",
                    arguments = listOf(navArgument("devId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val devId = backStackEntry.arguments?.getLong("devId") ?: 0L
                    DeveloperProfileScreen(
                        devId = devId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToApp = { appId ->
                            viewModel.loadAppDetail(appId)
                            navController.navigate("detail/$appId")
                        }
                    )
                }

                // PUBLISH APP SCREEN
                composable("publish") {
                    PublishAppScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // SIMULATED RUNNING APP OVERLAY FullScreen
            runningApp?.let { app ->
                SimulatedAppRunnerView(
                    app = app,
                    viewModel = viewModel,
                    onClose = { viewModel.closeRunningApp() }
                )
            }
        }
    }
}

// APP DYNAMIC LAUNCHER ICON COMPOSABLE
@Composable
fun AppIcon(
    symbolName: String,
    accentColorHex: String,
    sizeDp: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    val color = remember(accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(accentColorHex))
        } catch (_: Exception) {
            Color(0xFFFF7214)
        }
    }
    val icon = when (symbolName) {
        "sports_esports" -> Icons.Filled.SportsEsports
        "cloud" -> Icons.Filled.Cloud
        "forum" -> Icons.Filled.Forum
        "brush" -> Icons.Filled.Brush
        "directions_car" -> Icons.Filled.DirectionsCar
        "mood" -> Icons.Filled.Mood
        else -> Icons.Filled.Apps
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(sizeDp * 0.25f))
            .background(
                Brush.linearGradient(
                    colors = listOf(color, color.copy(alpha = 0.6f))
                )
            )
            .padding(sizeDp * 0.18f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// 1. HOME SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: SotarkPlayViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToPublish: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val appsList by viewModel.appsList.collectAsStateWithLifecycle()

    val categories = listOf("Все", "Игры", "Софт", "Инструменты", "Соцсети", "Развлечения")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP APP BAR
        CenterAlignedTopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sotark Play",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = "Сменить тему",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // SEARCH BAR
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("search_input"),
            placeholder = { Text("Поиск приложений, игр, авторов...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Поиск") },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Очистить")
                    }
                }
            } else null,
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
            )
        )

        // CATEGORY PILLS Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setCategory(category) },
                    label = { Text(category, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("category_chip_$category")
                )
            }
        }

        // CONTENT BODY (List of Apps)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (appsList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AppBlocking,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Приложений не найдено",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Попробуйте изменить поисковый запрос или категорию утилит.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Preseeded Beautiful Banner showing Sotark promo
                    item {
                        SotarkPromoBanner(onNavigateToDetail)
                    }

                    item {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Результаты поиска" else "Все приложения",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    items(appsList, key = { it.id }) { app ->
                        AppListItem(app = app, onClick = { onNavigateToDetail(app.id) })
                    }
                }
            }

            // FLOATING ACTION BUTTON to publish an app
            ExtendedFloatingActionButton(
                onClick = onNavigateToPublish,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("publish_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Filled.Add, contentDescription = "Опубликовать") },
                text = { Text("Опубликовать") }
            )
        }
    }
}

@Composable
fun SotarkPromoBanner(onNavigateToDetail: (Long) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .padding(16.dp)
            .clickable { onNavigateToDetail(1L) }, // Direct link to clicker game
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Beautiful custom geometric layout behind
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = size.width * 0.4f,
                        center = Offset(size.width * 0.9f, size.height * 0.1f)
                    )
                }
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.75f),
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.wrapContentSize()
                ) {
                    Text(
                        text = "ТРЕНД НЕДЕЛИ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sotark Tap Clicker",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Постройте крупнейшую энергетическую империю вселенной в оранжево-черных тонах!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Floating Custom Console Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun AppListItem(app: AppEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
            .testTag("app_item_${app.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(symbolName = app.iconSymbol, accentColorHex = app.iconAccentColorHex, sizeDp = 56.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = app.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.developerName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Rating
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Рейтинг",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", app.getAverageRating()),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Separation DOT
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )

                    // Category text
                    Text(
                        text = app.category,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    // Separation DOT
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )

                    // Size
                    Text(
                        text = "${app.sizeMb} МБ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Install Check Indicator Arrow
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Подробнее",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}


// 2. APP DETAIL SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    appId: Long,
    viewModel: SotarkPlayViewModel,
    onBack: () -> Unit,
    onNavigateToDev: (Long) -> Unit
) {
    val detailState by viewModel.appDetailState.collectAsStateWithLifecycle()
    var showReviewDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали приложения", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = detailState) {
                is DetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is DetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Error, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(state.message, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) { Text("Вернуться назад") }
                    }
                }
                is DetailUiState.Success -> {
                    val app = state.app
                    val reviews = state.reviews
                    val dev = state.developer

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        // Header section with Icon, Name, Category
                        item {
                            AppDetailHeader(app = app, onDevClick = { onNavigateToDev(app.developerId) })
                        }

                        // Installation Action Panel
                        item {
                            InstallationPanel(app = app, viewModel = viewModel)
                        }

                        // Specs Grid
                        item {
                            AppSpecsGrid(app = app)
                        }

                        // Expandable Description
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Описание", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = app.description,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        // Developer info card
                        dev?.let { d ->
                            item {
                                DeveloperMiniCard(developer = d, onDevClick = { onNavigateToDev(d.id) })
                            }
                        }

                        // Reviews section Header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Отзывы (${reviews.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                TextButton(
                                    onClick = { showReviewDialog = true },
                                    modifier = Modifier.testTag("add_review_button")
                                ) {
                                    Icon(Icons.Filled.RateReview, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Оценить")
                                }
                            }
                        }

                        // No Reviews placeholder
                        if (reviews.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Отзывов пока нет. Вы будете первыми!",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        } else {
                            items(reviews, key = { it.id }) { review ->
                                ReviewItem(review = review)
                            }
                        }
                    }

                    // Dialog to write a reviews
                    if (showReviewDialog) {
                        WriteReviewDialog(
                            appId = app.id,
                            onDismiss = { showReviewDialog = false },
                            onSubmit = { name, email, text, rating ->
                                viewModel.submitReview(app.id, name, email, text, rating)
                                showReviewDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// Composable function modifier size72 shortcut
fun Modifier.size72(): Modifier = this.size(72.dp)

@Composable
fun AppDetailHeader(app: AppEntity, onDevClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(symbolName = app.iconSymbol, accentColorHex = app.iconAccentColorHex, sizeDp = 80.dp)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = app.developerName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onDevClick() }
                    .padding(vertical = 2.dp)
            )
            Text(
                text = app.category,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InstallationPanel(app: AppEntity, viewModel: SotarkPlayViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (app.installStatus) {
                        "NOT_INSTALLED" -> "Доступно бесплатно"
                        "DOWNLOADING" -> "Загрузка: ${app.downloadProgress}%"
                        "INSTALLING" -> "Установка..."
                        "INSTALLED" -> "Приложение установлено"
                        else -> "Доступно бесплатно"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (app.installStatus == "DOWNLOADING" || app.installStatus == "INSTALLING") {
                    LinearProgressIndicator(
                        progress = { if (app.installStatus == "INSTALLING") 1f else app.downloadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                    )
                } else {
                    Text(
                        text = "Версия ${app.version} | ${app.sizeMb} МБ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ACTION CONTROLS
            when (app.installStatus) {
                "NOT_INSTALLED" -> {
                    Button(
                        onClick = { viewModel.installApp(app.id) },
                        modifier = Modifier.testTag("install_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Скачать")
                    }
                }
                "DOWNLOADING", "INSTALLING" -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                "INSTALLED" -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Uninstall button (Red action)
                        IconButton(
                            onClick = { viewModel.uninstallApp(app.id) },
                            modifier = Modifier.testTag("uninstall_button")
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                        }

                        // Open simulated experience
                        Button(
                            onClick = { viewModel.openRunningApp(app) },
                            modifier = Modifier.testTag("open_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.Launch, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Открыть")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppSpecsGrid(app: AppEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SpecCard(title = "РЕЙТИНГ", value = String.format("%.1f ★", app.getAverageRating()), desc = "${app.ratingCount} оценок")
        SpecCard(title = "СКАЧИВАНИЯ", value = "${app.downloadCount}", desc = "установок")
        SpecCard(title = "ОБЪЕМ", value = "${app.sizeMb}", desc = "Мегабайт")
    }
}

@Composable
fun RowScope.SpecCard(title: String, value: String, desc: String) {
    Card(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DeveloperMiniCard(developer: Developer, onDevClick: () -> Unit) {
    val avatarBg = remember(developer.avatarColorHex) {
        try {
            Color(android.graphics.Color.parseColor(developer.avatarColorHex))
        } catch (_: Exception) {
            Color(0xFFFF7214)
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onDevClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = developer.name.firstOrNull()?.toString() ?: "D",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Разработчик", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(developer.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(developer.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ReviewItem(review: Review) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.reviewerName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (index < review.rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = review.text,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun WriteReviewDialog(
    appId: Long,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(5) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Оставить отзыв", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))

                // Stars rating selection
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    repeat(5) { idx ->
                        val currentStar = idx + 1
                        IconButton(
                            onClick = { rating = currentStar },
                            modifier = Modifier.size(40.dp).testTag("dialog_star_$currentStar")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "$currentStar звезд",
                                tint = if (currentStar <= rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя или Никнейм") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_nickname"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Эл. почта") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_email"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Ваш отзыв") },
                    modifier = Modifier.fillMaxWidth().height(100.dp).testTag("dialog_text_input"),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotEmpty() && text.isNotEmpty()) {
                                onSubmit(name, email, text, rating)
                            }
                        },
                        enabled = name.isNotEmpty() && text.isNotEmpty(),
                        modifier = Modifier.testTag("submit_review_dialog")
                    ) {
                        Text("Отправить")
                    }
                }
            }
        }
    }
}


// 3. DEVELOPER PROFILE SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperProfileScreen(
    devId: Long,
    viewModel: SotarkPlayViewModel,
    onBack: () -> Unit,
    onNavigateToApp: (Long) -> Unit
) {
    val devState by viewModel.developerProfileState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль разработчика", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = devState) {
                is DevProfileUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is DevProfileUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) { Text("Назад") }
                    }
                }
                is DevProfileUiState.Success -> {
                    val developer = state.developer
                    val devApps = state.apps
                    val avatarBg = remember(developer.avatarColorHex) {
                        try {
                            Color(android.graphics.Color.parseColor(developer.avatarColorHex))
                        } catch (_: Exception) {
                            Color(0xFFFF7214)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        // Developer Avatar Header block
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(avatarBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = developer.name.firstOrNull()?.toString() ?: "D",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 32.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = developer.name,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = developer.email,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = developer.bio,
                                        modifier = Modifier.padding(16.dp),
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Published apps title block
                        item {
                            Text(
                                text = "Опубликованные приложения (${devApps.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // App listings
                        if (devApps.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Приложений пока нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(devApps, key = { it.id }) { app ->
                                AppListItem(app = app, onClick = { onNavigateToApp(app.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}


// 4. PUBLISH APP SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishAppScreen(
    viewModel: SotarkPlayViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Игры") }
    var sizeStr by remember { mutableStateOf("15.2") }
    var version by remember { mutableStateOf("1.0.0") }

    // Developer profiling inside same screen (Register or lock)
    var devName by remember { mutableStateOf("") }
    var devEmail by remember { mutableStateOf("") }
    var devBio by remember { mutableStateOf("") }

    // Icons Customizer
    val preseedSymbols = listOf("sports_esports", "cloud", "forum", "brush", "directions_car", "mood")
    var selectedSymbol by remember { mutableStateOf("sports_esports") }

    val preseedAccents = listOf("#FF7214", "#1E88E5", "#E040FB", "#00E676", "#FFD700", "#FF4081")
    var selectedAccent by remember { mutableStateOf("#FF7214") }

    val categories = listOf("Игры", "Софт", "Инструменты", "Соцсети", "Развлечения")
    var showCategoryMenu by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Публикация софта", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // STEP 1: App Identity Card Customizer
            Text("1. Внешний вид приложения", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    // Preview Icon
                    AppIcon(symbolName = selectedSymbol, accentColorHex = selectedAccent, sizeDp = 72.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Интерактивный предпросмотр иконки", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // Accent Colors Picker
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        preseedAccents.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        2.dp,
                                        if (selectedAccent == hex) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { selectedAccent = hex }
                            )
                        }
                    }

                    // Symbols Picker
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        preseedSymbols.forEach { sym ->
                            val iconVector = when (sym) {
                                "sports_esports" -> Icons.Filled.SportsEsports
                                "cloud" -> Icons.Filled.Cloud
                                "forum" -> Icons.Filled.Forum
                                "brush" -> Icons.Filled.Brush
                                "directions_car" -> Icons.Filled.DirectionsCar
                                "mood" -> Icons.Filled.Mood
                                else -> Icons.Filled.Apps
                            }
                            IconButton(
                                onClick = { selectedSymbol = sym },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedSymbol == sym) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else Color.Transparent
                                    )
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = if (selectedSymbol == sym) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // STEP 2: App Data
            Spacer(modifier = Modifier.height(20.dp))
            Text("2. Детали Вашего софта", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название приложения") },
                modifier = Modifier.fillMaxWidth().testTag("publish_title"),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // DROPDOWN CATEGORY MENU
            ExposedDropdownMenuBox(
                expanded = showCategoryMenu,
                onExpandedChange = { showCategoryMenu = !showCategoryMenu },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = category,
                    onValueChange = {},
                    label = { Text("Категория") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false }
                ) {
                    categories.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                category = selectionOption
                                showCategoryMenu = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = sizeStr,
                    onValueChange = { sizeStr = it },
                    label = { Text("Размер (МБ)") },
                    modifier = Modifier.weight(1f).testTag("publish_size"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = version,
                    onValueChange = { version = it },
                    label = { Text("Версия") },
                    modifier = Modifier.weight(1f).testTag("publish_version"),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание приложения, функции и т.д.") },
                modifier = Modifier.fillMaxWidth().height(110.dp).testTag("publish_desc"),
                maxLines = 5
            )

            // STEP 3: Developer Info
            Spacer(modifier = Modifier.height(20.dp))
            Text("3. Об авторе (Профиль разработчика)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = devName,
                onValueChange = { devName = it },
                label = { Text("Никнейм или Студия") },
                modifier = Modifier.fillMaxWidth().testTag("publish_dev_name"),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = devEmail,
                onValueChange = { devEmail = it },
                label = { Text("Эл. почта") },
                modifier = Modifier.fillMaxWidth().testTag("publish_dev_email"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = devBio,
                onValueChange = { devBio = it },
                label = { Text("Пару слов о себе (Био)") },
                modifier = Modifier.fillMaxWidth().height(80.dp).testTag("publish_dev_bio"),
                maxLines = 3
            )

            // TRIGGER ACTION
            val isFormValid = title.isNotEmpty() && description.isNotEmpty() && devName.isNotEmpty() && devEmail.isNotEmpty()
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val finalSize = sizeStr.toDoubleOrNull() ?: 10.0
                    viewModel.publishNewApp(
                        title = title,
                        description = description,
                        category = category,
                        sizeMb = finalSize,
                        version = version,
                        developerName = devName,
                        developerEmail = devEmail,
                        developerBio = devBio,
                        accentColorHex = selectedAccent,
                        symbolName = selectedSymbol,
                        onSuccess = {
                            onBack()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_publish_button"),
                enabled = isFormValid,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Опубликовать софт бесплатно", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}


// 5. INTERACTIVE SIMULATORS FOR INSTALLED APP RUNS
@Composable
fun SimulatedAppRunnerView(
    app: AppEntity,
    viewModel: SotarkPlayViewModel,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F1A)) // Fun futuristic backdrop for simulated device
        ) {
            // Simulated Device Shell Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Mock device top bar (Status Bar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(Color(0xFF07070F))
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sotark OS v1.0 [Simulated Android]", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Wifi, "Wifi Status", tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Icon(Icons.Filled.BatteryChargingFull, "Battery", tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Text("15:23", fontSize = 10.sp, color = Color.Gray)
                    }
                }

                // Application Custom Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1D1B26))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(symbolName = app.iconSymbol, accentColorHex = app.iconAccentColorHex, sizeDp = 36.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(app.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            Text("разработчик: ${app.developerName}", fontSize = 10.sp, color = Color.LightGray)
                        }
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("sim_close")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Закрыть симулятор", tint = Color.Red)
                    }
                }

                // Interactive Content area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF17151F))
                ) {
                    // Route to specific simulator
                    when (app.id) {
                        1L -> TapClickerSimulator(viewModel)
                        2L -> OrionWeatherSimulator(viewModel)
                        3L -> ChatSphereSimulator(viewModel)
                        4L -> QuickPaintSimulator(viewModel)
                        5L -> DoodleRacerSimulator()
                        else -> GenericAppSimulator(app = app)
                    }
                }

                // Simulated Navigation Keys Bar (Home, Back)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color(0xFF0F0E17)),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Sim Back", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Home, "Sim Home", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// SIMULATOR A: TAP CLICKER IN ENERGETICS
@Composable
fun TapClickerSimulator(viewModel: SotarkPlayViewModel) {
    val score by viewModel.clickerCount.collectAsStateWithLifecycle()
    var reactorPower by remember { mutableStateOf(1) }

    val coroutineScope = rememberCoroutineScope()
    var scaleFactor by remember { mutableStateOf(1f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "SOTARK ENERGY CLICKER",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFF7214),
                    fontSize = 20.sp,
                )
                Text(
                    "Фирменный кликер Sotark Play",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }

            // Energy counter
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$score",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    "МЕГАВАТТ НАКОПЛЕНО",
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }

            // Reactor Tap Button
            val animatedScale by animateFloatAsState(
                targetValue = scaleFactor,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                finishedListener = { scaleFactor = 1f },
                label = ""
            )

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .clickable {
                        viewModel.tapClicker()
                        scaleFactor = 0.85f
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFF9E66), Color(0xFFFF5500))
                        )
                    )
                    .border(4.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.OfflineBolt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }

            // Simple reactor upgrades
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Surface(
                    color = Color(0xFF282535),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.clickable {
                        if (score >= 10) {
                            // Upgrading (mock)
                            reactorPower++
                        }
                    }
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Улучшить ядро (10 МВт)", fontSize = 10.sp, color = Color.White)
                        Text("Сила клика: x$reactorPower", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9E66))
                    }
                }
            }
        }
    }
}

// SIMULATOR B: WEATHER BAROMETER SCANNER
@Composable
fun OrionWeatherSimulator(viewModel: SotarkPlayViewModel) {
    val wind by viewModel.weatherWindSpeed.collectAsStateWithLifecycle()
    val phase by viewModel.weatherPhaseText.collectAsStateWithLifecycle()
    var isScanning by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scanAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )

    LaunchedEffect(isScanning) {
        if (isScanning) {
            delay(2000)
            viewModel.measureWeather()
            isScanning = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("ОРИОН ПОГОДНЫЙ РАДАР", fontWeight = FontWeight.Bold, color = Color(0xFF42A5F5), fontSize = 18.sp)

            // Circular radar sweeping visual
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0A141D))
                    .border(2.dp, Color(0xFF42A5F5), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // grid lines
                    drawCircle(Color(0xFF42A5F5).copy(alpha = 0.2f), radius = size.minDimension / 4)
                    drawCircle(Color(0xFF42A5F5).copy(alpha = 0.2f), radius = size.minDimension / 2)
                    drawLine(Color(0xFF42A5F5).copy(alpha = 0.2f), Offset(0f, size.height/2), Offset(size.width, size.height/2))
                    drawLine(Color(0xFF42A5F5).copy(alpha = 0.2f), Offset(size.width/2, 0f), Offset(size.width/2, size.height))

                    // radar sweeping hand
                    if (isScanning) {
                        val length = size.minDimension / 2
                        val rx = (length * kotlin.math.cos(Math.toRadians(scanAngle.toDouble()))).toFloat()
                        val ry = (length * kotlin.math.sin(Math.toRadians(scanAngle.toDouble()))).toFloat()
                        drawLine(
                            color = Color(0xFF00E676),
                            start = Offset(size.width / 2, size.height / 2),
                            end = Offset(size.width / 2 + rx, size.height / 2 + ry),
                            strokeWidth = 3f
                        )
                    }
                }
                Icon(Icons.Filled.CloudQueue, null, tint = Color.White, modifier = Modifier.size(44.dp))
            }

            // Stats
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF202936))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Скорость ветра: ${String.format("%.1f", wind)} м/с", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Космическая погода: $phase", color = Color.LightGray, fontSize = 13.sp)
                }
            }

            Button(
                onClick = { isScanning = true },
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сканирование эфира...")
                } else {
                    Icon(Icons.Filled.NetworkCheck, null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Запустить метео-сканер")
                }
            }
        }
    }
}

// SIMULATOR C: CHATSPHERE CLIENT
@Composable
fun ChatSphereSimulator(viewModel: SotarkPlayViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val listState = rememberScrollState()

    LaunchedEffect(messages.size) {
        listState.animateScrollTo(listState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Chat History Scroll
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(listState)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            messages.forEach { (msg, isMe) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (isMe) Color(0xFFFF7214) else Color(0xFF2C253B),
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isMe) 12.dp else 0.dp,
                            bottomEnd = if (isMe) 0.dp else 12.dp
                        ),
                        modifier = Modifier.widthIn(max = 240.dp)
                    ) {
                        Text(
                            text = msg,
                            modifier = Modifier.padding(10.dp),
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Input bottom bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Зашифрованное сообщение...", color = Color.Gray, fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1E1C24),
                    unfocusedContainerColor = Color(0xFF1E1C24)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (input.trim().isNotEmpty()) {
                        viewModel.sendChatMessage(input)
                        input = ""
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFFF7214))
            ) {
                Icon(Icons.Filled.Send, "Send", tint = Color.White)
            }
        }
    }
}

// SIMULATOR D: MULTITOUCH CANVAS DOODLE PAINTER
@Composable
fun QuickPaintSimulator(viewModel: SotarkPlayViewModel) {
    val paths by viewModel.paintPaths.collectAsStateWithLifecycle()
    var selectedColor by remember { mutableStateOf(Color.Red) }
    var currentPath = remember { mutableStateListOf<Offset>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ХОЛСТ БЫСТРЫХ НАБРОСКОВ", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            IconButton(onClick = { viewModel.clearPaintCanvas() }) {
                Icon(Icons.Filled.DeleteSweep, "Clear Drawing", tint = Color.Red)
            }
        }

        // Multi-touch Drawing canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { currentPath.clear() },
                        onDragEnd = {
                            if (currentPath.isNotEmpty()) {
                                viewModel.addPaintPath(currentPath.toList() to selectedColor)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentPath.add(change.position)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw historic paths saved in VM
                paths.forEach { (pathPoints, pathColor) ->
                    if (pathPoints.size > 1) {
                        for (i in 0 until pathPoints.size - 1) {
                            drawLine(
                                color = pathColor,
                                start = pathPoints[i],
                                end = pathPoints[i + 1],
                                strokeWidth = 8f
                            )
                        }
                    }
                }

                // Draw current drawing path in progress
                if (currentPath.size > 1) {
                    for (i in 0 until currentPath.size - 1) {
                        drawLine(
                            color = selectedColor,
                            start = currentPath[i],
                            end = currentPath[i + 1],
                            strokeWidth = 8f
                        )
                    }
                }
            }
        }

        // Palette Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val colors = listOf(Color.Red, Color.Blue, Color(0xFF00E676), Color.Black, Color(0xFFFF7214))
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            2.dp,
                            if (selectedColor == color) Color.DarkGray else Color.Transparent,
                            CircleShape
                        )
                        .clickable { selectedColor = color }
                )
            }
        }
    }
}

// SIMULATOR E: RETRO DOODLE RACER MINI-GAME
@Composable
fun DoodleRacerSimulator() {
    var gameActive by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    var carLane by remember { mutableStateOf(1) } // 0 = Left, 1 = Middle, 2 = Right

    // Obstacles descending
    var obstacleY by remember { mutableStateOf(0f) }
    var obstacleLane by remember { mutableStateOf((0..2).random()) }

    // Tick game loop when active
    LaunchedEffect(gameActive) {
        while (gameActive) {
            delay(50)
            obstacleY += 12f // speed
            if (obstacleY > 400f) {
                // dodged! Increment score
                obstacleY = 0f
                obstacleLane = (0..2).random()
                score++
            }

            // check crash
            if (obstacleY in 300f..350f && obstacleLane == carLane) {
                gameActive = false
                gameOver = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!gameActive && !gameOver) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.DirectionsCar, null, tint = Color.Yellow, modifier = Modifier.size(72.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("КАРАКУЛИ ГОНКИ", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Уворачивайтесь от преград! Управление в 1 тап.", color = Color.LightGray, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    score = 0
                    carLane = 1
                    obstacleY = 0f
                    obstacleLane = (0..2).random()
                    gameActive = true
                    gameOver = false
                }) {
                    Text("НАЧАТЬ ИГРУ")
                }
            }
        } else if (gameOver) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ИГРА ОКОНЧЕНА 💥", fontWeight = FontWeight.Black, color = Color.Red, fontSize = 24.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Ваш счет: $score уклонений", color = Color.White, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    score = 0
                    carLane = 1
                    obstacleY = 0f
                    obstacleLane = (0..2).random()
                    gameActive = true
                    gameOver = false
                }) {
                    Text("Сыграть снова")
                }
            }
        } else {
            // Active Game screen
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Счет: $score", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Каракули Гонки", color = Color.Yellow, fontSize = 11.sp)
                }

                // Road Screen Box (Canvas drawing)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .border(1.dp, Color.LightGray)
                        .background(Color(0xFF23252E))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // draw 3 lanes separators
                        val laneWidth = size.width / 3f
                        drawLine(Color.Gray, Offset(laneWidth, 0f), Offset(laneWidth, size.height), strokeWidth = 2f)
                        drawLine(Color.Gray, Offset(laneWidth * 2, 0f), Offset(laneWidth * 2, size.height), strokeWidth = 2f)

                        // Draw descending obstacle
                        val obsX = obstacleLane * laneWidth + (laneWidth / 2f)
                        drawCircle(Color.Red, radius = 24f, center = Offset(obsX, obstacleY))

                        // Draw Player Car representation
                        val carX = carLane * laneWidth + (laneWidth / 2f)
                        val carY = size.height - 80f
                        drawRect(Color.Yellow, topLeft = Offset(carX - 20f, carY - 30f), size = androidx.compose.ui.geometry.Size(40f, 60f))
                    }
                }

                // Steering controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { if (carLane > 0) carLane-- },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Icon(Icons.Filled.ArrowBackIosNew, "Left")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Лево")
                    }
                    Button(
                        onClick = { if (carLane < 2) carLane++ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("Право")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.ArrowForwardIos, "Right")
                    }
                }
            }
        }
    }
}

// SIMULATOR G: GENERIC RUNNING APP TERMINAL REPORT
@Composable
fun GenericAppSimulator(app: AppEntity) {
    var timerSeconds by remember { mutableStateOf(0) }
    var terminalHistory by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(Unit) {
        terminalHistory = listOf(
            "[INIT] Запуск виртуальной среды Sotark OS...",
            "Загрузка пакета: ${app.title} (v${app.version})",
            "Создано локальное окружение в куче: 0x7FFA830C",
            "Выполнен аудит подписей... Успешно ✅",
            "[SYSTEM] Приложение работает стабильно."
        )

        while (true) {
            delay(1500)
            timerSeconds++
            val logs = listOf(
                "Запрос дескрипторов пройден",
                "Асинхронные потоки ядра активны",
                "Релаксация памяти: освобождено 15 МБ",
                "Синхронизация локальных настроек OK",
                "Метрики отправлены на заглушку сервера"
            )
            val update = terminalHistory.toMutableList()
            update.add("[INFO - t+${timerSeconds}s] ${logs.random()}")
            if (update.size > 8) update.removeAt(0)
            terminalHistory = update
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AppIcon(symbolName = app.iconSymbol, accentColorHex = app.iconAccentColorHex, sizeDp = 50.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(app.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                Text("Версия в песочнице: ${app.version}", fontSize = 11.sp, color = Color.LightGray)
            }

            // Green terminal logs block
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = BorderStroke(1.dp, Color(0xFF00E676))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    terminalHistory.forEach { log ->
                        Text(
                            text = log,
                            color = Color(0xFF00E676),
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            // Interactive "Self-Test Check" button
            Button(
                onClick = {
                    val update = terminalHistory.toMutableList()
                    update.add("[TEST] Прохождение ручной диагностики... Все тесты успешны! 👍")
                    terminalHistory = update
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
            ) {
                Icon(Icons.Filled.SystemUpdateAlt, null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Локальный тест принудительно", color = Color.White)
            }
        }
    }
}
