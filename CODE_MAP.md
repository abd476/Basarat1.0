# BASARAT — Project Code Map

> Reference map of the Android client: folders, files, dependencies, and how data flows.
> Branch: `phase1.experiments` · Package: `com.fyp.basarattradeinvest` · ApplicationId: `com.basarat.app`

---

## 1. Project Identity & Build Configuration

| Property | Value | Source file |
|---|---|---|
| Project name | Basarat Trade & Invest | `settings.gradle.kts` |
| Gradle module | `:app` (single module) | `settings.gradle.kts` |
| Namespace | `com.fyp.basarattradeinvest` | `app/build.gradle.kts` |
| ApplicationId | `com.basarat.app` | `app/build.gradle.kts` |
| AGP | 9.1.1 | `gradle/libs.versions.toml` |
| Kotlin | 2.2.10 | `gradle/libs.versions.toml` |
| compileSdk / targetSdk | 35 | `app/build.gradle.kts` |
| minSdk | 24 (Android 7.0) | `app/build.gradle.kts` |
| Java | 17 | `app/build.gradle.kts` |
| ViewBinding | enabled | `app/build.gradle.kts` |
| R8 minify (release) | disabled | `app/build.gradle.kts` |
| DI framework | **none** (manual, via constructor defaults) | — |

### Root-level files

| File | Purpose |
|---|---|
| `settings.gradle.kts` | Declares repositories (`google`, `mavenCentral`, `jitpack`), includes `:app` |
| `build.gradle.kts` | Root build script (no logic, delegates to module) |
| `gradle/libs.versions.toml` | **Version catalog** — single source of truth for every dependency version |
| `gradle/wrapper/*` | Gradle wrapper (pins the build tool version) |
| `gradle.properties` | JVM memory / build flags |
| `app/build.gradle.kts` | Module config: SDK levels, build types, dependencies |
| `local.properties` | Android SDK path (gitignored, machine-specific) |
| `openapi.json` | Backend OpenAPI contract, used to generate/verify DTOs |
| `PROJECT_WORK_SUMMARY.md` | Feature documentation |
| `README.md` | Project overview |
| `.github/copilot-instructions.md` | Architecture rules the codebase follows |
| `AGENTS.md` | Agent instructions |
| `scripts/scratch_*.ps1/.py`, `scripts/test_live_*.ps1` | Throwaway scripts used to probe the live backend during development |

---

## 2. Dependencies Aligned to Usage

Every library below is declared in `gradle/libs.versions.toml` and consumed in `app/build.gradle.kts`.
The "Used in" column was verified by scanning imports across the source tree.

### UI / AndroidX

| Library | Version | Used in | Count | Why it's here |
|---|---|---|---|---|
| `androidx.core:core-ktx` | 1.15.0 | everywhere | — | Kotlin extensions (`View.doOnClick`, etc.) |
| `androidx.appcompat` | 1.7.0 | `MainActivity`, themes | — | `AppCompatActivity`, backward-compatible widgets |
| `com.google.android.material` | 1.12.0 | 17 kt files + layouts | Chips, Cards, Buttons, BottomNav, NavigationView | Material 3 component library |
| `androidx.constraintlayout` | 2.2.0 | most layouts | — | Constraint-based screen layout |
| `androidx.activity:activity-ktx` | 1.10.0 | Fragments | — | `by viewModels()`, activity result APIs |
| `androidx.swiperefreshlayout` | 1.1.0 | 26 layouts | — | Pull-to-refresh containers |
| `androidx.coordinatorlayout` | 1.2.0 | 30 layouts | — | Nested scrolling, AppBar behaviour |
| `androidx.lifecycle:lifecycle-viewmodel-ktx` | 2.8.7 | every ViewModel | 245 total lifecycle refs | `viewModelScope`, `ViewModel` |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.8.7 | every Fragment | — | `repeatOnLifecycle`, `lifecycleScope` |
| `androidx.lifecycle:lifecycle-process` | 2.8.7 | `BasaratFirebaseService` | 2 | `ProcessLifecycleOwner` — app-level foreground/background |
| `androidx.navigation:navigation-fragment-ktx` | 2.8.8 | 56 kt files | — | `NavHostFragment`, `navController`, `Safe Args` |
| `androidx.navigation:navigation-ui-ktx` | 2.8.8 | `MainActivity` | — | `setupWithNavController` for BottomNav |

### Networking & Serialization

