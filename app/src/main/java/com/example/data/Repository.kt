package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppRepository(private val db: AppDatabase) {
    private val appDao = db.appDao()
    private val developerDao = db.developerDao()
    private val reviewDao = db.reviewDao()

    val allApps: Flow<List<AppEntity>> = appDao.getAllApps()

    fun getAppsByCategory(category: String): Flow<List<AppEntity>> = appDao.getAppsByCategory(category)

    fun searchApps(query: String): Flow<List<AppEntity>> = appDao.searchApps(query)

    fun getAppById(id: Long): Flow<AppEntity?> = appDao.getAppById(id)

    fun getAppsByDeveloper(developerId: Long): Flow<List<AppEntity>> = appDao.getAppsByDeveloper(developerId)

    fun getReviewsForApp(appId: Long): Flow<List<Review>> = reviewDao.getReviewsForApp(appId)

    suspend fun getDeveloperById(id: Long): Developer? = withContext(Dispatchers.IO) {
        developerDao.getDeveloperById(id)
    }

    suspend fun getOrCreateDeveloper(name: String, email: String, bio: String): Long = withContext(Dispatchers.IO) {
        val existing = developerDao.getDeveloperByEmail(email)
        if (existing != null) {
            existing.id
        } else {
            val colors = listOf("#FF7214", "#1E88E5", "#E040FB", "#00E676", "#FFD700")
            val randColor = colors.random()
            developerDao.insertDeveloper(Developer(name = name, email = email, bio = bio, avatarColorHex = randColor))
        }
    }

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
        iconSymbol: String
    ): Long = withContext(Dispatchers.IO) {
        val devId = getOrCreateDeveloper(developerName, developerEmail, developerBio)
        val newApp = AppEntity(
            title = title,
            description = description,
            category = category,
            sizeMb = sizeMb,
            version = version,
            developerId = devId,
            developerName = developerName,
            iconAccentColorHex = iconAccentColorHex,
            iconSymbol = iconSymbol,
            downloadCount = 0,
            installStatus = "NOT_INSTALLED"
        )
        val appId = appDao.insertApp(newApp)

        // Seed an initial friendly review
        reviewDao.insertReview(
            Review(
                appId = appId,
                reviewerName = "Sotark Quality Bot",
                reviewerEmail = "bot@sotark.store",
                text = "Приложение успешно опубликовано и прошло базовую проверку совместимости со всеми версиями Android. Поздравляем разработчика!",
                rating = 5
            )
        )
        appDao.addRatingToApp(appId, 5.0)

        appId
    }

    suspend fun addReview(appId: Long, name: String, email: String, text: String, rating: Int) = withContext(Dispatchers.IO) {
        val review = Review(
            appId = appId,
            reviewerName = name,
            reviewerEmail = email,
            text = text,
            rating = rating
        )
        reviewDao.insertReview(review)
        appDao.addRatingToApp(appId, rating.toDouble())
    }

    suspend fun deleteApp(appId: Long) = withContext(Dispatchers.IO) {
        val app = appDao.getAppByIdSuspend(appId)
        if (app != null) {
            // Delete app (we write uninstall or simple status reset)
            appDao.updateInstallStatus(appId, "NOT_INSTALLED", 0)
        }
    }

    // Interactive simulated "server-side" download and package installation
    suspend fun installApp(appId: Long) = withContext(Dispatchers.IO) {
        try {
            // 1. Set to DOWNLOADING
            appDao.updateInstallStatus(appId, "DOWNLOADING", 0)
            
            // Increment progress
            for (progress in 10..100 step 15) {
                delay(250)
                appDao.updateInstallStatus(appId, "DOWNLOADING", minOf(progress, 100))
            }
            
            // 2. Set to INSTALLING
            appDao.updateInstallStatus(appId, "INSTALLING", 100)
            delay(1200)

            // 3. Set to INSTALLED & increment download counts
            appDao.updateInstallStatus(appId, "INSTALLED", 100)
            appDao.incrementDownloadCount(appId)
        } catch (e: Exception) {
            Log.e("Repository", "Failed install simulation", e)
            appDao.updateInstallStatus(appId, "NOT_INSTALLED", 0)
        }
    }

    suspend fun uninstallApp(appId: Long) = withContext(Dispatchers.IO) {
        appDao.updateInstallStatus(appId, "NOT_INSTALLED", 0)
    }

    // Database Seeding
    suspend fun seedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        val existingApps = appDao.getAllApps().first()
        if (existingApps.isNotEmpty()) {
            return@withContext
        }

        Log.d("Repository", "Seeding database with default developers and apps...")

        // Seed Developers
        val dev1Id = developerDao.insertDeveloper(
            Developer(
                name = "Sotark Dev Studio",
                email = "studio@sotark.ru",
                bio = "Независимая студия разработки игр и утилит для Android. Наша миссия — качественный софт для каждого.",
                avatarColorHex = "#FF7214"
            )
        )

        val dev2Id = developerDao.insertDeveloper(
            Developer(
                name = "Aero Soft",
                email = "contact@aerosoft.com",
                bio = "Создатели элегантных повседневных утилит, погодных приложений и виджетов.",
                avatarColorHex = "#1E88E5"
            )
        )

        val dev3Id = developerDao.insertDeveloper(
            Developer(
                name = "Социал Лабс",
                email = "labs@social.net",
                bio = "Компания, объединяющая людей. Мы разрабатываем защищенные мессенджеры и социальные хабы.",
                avatarColorHex = "#E040FB"
            )
        )

        // Seed Apps
        // 1. Sotark Clicker (Игры)
        val app1Id = appDao.insertApp(
            AppEntity(
                title = "Sotark Tap Clicker",
                description = "Залипательный фантастический кликер в фирменном оранжево-черном стиле! Нажимайте на ядро реактора, улучшайте генераторы энергии, нанимайте роботов-помощников и постройте крупнейшую энергетическую империю в галактике. Простой игровой процесс, который затягивает на часы!",
                category = "Игры",
                sizeMb = 12.4,
                version = "1.2.0",
                developerId = dev1Id,
                developerName = "Sotark Dev Studio",
                iconAccentColorHex = "#FF5500",
                iconSymbol = "sports_esports",
                downloadCount = 1420
            )
        )

        // 2. Space Weather (Инструменты)
        val app2Id = appDao.insertApp(
            AppEntity(
                title = "Орион Погода",
                description = "Уникальное погодное приложение с невероятной детализацией погодных условий, уровня давления, влажности и фаз луны. Просматривайте интерактивные 3D карты ветра, симуляцию космических бурь и солнечной активности в реальном времени. Лаконичный интерфейс в голубых тонах.",
                category = "Инструменты",
                sizeMb = 28.1,
                version = "3.1.5",
                developerId = dev2Id,
                developerName = "Aero Soft",
                iconAccentColorHex = "#0288D1",
                iconSymbol = "cloud",
                downloadCount = 850
            )
        )

        // 3. ChatSphere (Соцсети)
        val app3Id = appDao.insertApp(
            AppEntity(
                title = "ChatSphere (Сфера Общения)",
                description = "Тайный пиринговый мессенджер нового поколения без центральных серверов! Полное шифрование сообщений, секретные исчезающие комнаты, обмен зашифрованными файлами любого объема и встроенный ИИ-помощник для перевода сообщений на лету. Общайтесь конфиденциально.",
                category = "Соцсети",
                sizeMb = 45.0,
                version = "0.9.8",
                developerId = dev3Id,
                developerName = "Социал Лабс",
                iconAccentColorHex = "#E040FB",
                iconSymbol = "forum",
                downloadCount = 3100
            )
        )

        // 4. Quick Canvas Paint (Софт)
        val app4Id = appDao.insertApp(
            AppEntity(
                title = "Панель Рисования",
                description = "Удобный и интуитивно понятный графический редактор для набросков, портретов и создания модных мемов. Набор профессиональных кистей, поддержка слоев, настраиваемое сглаживание линий и экспорт готовых рисунков прямо в галерею смартфона.",
                category = "Софт",
                sizeMb = 8.7,
                version = "2.0.1",
                developerId = dev2Id,
                developerName = "Aero Soft",
                iconAccentColorHex = "#00E676",
                iconSymbol = "brush",
                downloadCount = 920
            )
        )

        // 5. Doodle Racer (Игры)
        val app5Id = appDao.insertApp(
            AppEntity(
                title = "Каракули Гонки",
                description = "Ретро-гоночная аркада, нарисованная на тетрадном листе! Избегайте препятствий, собирайте капли топлива, покупайте новые забавные машинки и побеждайте в бесконечных заездах. Управление в одно касание!",
                category = "Игры",
                sizeMb = 15.2,
                version = "1.0.3",
                developerId = dev1Id,
                developerName = "Sotark Dev Studio",
                iconAccentColorHex = "#FFC107",
                iconSymbol = "directions_car",
                downloadCount = 740
            )
        )

        // Feed Reviews
        // App 1
        reviewDao.insertReview(Review(appId = app1Id, reviewerName = "Алексей Павлов", reviewerEmail = "alex@mail.ru", text = "Кликер просто потрясающий! Очень стильный интерфейс, играть в темной теме — одно удовольствие. Разрабам респект!", rating = 5))
        reviewDao.insertReview(Review(appId = app1Id, reviewerName = "Мария К.", reviewerEmail = "masha@yandex.ru", text = "Интересная задумка, но хочется больше видов улучшений и фоновые звуки. Жду обновлений!", rating = 4))
        appDao.addRatingToApp(app1Id, 9.0) // 5 + 4

        // App 2
        reviewDao.insertReview(Review(appId = app2Id, reviewerName = "Дмитрий С.", reviewerEmail = "dima@gmail.com", text = "Красивое оформление, показывает даже силу солнечного ветра. Намного круче стандартных скучных погодных приложений.", rating = 5))
        reviewDao.insertReview(Review(appId = app2Id, reviewerName = "Юлия В.", reviewerEmail = "julia@list.ru", text = "Иногда немного долго обновляется геопозиция, но в целом супер.", rating = 4))
        appDao.addRatingToApp(app2Id, 9.0)

        // App 3
        reviewDao.insertReview(Review(appId = app3Id, reviewerName = "Кирилл", reviewerEmail = "kir@web.net", text = "Мессенджер действительно анонимный, дизайн стильный, функции на высоте. Буду теперь сидеть только в Сфере!", rating = 5))
        reviewDao.insertReview(Review(appId = app3Id, reviewerName = "Анна М.", reviewerEmail = "anna@mail.ru", text = "Интерфейс выглядит здорово, но некоторые друзья жалуются на долгие видеозвонки. Переписки работают мгновенно.", rating = 5))
        reviewDao.insertReview(Review(appId = app3Id, reviewerName = "ГеймерПро", reviewerEmail = "gp@outlook.com", text = "Шикарный концепт без единого тормоза. Ставлю пять звезд!", rating = 5))
        appDao.addRatingToApp(app3Id, 15.0)

        // App 4
        reviewDao.insertReview(Review(appId = app4Id, reviewerName = "Антон", reviewerEmail = "ant@paint.ru", text = "Отличная рисовалка, ничего лишнего и полностью бесплатно. Благодарен за отсутствие рекламы!", rating = 5))
        appDao.addRatingToApp(app4Id, 5.0)

        // App 5
        reviewDao.insertReview(Review(appId = app5Id, reviewerName = "Елена Г.", reviewerEmail = "elena@yandex.ru", text = "Забавные гоночки, играем с сыном наперегонки. Рисовка ручкой заслуживает отдельной похвалы!", rating = 5))
        appDao.addRatingToApp(app5Id, 5.0)
    }
}