| Library | Version | Used in | Count | Why it's here |
|---|---|---|---|---|
| `com.squareup.retrofit2:retrofit` | 2.11.0 | 21 API interfaces | 114 imports | Declarative REST client (`@GET`, `@Query`, `@Path`) |
| `retrofit2:converter-gson` | 2.11.0 | `NetworkClient` | — | Converts JSON → DTO via Gson |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | `NetworkClient`, `LiveMarketDataSource` | 24 | Connection pooling, interceptors, authenticator, timeouts |
| `okhttp3:logging-interceptor` | 4.12.0 | `NetworkClient` | — | Configurable HTTP logging with header redaction |
| `com.google.code.gson:gson` | 2.11.0 | 76 DTO files | 76 | JSON ↔ object mapping via `@SerializedName` |

### Async, Charts, Push, Auth

| Library | Version | Used in | Count | Why it's here |
|---|---|---|---|---|
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.10.1 | all ViewModels | 169 flow imports | `suspend`, `Flow`, `StateFlow`, `viewModelScope` |
| `com.github.PhilJay:MPAndroidChart` | v3.1.0 | 7 screens | 54 | Line, bar, pie, candlestick charts |
| `androidx.work:work-runtime-ktx` | 2.12.0 | `DeviceRegistrationWorker`, `DeviceRegistrationScheduler` | 8 | Deferred push-token registration |
| `com.google.firebase:firebase-messaging` | BOM 34.19.0 | `BasaratFirebaseService` | 4 | FCM push notifications |
| `androidx.credentials` | 1.6.0 | `GoogleSignInLauncher` | 6 | Credential Manager (modern sign-in) |
| `androidx.credentials:play-services-auth` | 1.6.0 | `GoogleSignInLauncher` | — | Google ID integration |
| `com.google.android.libraries.identity.googleid` | **1.1.1 (pinned)** | `GoogleSignInLauncher` | 2 | Pinned: 1.2.x ships Kotlin 2.4 metadata unreadable by Kotlin 2.2 compiler |

### Which screens use charts

MPAndroidChart appears in exactly 7 Fragments:

```
feature/dashboard/HomeFragment.kt
feature/portfolio/main/PortfolioMainFragment.kt
feature/recommendation/smartselection/SmartSelectionFragment.kt
feature/sentiment/SentimentFragment.kt
feature/stock/analysis/StockAnalysisFragment.kt
feature/stock/chart/ChartFullscreenFragment.kt
feature/stock/detail/StockDetailFragment.kt
```

### Testing

| Library | Version | Scope |
|---|---|---|
| `junit:junit` | 4.13.2 | `testImplementation` — all 25 unit test classes |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.10.1 | `testImplementation` — `runTest`, `TestDispatcher` |
| `androidx.test.ext:junit` | 1.2.1 | `androidTestImplementation` |
| `androidx.test.espresso:espresso-core` | 3.6.1 | `androidTestImplementation` |

---

## 3. Source Tree Overview

```
app/src/main/java/com/fyp/basarattradeinvest/     409 Kotlin files
├── BasaratApplication.kt                          Application entry point
├── MainActivity.kt                                Single Activity host
├── core/                                          17 files — cross-cutting infrastructure
├── data/                                         109 files — backend integration
├── domain/                                       113 files — pure business logic
└── feature/                                      170 files — 20 UI feature packages
```

**The dependency rule — arrows point inward only:**

```
feature/  ──►  domain/  ◄──  data/
   │            ▲            │
   │            │            │
 Fragment    UseCase    Repository impl
 ViewModel   Model      Retrofit + DTOs
```

- `domain/` contains **zero Android imports** — it cannot reference `android.*`. This is what lets all unit tests run on a bare JVM without an emulator.
- `data/` implements the interfaces declared in `domain/repository/`.
- `feature/` never imports from `data/`. Fragments talk to ViewModels; ViewModels talk to use cases.

---

## 4. `core/` — Cross-Cutting Infrastructure (17 files)

Everything here is reusable across features and knows nothing about any one screen.

```
core/
├── network/
│   ├── AuthInterceptor.kt          Attaches "Authorization: Bearer <token>" to every request
│   ├── TokenAuthenticator.kt       Refreshes token on 401, retries the failed request
│   ├── NetworkErrorParser.kt       Maps HTTP status codes → user-readable messages (79 call sites)
│   └── DevTokenConfig.kt           Developer-mode token override
├── session/
│   ├── SessionManager.kt           object singleton — session facade, owns token lifecycle
│   ├── SecureTokenStorage.kt       AES-256-GCM encrypted SharedPreferences (Android Keystore)
│   ├── AuthState.kt                StateFlow of authentication status
│   └── RefreshTokenFailure.kt      Typed refresh-failure signal
├── notifications/
│   ├── BasaratFirebaseService.kt   FirebaseMessagingService — receives pushes, routes screens
│   ├── DeviceRegistrationWorker.kt WorkManager worker — registers FCM token with backend
│   ├── DeviceRegistrationScheduler.kt Schedules the worker (post-login, retry backoff)
│   ├── NotificationChannels.kt     Android notification channels
│   ├── NotificationEvents.kt       SharedFlow event bus for notification navigation
│   └── NotificationTarget.kt       Typed deep-link target (screen, symbol, ids)
└── ui/
    ├── ImageLoader.kt              Image loading + caching helper
    └── components/
        ├── IntradayChartView.kt    Custom candlestick chart View
        └── MiniSparklineView.kt    Custom sparkline View
```

### Auth flow (the strongest viva topic)

**Step 1 — attach.** `AuthInterceptor` runs on every request. It keeps an allowlist of endpoints that must stay anonymous (login, signup, refresh, password reset) plus the public market WebSocket:

```kotlin
override fun intercept(chain: Interceptor.Chain): Response {
    val path = chain.request().url.encodedPath
    val isUnauthenticated = UNAUTHENTICATED_PATHS.any { path.endsWith(it) }
    val isPublicMarketWebSocket = path == "/api/v1/ws/market"

    val requestBuilder = chain.request().newBuilder()
    if (!isUnauthenticated && !isPublicMarketWebSocket) {
        val token = SessionManager.getAccessToken()
        if (!token.isNullOrBlank() && requestBuilder.header("Authorization") == null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
    }
    return chain.proceed(requestBuilder.build())
}
```

**Step 2 — refresh on 401.** `TokenAuthenticator` has three safeguards:

| Safeguard | Code | Prevents |
|---|---|---|
| Skip auth endpoints | `if (requestUrl.endsWith("/api/v1/auth/refresh") …) return null` | Infinite recursion |
| `synchronized(lock)` | wraps the whole refresh | Concurrent refresh stampede (5 parallel 401s → 5 refreshes → rotating tokens invalidate each other) |
| Already-refreshed check | `if (currentToken != requestToken) retry with currentToken` | Redundant refresh when another thread won the race |
| `responseCount(response) >= 2` | bail to logout | Infinite retry loop |

**Step 3 — store securely.** `SecureTokenStorage` uses the Android Keystore:

- Key alias `BasaratKeyAlias`, generated in `AndroidKeyStore`, **never leaves secure hardware**
- `AES/GCM/NoPadding`, 256-bit, 128-bit auth tag
- IV prepended to ciphertext (GCM requires a unique IV per encryption)
- Non-sensitive fields (userId, email, name) stored as plaintext — they're not credentials
- Graceful fallback to Base64 when Keystore is unavailable (headless JVM tests)

---

## 5. `domain/` — Business Logic (113 files)

### `domain/model/` — 43 files, pure Kotlin data classes

Every model lives in a feature sub-package; nothing sits at the top level.

```
alerts/AlertModels.kt  InAppNotificationModels.kt  NotificationPreferences.kt
assistant/AssistantModels.kt
auth/AuthSession.kt  InvestmentProfileOptions.kt  User.kt  UserProfile.kt  VerifyResetCodeResult.kt
community/CommunityDomainModels.kt
etf/EtfModels.kt
ipo/IpoModels.kt
live/LiveQuote.kt  MarketSessionStatus.kt  WsConnectionState.kt
market/BenchmarkTrend.kt  CandleEntry.kt  CorporateEvent.kt  IndexConstituents.kt  MarketIndex.kt
market/MarketOverview.kt  MarketStock.kt  SectorPerformance.kt
news/NewsArticle.kt  NewsModels.kt
portfolio/PortfolioModels.kt
recommendations/EngineWeights.kt  RecommendationDetail.kt  StockRecommendation.kt  TargetStop.kt
risk/RiskModels.kt
sentiment/MarketSentiment.kt  SentimentModels.kt
shariah/ShariahDomainModels.kt  ShariahStock.kt
stock/ForecastHistory.kt  StockAnalysis.kt  StockDetail.kt  StockForecast.kt  StockSearchResult.kt
stock/StockTrade.kt  SymbolFilterType.kt
watchlist/WatchlistModels.kt
```

Models expose **computed properties** rather than pushing formatting into Fragments:

```kotlin
data class CommunityPost(… , val likeCount: Int, val commentCount: Int, val createdAt: String) {
    val isStockPost: Boolean
        get() = !stockSymbol.isNullOrBlank() || postType.equals("STOCK", ignoreCase = true)

    val authorDisplayName: String
        get() = if (!authorFullName.isNullOrBlank()) authorFullName
                else if (!authorUsername.isNullOrBlank()) "@$authorUsername"
                else "Trader #${authorId.take(8)}"
}
```

### `domain/repository/` — 20 interface files

Pure contracts, no implementations:

```
AlertsRepository.kt     AssistantRepository.kt   AuthRepository.kt
CommunityRepository.kt  DevicesRepository.kt      EtfRepository.kt
ForecastRepository.kt   IpoRepository.kt          LiveMarketRepository.kt
MarketRepository.kt     NewsRepository.kt         NotificationsRepository.kt
PortfolioRepository.kt  RecommendationsRepository.kt  RiskRepository.kt
SentimentRepository.kt  ShariahRepository.kt      StockRepository.kt
UsersRepository.kt      WatchlistRepository.kt
```

Every method returns `Result<T>` (Kotlin's built-in success/failure wrapper):

```kotlin
interface StockRepository {
    suspend fun getStockOverview(symbol: String): Result<StockDetail>
    suspend fun getPriceHistory(symbol: String, range: String = "1M"): Result<List<CandleEntry>>
    suspend fun getTechnicalIndicators(symbol: String): Result<TechnicalAnalysis>
    suspend fun getFundamentals(symbol: String): Result<FundamentalMetrics>
    // …
}
```

### `domain/usecase/` — 53 files, one class per operation

Grouped by feature: `alerts/` `auth/` `community/` `forecast/` `market/` `news/` `notifications/` `portfolio/` `recommendations/` `risk/` `sentiment/` `shariah/` `stocks/` `user/`. Nothing sits at the top level.

Most are thin pass-throughs, but some hold **real business rules** — e.g. `CreateCommunityPostUseCase` enforces that a Stock Idea post must carry a ticker:

```kotlin
suspend operator fun invoke(content: String, postType: String, stockSymbol: String?): Result<CommunityPost> {
    val validPostType = if (postType.equals("STOCK", ignoreCase = true)) "STOCK" else "GENERAL_MARKET"
    if (validPostType == "STOCK" && stockSymbol.isNullOrBlank()) {
        return Result.failure(IllegalArgumentException("Please specify a stock ticker for Stock Idea posts."))
    }
    return repository.createPost(content, validPostType, stockSymbol)
}
```

`market/BenchmarkTrendAggregator.kt` and `market/BuildBenchmarkTrendUseCase.kt` are pure aggregation logic — ideal unit-test targets.

---

## 6. `data/` — Backend Integration (109 files)

### `data/remote/NetworkClient.kt` — the single Retrofit hub

A Kotlin `object` (singleton) that builds **one OkHttpClient** and **two Retrofit instances**, then lazily exposes all 21 API services.

```kotlin
object NetworkClient {
    const val BASE_URL = "http://193.123.84.223:8000/"
    const val MARKET_BASE_URL = "http://193.123.84.223:8000/"

    private val authInterceptor = AuthInterceptor()
    private val tokenAuthenticator = TokenAuthenticator(BASE_URL)

    private val dispatcher = Dispatcher().apply { maxRequestsPerHost = 12 }

    private val okHttpClient = OkHttpClient.Builder()
        .dispatcher(dispatcher)
        .addInterceptor(authInterceptor)
        .addInterceptor(streamAwareLoggingInterceptor)
        .authenticator(tokenAuthenticator)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL).client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create()).build()

    val stocksApiService: StocksApiService by lazy { marketRetrofit.create(StocksApiService::class.java) }
    // …20 more services
}
```

**Three deliberate engineering decisions:**

1. **`maxRequestsPerHost = 12`** — OkHttp defaults to 5. Portfolio fans out 5 parallel requests, so the default made a single screen's own requests queue behind other screens'.
2. **Per-request log level** — `streamAwareLoggingInterceptor` chooses level by path: `HEADERS` for `/api/v1/devices/` (contains the FCM token), `HEADERS` + 2-minute read timeout for `/api/v1/assistant/chat/stream` (SSE — `BODY` level buffers and breaks token streaming), `BASIC` otherwise. All levels `redactHeader("Authorization")`.
3. **Two Retrofit instances, shared OkHttp client** — currently both point at the same host, but market endpoints can be repointed without touching the rest.

### `data/remote/api/` — 21 interfaces, 121 endpoints

| Interface | Endpoints | Interface | Endpoints |
|---|---|---|---|
| `CommunityApiService` | 20 | `AuthApiService` | 11 |
| `MarketApiService` | 13 | `PortfolioApiService` | 12 |
| `NewsApiService` | 8 | `AssistantApiService` | 8 |
| `StocksApiService` | 5 | `UsersApiService` | 5 |
| `RecommendationsApiService` | 5 | `EtfApiService` | 4 |
| `IpoApiService` | 4 | `RiskApiService` | 4 |
| `SentimentApiService` | 4 | `ShariahApiService` | 4 |
| `AlertsApiService` | 3 | `DevicesApiService` | 2 |
| `ForecastApiService` | 2 | `NotificationsApiService` | 2 |
| `WatchlistApiService` | 4 | `LiveMarketApiService` | 1 (WS) |
| `AuthPayloadRunner.kt` | 0 (test/dev harness) | | |

Every function is `suspend` and returns `Response<T>`:

```kotlin
interface StocksApiService {
    @GET("api/v1/stocks/search")
    suspend fun searchStocks(@Query("q") query: String, @Query("limit") limit: Int = 10)
        : Response<StockSearchResponseDto>

    @GET("api/v1/stocks/{symbol}/overview")
    suspend fun getStockOverview(@Path("symbol") symbol: String): Response<StockOverviewDto>

    @GET("api/v1/stocks/{symbol}/technical-indicators")
    suspend fun getTechnicalIndicators(
        @Path("symbol") symbol: String,
        @Query("indicators") indicators: String = "RSI,MACD,BB,SMA,ADX",
        @Query("period") period: Int = 14,
        @Query("limit") limit: Int = 30
    ): Response<TechnicalIndicatorsResponseDto>
}
```

`suspend` integrates with coroutines (no callbacks). `Response<T>` exposes `isSuccessful`, `code()`, `errorBody()` — which is what powers status-aware error messages.

Annotations in use: `@GET` `@POST` `@PUT` `@DELETE`, `@Path` `@Query` `@Header`, `@Part` (multipart image upload), `@Body`.

### `data/remote/dto/` — 51 files

DTOs mirror the JSON exactly, using `@SerializedName` to map `snake_case` → `camelCase`.

```
alerts/1  assistant/1  auth/2  community/2  etf/1  forecast/2  ipo/1
live/1    market/8     news/1  notifications/2  portfolio/7
recommendations/4       risk/3  sentiment/2  shariah/1  stocks/5
user/6    watchlist/1
```

> **Important pattern:** every field is **nullable with a default** (`val label: String? = null`).
> Gson bypasses Kotlin constructors, so a non-null `String` field the backend omits is `null` at runtime and throws `NullPointerException` on first read. This caused a real crash that was diagnosed and fixed — worth citing as a debugging example.

### `data/mapper/` — 12 files, DTO → domain conversion

```
AlertDtoMapper.kt           AssistantDtoMapper.kt        CommunityDtoMapper.kt
ForecastDtoMapper.kt        NewsDtoMapper.kt             NotificationDtoMapper.kt
PortfolioDtoMapper.kt       RecommendationsDtoMapper.kt  RiskDtoMapper.kt
SentimentDtoMapper.kt       ShariahDtoMapper.kt          StocksDtoMapper.kt
```

Plain `object`s with pure functions — no Android dependency, trivially unit-testable. Null-safe fallbacks live here:

```kotlin
fun mapStockSentiment(dto: SentimentResponseDto): StockSentiment = StockSentiment(
    symbol = dto.symbol.orEmpty(),
    score  = dto.score,
    label  = dto.label ?: "neutral",
    trend  = dto.trend ?: "stable",
    // …
)
```

Mappers also host **derived business calculations**. `ForecastDtoMapper.resolveTargets()` computes a target price and stop loss from direction, horizon and confidence when the backend doesn't supply them.

### `data/repository/` — 21 implementations + 1 cache

```
AlertsRepositoryImpl.kt        AssistantRepositoryImpl.kt     AuthRepositoryImpl.kt
CommunityFeedCache.kt          CommunityRepositoryImpl.kt     DevicesRepositoryImpl.kt
EtfRepositoryImpl.kt           ForecastRepositoryImpl.kt      IpoRepositoryImpl.kt
LiveMarketRepositoryImpl.kt    MarketRepositoryImpl.kt        NewsRepositoryImpl.kt
NotificationsRepositoryImpl.kt PortfolioRepositoryImpl.kt     RecommendationsRepositoryImpl.kt
RiskRepositoryImpl.kt          SentimentRepositoryImpl.kt     ShariahRepositoryImpl.kt
StockRepositoryImpl.kt         UsersRepositoryImpl.kt         WatchlistRepositoryImpl.kt
```

**Every** method follows the same contract: run on `Dispatchers.IO`, return `Result<T>`, never throw.

```kotlin
override suspend fun getStockForecast(symbol: String, horizon: String): Result<StockForecast> =
    withContext(Dispatchers.IO) {
        try {
            val response = apiService.getStockForecast(symbol, horizon)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(ForecastDtoMapper.mapToDomain(it)) }
                    ?: Result.failure(Exception("Forecast response body is empty for $symbol"))
            } else {
                Result.failure(Exception(
                    when (response.code()) {
                        401 -> "Session expired or unauthorized. Please log in."
                        404 -> "Forecast not available for symbol $symbol."
                        503 -> "ML model service temporarily unavailable for $symbol."
                        else -> "Unable to load forecast for $symbol (HTTP ${response.code()})"
                    }
                ))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Network error loading forecast for $symbol"))
        }
    }
```

`CommunityFeedCache.kt` is an in-memory cache so returning to the Community tab doesn't refetch.

### `data/remote/live/LiveMarketDataSource.kt` — WebSocket

Singleton OkHttp WebSocket listener for real-time quotes (`/api/v1/ws/market`). Built with `readTimeout(0)` — no timeout, connection stays open. Exposed through `LiveMarketRepositoryImpl` and shared by all ViewModels.

---

## 7. `feature/` — 20 UI Packages (170 files)

### Convention — 3 files per screen (+ adapters)

```
<Feature>Fragment.kt      UI only: inflate via ViewBinding, wire clicks, render state
<Feature>ViewModel.kt     state + orchestration; calls use cases via viewModelScope
<Feature>UiState.kt       sealed interface: Loading / Success / Empty / Error
adapter/<Name>Adapter.kt  RecyclerView ListAdapter + DiffUtil
```

| Package | Files | Screens |
|---|---|---|
| `alerts` | 4 | AlertsFragment, AlertsViewModel, AlertsUiState, adapter |
| `assistant` | 7 | AssistantChatFragment, AssistantConversationsFragment (+ markdown formatter, 2 adapters) |
| `auth` | 15 | Login, SignUp, ForgotPassword, ResetPassword, VerifyResetCode, EmailVerification (+ `GoogleSignInLauncher`, `OtpInputHelper`) |
| `common` | 1 | `FormatUtils.kt` — shared formatters |
| `community` | 23 | profile, create, follow, notifications, comments, **admin** (hub + report detail) |
| `dashboard` | 4 | HomeFragment (app home screen) |
| `etf` | 8 | EtfDirectoryFragment, EtfDetailFragment |
| `ipo` | 12 | IpoFragment, IpoDirectoryFragment, IpoCalendarFragment, IpoDetailFragment, IpoPerformanceFragment |
| `markets` | 6 | MarketDashboardFragment + 3 adapters |
| `news` | 8 | NewsEventsFragment, detail/NewsArticleDetailFragment |
| `notifications` | 4 | NotificationsFragment |
| `onboarding` | 3 | OnboardingFragment, OnboardingAdapter, OnboardingSlide |
| `portfolio` | 17 | main, holding detail, transactions (history + detail + 2 dialogs) |
| `profile` | 13 | Profile, EditProfile, ChangePassword, NotificationPreferences, RiskProfile |
| `recommendation` | 13 | list, detail, settings, **smartselection** |
| `risk` | 5 | RiskAnalyticsFragment (VaR, Monte Carlo) |
| `sentiment` | 4 | SentimentFragment |
| `shariah` | 8 | ShariahScreeningFragment, details bottom sheet |
| `splash` | 1 | SplashFragment |
| `stock` | 14 | detail, analysis, chart fullscreen, search |

### UI State pattern

State is a **sealed hierarchy**, never a bag of booleans. Note the section-level sub-states:

```kotlin
sealed interface StockAnalysisUiState {
    data object Loading : StockAnalysisUiState
    data object Empty : StockAnalysisUiState
    data class Error(val message: String) : StockAnalysisUiState
    data class Success(
        val analysis: StockAnalysis,
        val selectedTab: AnalysisTab,
        val priceHistoryState: PriceHistorySectionState,
        val forecastState: ForecastSectionState,
        val sentimentState: StockSentimentSectionState
    ) : StockAnalysisUiState
}
```

This is why the Sentiment tab can show an error while Technical still renders — each section has its own independent state.

### ViewModel pattern

```kotlin
class StockSearchViewModel(
    private val repository: StockRepository = StockRepositoryImpl(),   // manual DI
    private val searchStocksUseCase: SearchStocksUseCase = SearchStocksUseCase(repository)
) : ViewModel() {

    private val _uiState = MutableStateFlow<StockSearchUiState>(StockSearchUiState.Idle)
    val uiState: StateFlow<StockSearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            queryFlow
                .debounce(300)           // don't call the API per keystroke
                .distinctUntilChanged()   // skip duplicate queries
                .collectLatest { executeSearch(it) }  // cancel in-flight on new input
        }
    }
}
```

Manual constructor injection (no Hilt) is what makes ViewModels unit-testable with fakes.

### Fragment pattern

```kotlin
class StockAnalysisFragment : Fragment() {
    private var _binding: FragmentStockAnalysisBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StockAnalysisViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerViews(); setupClickListeners(); observeUiState()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {  // lifecycle-safe
                viewModel.uiState.collect { state -> when (state) { … } }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }  // no leak
}
```

---

## 8. Data Flow — End to End

```
 ① User taps "LUCK" on Markets screen
        ↓
 ② MarketDashboardFragment → navigate(R.id.stockDetailFragment, bundleOf("symbol" to "LUCK"))
        ↓
 ③ StockDetailFragment.onViewCreated() → viewModel.load("LUCK")
        ↓
 ④ StockDetailViewModel.load() → viewModelScope.launch { }
        ↓
 ⑤ GetStockOverviewUseCase("LUCK")
        ↓
 ⑥ StockRepository.getStockOverview("LUCK")            ← interface, domain/
        ↓
 ⑦ StockRepositoryImpl.getStockOverview("LUCK")         ← impl, data/repository/
        ↓
 ⑧ NetworkClient.stocksApiService.getStockOverview("LUCK")
        ↓
 ⑨ AuthInterceptor adds "Authorization: Bearer <access_token>"
        ↓
═════════ HTTP GET http://193.123.84.223:8000/api/v1/stocks/LUCK/overview ═════════
        ↓
 ⑩ Gson deserializes JSON → StockOverviewDto           ← data/remote/dto/stocks/
        ↓
 ⑪ StocksDtoMapper.mapOverviewToStockDetail() → StockDetail   ← domain model, null-safe
        ↓
 ⑫ Repository wraps in Result.success(StockDetail)
        ↓
 ⑬ ViewModel emits StateFlow<StockDetailUiState.Success>
        ↓
 ⑭ Fragment collects (repeatOnLifecycle STARTED) → bindDetail(state.detail)
        ↓
 ⑮ binding.tvPrice.text = String.format("PKR %.2f", detail.price)
```

**On 401 anywhere in that chain:** `TokenAuthenticator` intercepts, refreshes via `/api/v1/auth/refresh`, updates `SessionManager`, and transparently replays the original request. The user never sees an error.

---

## 9. Resources (`app/src/main/res/`)

| Folder | Files | Contents |
|---|---|---|
| `layout/` | 94 | Screens (`fragment_*.xml`), dialogs (`bottom_sheet_*.xml`), rows (`item_*.xml`), partials (`content_*.xml`) |
| `drawable/` | 164 | Vector icons (`ic_*.xml`), backgrounds (`bg_*.xml`), badges, skeletons |
| `navigation/` | 1 | `nav_graph.xml` — **45 destinations, 72 actions** |
| `values/` | 5 | `strings.xml`, `colors.xml`, `themes.xml`, `styles`, type-scale `TextAppearance.*` |
| `values-night/` | 2 | **Dark-mode overrides** for colors + themes |
| `menu/` | 2 | `bottom_nav_menu.xml`, `drawer_menu.xml` |
| `color/` | 3 | State-list colour selectors (e.g. `nav_item_color`, `drawer_item_tint`) |
| `xml/` | 2 | Backup rules, data-extraction rules |
| `mipmap-*` | 17 | Launcher icons at 5 densities + adaptive icon |

### Layout naming convention

| Prefix | Meaning |
|---|---|
| `fragment_*` | Full screen, one per Fragment |
| `bottom_sheet_*` | `BottomSheetDialogFragment` |
| `item_*` | RecyclerView row, one per adapter |
| `content_*` | Reusable partial inflated into a parent |

### Dark mode

Resource-qualifier based, not programmatic:

```xml
<!-- values/colors.xml -->        <color name="color_text_primary">#111827</color>
<!-- values-night/colors.xml -->  <color name="color_text_primary">#F8FAFC</color>
```

Any drawable must reference `@color/...`, never a hardcoded hex, or it will be invisible in one mode. (`ic_back_arrow` originally hardcoded `#FF000000` — a real bug fixed this session.)

### Typography

Shared styles applied across layouts: `TextAppearance.Basarat.Title`, `.Body`, `.Body.Strong`, `.Body.Small`, `.Body.Small.Strong`, `.Meta`, `.Meta.Strong`.

### Navigation

Single `NavHostFragment` in `activity_main.xml` hosting all 45 destinations. `MainActivity` owns:
- the `DrawerLayout` (13 items, navigates generically via `item.itemId`)
- the `BottomNavigationView` (`setupWithNavController`)

---

## 10. App Entry Points

`AndroidManifest.xml`: one Activity, one Service.

```xml
<application android:name=".BasaratApplication" …>
    <service android:name=".core.notifications.BasaratFirebaseService" … />
    <activity android:name=".MainActivity" android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```

`BasaratApplication.onCreate()` — restores the encrypted session and creates notification channels:

```kotlin
class BasaratApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
        NotificationChannels.create(this)
    }
}
```

---

## 11. Tests (25 files, `app/src/test/`)

| Category | Files |
|---|---|
| Mapper / DTO tests | `StocksModuleUnitTest`, `SentimentModuleUnitTest`, `ForecastModuleUnitTest`, `MarketModuleUnitTest`, `NewsDtoDeserializationTest`, `LiveMarketProtocolDtoTest`, `MarketSessionStatusTest`, `PortfolioModuleUnitTest`, `RiskModuleUnitTest`, `ShariahModuleUnitTest`, `RecommendationsModuleUnitTest`, `AssistantModuleUnitTest`, `AlertsModuleUnitTest`, `NotificationsModuleUnitTest`, `UsersModuleUnitTest` |
| Auth tests | `AuthModuleUnitTest` |
| Community tests | `CommunityModuleUnitTest`, `AdminCommunityUnitTest`, `CreateCommunityPostUseCaseTest`, `CommunityFeedCacheTest` |
| ViewModel tests | `HomeViewModelUnitTest`, `StockDetailViewModelTest` |
| UI state tests | `HomeUiStateTest`, `MarketDashboardUiStateTest` |

All run on the **plain JVM** — no emulator — because `domain/` has no Android dependencies.

**Status: 136 tests, 4 failing** (verified pre-existing, not regressions):
- `HomeViewModelUnitTest > testHomeViewModelStartsInLoadingState`
- `StockDetailViewModelTest > recommendation card keeps the same symbol state across price updates`
- `NewsDtoDeserializationTest > testDeserializeNewsWithStringSentiment`
- `NewsDtoDeserializationTest > testDeserializeNewsWithObjectSentiment`

---

## 12. Architecture Rules the Codebase Follows

From `.github/copilot-instructions.md`:

1. **No mock financial data.** Prices, RSI, sentiment, forecasts — if the backend doesn't provide it, show `N/A` or an empty state. Never fabricate.
2. **Data flows one direction:** Backend → DTO → Domain Model → Repository → ViewModel → StateFlow → XML.
3. **Never bypass the repository layer** from a Fragment.
4. **Clean UI states** (`Loading`/`Success`/`Empty`/`Error`), not boolean flag combinations.
5. **Reuse the single Retrofit client** — don't create another one or a second auth system.
6. **Verify against Swagger** before adding an endpoint; never invent response shapes.
7. **Fragments contain no business logic** — display, collect state, handle interaction, navigate.
8. **Don't log tokens** — redact `Authorization`, `Cookie`, `Set-Cookie` in all interceptors.

---

## 13. Viva Talking Points

Ranked by impact:

1. **Clean Architecture dependency rule** — `domain/` has zero Android imports, proven by tests running on a bare JVM.
2. **Token refresh correctness** — `synchronized` + already-refreshed check + `responseCount >= 2` prevent refresh stampedes and infinite loops.
3. **Hardware-backed security** — AES-256-GCM with the key in Android Keystore, never leaving secure hardware.
4. **The Gson null-trap** — Kotlin non-null types are a compile-time illusion; Gson bypasses constructors, so absent JSON becomes a runtime NPE. A genuine bug you diagnosed and fixed.
5. **SSE vs `Level.BODY` logging** — body logging buffers and breaks token streaming. Shows transport-level understanding.
6. **`debounce` + `collectLatest`** — correct coroutine hygiene for search-as-you-type.
7. **Per-section rebinding** — avoiding redundant chart rebuilds and entry-animation restarts.
8. **`Result<T>` everywhere** — 135 usages; no exceptions escape into the UI layer.
9. **OkHttp concurrency tuning** — `maxRequestsPerHost = 12` because one screen fans out 5 requests.
10. **`values-night` resource qualifiers** — dark mode without a line of Kotlin.