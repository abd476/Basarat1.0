# Design Document — IPO & ETF Integration + Market Dashboard Chip Navigation

## Overview

This design covers the full implementation of IPO and ETF data integration into the BASARAT Android application, plus the chip navigation wiring on `MarketDashboardFragment`. The feature adds two complete asset-class flows — IPOs (with Directory, Calendar, and Performance tabs) and ETFs (with Directory and a tabbed Detail screen) — all sourced from live BASARAT backend endpoints.

The implementation strictly follows the existing BASARAT architecture: MVVM, Repository pattern, `StateFlow`-based UI state, `ListAdapter`/`DiffUtil`, and ViewBinding, as established by existing features like `MarketDashboardFragment` and `StockDetailFragment`.

---

## Architecture

### Layer Overview

```
Fragment (XML ViewBinding)
       ↓  collect StateFlow
ViewModel (viewModelScope)
       ↓  suspend fun
Repository (interface + Impl)
       ↓  Retrofit Response<DTO>
ApiService (Retrofit interface)
       ↓  HTTP
BASARAT Backend (/api/v1/ipos, /api/v1/etfs)
```

Each layer is isolated: Fragments bind UI only; ViewModels manage state transitions; Repositories handle data access and map DTOs to domain models; API services declare HTTP contracts.

### Package Layout

```
data/remote/api/
    IpoApiService.kt
    EtfApiService.kt
data/remote/dto/ipo/
    IpoDtos.kt
data/remote/dto/etf/
    EtfDtos.kt
data/remote/
    NetworkClient.kt          (modified — add lazy properties)
data/repository/
    IpoRepositoryImpl.kt
    EtfRepositoryImpl.kt
domain/model/
    IpoModels.kt              (domain models + .toDomain() mappers)
    EtfModels.kt
domain/repository/
    IpoRepository.kt
    EtfRepository.kt
feature/ipo/
    IpoFragment.kt            (ViewPager2 tab host)
    IpoUiState.kt
    IpoViewModel.kt
    IpoDetailFragment.kt
    IpoDetailUiState.kt
    IpoDetailViewModel.kt
    adapter/
        IpoListAdapter.kt
        IpoCalendarAdapter.kt
        IpoPerformanceAdapter.kt
feature/etf/
    EtfDirectoryFragment.kt
    EtfDirectoryUiState.kt
    EtfDirectoryViewModel.kt
    EtfDetailFragment.kt
    EtfDetailUiState.kt
    EtfDetailViewModel.kt
    adapter/
        EtfListAdapter.kt
res/layout/
    fragment_ipo_directory.xml   (replaces empty file)
    fragment_ipo_detail.xml
    fragment_etf_directory.xml
    fragment_etf_detail.xml
    item_ipo_card.xml
    item_ipo_calendar_event.xml
    item_ipo_performance.xml
    item_etf_card.xml
res/navigation/
    nav_graph.xml                (modified — add 4 destinations + actions)
res/layout/
    fragment_market_dashboard.xml (modified — add IDs to 2 chips)
feature/markets/
    MarketDashboardFragment.kt   (modified — wire chip listeners)
```

---

## Components

### 1. API Services

#### `IpoApiService`

Retrofit interface for all `/api/v1/ipos` endpoints. Declared with `suspend` functions returning `Response<T>` to allow the repository to inspect HTTP status codes before mapping.

```kotlin
interface IpoApiService {
    @GET("api/v1/ipos")
    suspend fun getIpos(): Response<IposResponseDto>

    @GET("api/v1/ipos/{symbol}")
    suspend fun getIpoDetail(@Path("symbol") symbol: String): Response<IpoItemDto>

    @GET("api/v1/ipos/calendar")
    suspend fun getIpoCalendar(): Response<IpoCalendarResponseDto>

    @GET("api/v1/ipos/performance")
    suspend fun getIpoPerformance(): Response<IpoPerformanceResponseDto>
}
```

#### `EtfApiService`

```kotlin
interface EtfApiService {
    @GET("api/v1/etfs")
    suspend fun getEtfs(): Response<EtfsResponseDto>

    @GET("api/v1/etfs/{symbol}")
    suspend fun getEtfDetail(@Path("symbol") symbol: String): Response<EtfItemDto>

    @GET("api/v1/etfs/{symbol}/history")
    suspend fun getEtfHistory(
        @Path("symbol") symbol: String,
        @Query("timeframe") timeframe: String? = null
    ): Response<EtfHistoryResponseDto>

    @GET("api/v1/etfs/{symbol}/performance")
    suspend fun getEtfPerformance(@Path("symbol") symbol: String): Response<EtfPerformanceDto>
}
```

---

### 2. DTOs

All DTOs use `@SerializedName` annotations matching the exact backend field names. They are plain data classes with nullable types for any field that may be absent.

#### `IpoDtos.kt`

```kotlin
data class IpoItemDto(
    @SerializedName("id") val id: String?,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("company_name") val companyName: String?,
    @SerializedName("sector") val sector: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("issue_size_shares") val issueSizeShares: Long?,
    @SerializedName("issue_size_pkr") val issueSizePkr: Double?,
    @SerializedName("floor_price") val floorPrice: Double?,
    @SerializedName("strike_price") val strikePrice: Double?,
    @SerializedName("listing_price") val listingPrice: Double?,
    @SerializedName("current_price") val currentPrice: Double?,
    @SerializedName("book_building_start") val bookBuildingStart: String?,
    @SerializedName("book_building_end") val bookBuildingEnd: String?,
    @SerializedName("public_subscription_start") val publicSubscriptionStart: String?,
    @SerializedName("public_subscription_end") val publicSubscriptionEnd: String?,
    @SerializedName("listing_date") val listingDate: String?,
    @SerializedName("lead_manager") val leadManager: String?,
    @SerializedName("is_shariah_compliant") val isShariahCompliant: Boolean?,
    @SerializedName("prospectus_url") val prospectusUrl: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("subscription_multiplier") val subscriptionMultiplier: Double?,
    @SerializedName("listing_gain_pct") val listingGainPct: Double?,
    @SerializedName("current_gain_pct") val currentGainPct: Double?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class IposResponseDto(
    @SerializedName("total") val total: Int?,
    @SerializedName("upcoming_count") val upcomingCount: Int?,
    @SerializedName("active_count") val activeCount: Int?,
    @SerializedName("listed_count") val listedCount: Int?,
    @SerializedName("ipos") val ipos: List<IpoItemDto>?
)

data class IpoCalendarEventDto(
    @SerializedName("ipo_id") val ipoId: String?,
    @SerializedName("symbol") val symbol: String?,
    @SerializedName("company_name") val companyName: String?,
    @SerializedName("event_type") val eventType: String?,
    @SerializedName("event_date") val eventDate: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("price_info") val priceInfo: String?
)

data class IpoCalendarResponseDto(
    @SerializedName("total_events") val totalEvents: Int?,
    @SerializedName("upcoming_milestones") val upcomingMilestones: List<IpoCalendarEventDto>?
)

data class IpoPerformanceItemDto(
    @SerializedName("symbol") val symbol: String?,
    @SerializedName("company_name") val companyName: String?,
    @SerializedName("sector") val sector: String?,
    @SerializedName("listing_date") val listingDate: String?,
    @SerializedName("offer_price") val offerPrice: Double?,
    @SerializedName("first_day_close") val firstDayClose: Double?,
    @SerializedName("current_price") val currentPrice: Double?,
    @SerializedName("first_day_return_pct") val firstDayReturnPct: Double?,
    @SerializedName("total_return_pct") val totalReturnPct: Double?
)

data class IpoPerformanceResponseDto(
    @SerializedName("total_listed") val totalListed: Int?,
    @SerializedName("average_listing_day_gain_pct") val averageListingDayGainPct: Double?,
    @SerializedName("top_performers") val topPerformers: List<IpoPerformanceItemDto>?,
    @SerializedName("recent_listings") val recentListings: List<IpoPerformanceItemDto>?
)
```

#### `EtfDtos.kt`

```kotlin
data class EtfQuoteDto(
    @SerializedName("current_price") val currentPrice: Double?,
    @SerializedName("change") val change: Double?,
    @SerializedName("change_percent") val changePercent: Double?,
    @SerializedName("open_price") val openPrice: Double?,
    @SerializedName("high_price") val highPrice: Double?,
    @SerializedName("low_price") val lowPrice: Double?,
    @SerializedName("close_price") val closePrice: Double?,
    @SerializedName("volume") val volume: Long?,
    @SerializedName("bid_price") val bidPrice: Double?,
    @SerializedName("ask_price") val askPrice: Double?,
    @SerializedName("data_as_of") val dataAsOf: String?,
    @SerializedName("quote_source") val quoteSource: String?
)

data class EtfItemDto(
    @SerializedName("id") val id: String?,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("name") val name: String?,
    @SerializedName("fund_manager") val fundManager: String?,
    @SerializedName("category") val category: String?,
    @SerializedName("benchmark_index") val benchmarkIndex: String?,
    @SerializedName("is_shariah_compliant") val isShariahCompliant: Boolean?,
    @SerializedName("expense_ratio") val expenseRatio: Double?,
    @SerializedName("inception_date") val inceptionDate: String?,
    @SerializedName("total_assets_pkr") val totalAssetsPkr: Double?,
    @SerializedName("description") val description: String?,
    @SerializedName("is_active") val isActive: Boolean?,
    @SerializedName("quote") val quote: EtfQuoteDto?,
    @SerializedName("current_price") val currentPrice: Double?,
    @SerializedName("change") val change: Double?,
    @SerializedName("change_percent") val changePercent: Double?,
    @SerializedName("volume") val volume: Long?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

data class EtfsResponseDto(
    @SerializedName("total") val total: Int?,
    @SerializedName("shariah_count") val shariahCount: Int?,
    @SerializedName("conventional_count") val conventionalCount: Int?,
    @SerializedName("etfs") val etfs: List<EtfItemDto>?
)

data class EtfHistoryEntryDto(
    @SerializedName("date") val date: String?,
    @SerializedName("open") val open: Double?,
    @SerializedName("high") val high: Double?,
    @SerializedName("low") val low: Double?,
    @SerializedName("close") val close: Double?,
    @SerializedName("volume") val volume: Long?
)

data class EtfHistoryResponseDto(
    @SerializedName("symbol") val symbol: String?,
    @SerializedName("timeframe") val timeframe: String?,
    @SerializedName("count") val count: Int?,
    @SerializedName("history") val history: List<EtfHistoryEntryDto>?
)

data class EtfReturnsDto(
    @SerializedName("1D") val oneDay: Double?,
    @SerializedName("1W") val oneWeek: Double?,
    @SerializedName("1M") val oneMonth: Double?,
    @SerializedName("3M") val threeMonths: Double?,
    @SerializedName("1Y") val oneYear: Double?,
    @SerializedName("YTD") val ytd: Double?
)

data class EtfPerformanceDto(
    @SerializedName("symbol") val symbol: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("benchmark_index") val benchmarkIndex: String?,
    @SerializedName("returns") val returns: EtfReturnsDto?,
    @SerializedName("benchmark_returns") val benchmarkReturns: EtfReturnsDto?,
    @SerializedName("tracking_difference_1m") val trackingDifference1m: Double?,
    @SerializedName("volatility_annualized") val volatilityAnnualized: Double?
)
```

---

### 3. Domain Models and Mappers

Domain models use Kotlin-idiomatic camelCase property names. Each file contains the data classes and a `.toDomain()` extension function for the corresponding DTO.

#### `IpoModels.kt`

```kotlin
data class IpoItem(
    val id: String?,
    val symbol: String,
    val companyName: String,
    val sector: String?,
    val status: String?,
    val issueSizeShares: Long?,
    val issueSizePkr: Double?,
    val floorPrice: Double?,
    val strikePrice: Double?,
    val listingPrice: Double?,
    val currentPrice: Double?,
    val bookBuildingStart: String?,
    val bookBuildingEnd: String?,
    val publicSubscriptionStart: String?,
    val publicSubscriptionEnd: String?,
    val listingDate: String?,
    val leadManager: String?,
    val isShariahCompliant: Boolean,
    val prospectusUrl: String?,
    val description: String?,
    val subscriptionMultiplier: Double?,
    val listingGainPct: Double?,
    val currentGainPct: Double?,
    val createdAt: String?,
    val updatedAt: String?
)

fun IpoItemDto.toDomain(): IpoItem = IpoItem(
    id = id,
    symbol = symbol,
    companyName = companyName ?: symbol,
    sector = sector,
    status = status,
    issueSizeShares = issueSizeShares,
    issueSizePkr = issueSizePkr,
    floorPrice = floorPrice,
    strikePrice = strikePrice,
    listingPrice = listingPrice,
    currentPrice = currentPrice,
    bookBuildingStart = bookBuildingStart,
    bookBuildingEnd = bookBuildingEnd,
    publicSubscriptionStart = publicSubscriptionStart,
    publicSubscriptionEnd = publicSubscriptionEnd,
    listingDate = listingDate,
    leadManager = leadManager,
    isShariahCompliant = isShariahCompliant ?: false,
    prospectusUrl = prospectusUrl,
    description = description,
    subscriptionMultiplier = subscriptionMultiplier,
    listingGainPct = listingGainPct,
    currentGainPct = currentGainPct,
    createdAt = createdAt,
    updatedAt = updatedAt
)

data class IpoCalendarEvent(
    val ipoId: String?,
    val symbol: String,
    val companyName: String,
    val eventType: String,
    val eventDate: String?,
    val status: String?,
    val priceInfo: String?
)

fun IpoCalendarEventDto.toDomain(): IpoCalendarEvent = IpoCalendarEvent(
    ipoId = ipoId,
    symbol = symbol ?: "",
    companyName = companyName ?: symbol ?: "",
    eventType = eventType ?: "",
    eventDate = eventDate,
    status = status,
    priceInfo = priceInfo
)

data class IpoPerformanceItem(
    val symbol: String,
    val companyName: String,
    val sector: String?,
    val listingDate: String?,
    val offerPrice: Double?,
    val firstDayClose: Double?,
    val currentPrice: Double?,
    val firstDayReturnPct: Double?,
    val totalReturnPct: Double?
)

fun IpoPerformanceItemDto.toDomain(): IpoPerformanceItem = IpoPerformanceItem(
    symbol = symbol ?: "",
    companyName = companyName ?: symbol ?: "",
    sector = sector,
    listingDate = listingDate,
    offerPrice = offerPrice,
    firstDayClose = firstDayClose,
    currentPrice = currentPrice,
    firstDayReturnPct = firstDayReturnPct,
    totalReturnPct = totalReturnPct
)

data class IpoPerformanceSummary(
    val totalListed: Int,
    val averageListingDayGainPct: Double?,
    val topPerformers: List<IpoPerformanceItem>,
    val recentListings: List<IpoPerformanceItem>
)
```

#### `EtfModels.kt`

```kotlin
data class EtfQuote(
    val currentPrice: Double?,
    val change: Double?,
    val changePercent: Double?,
    val openPrice: Double?,
    val highPrice: Double?,
    val lowPrice: Double?,
    val closePrice: Double?,
    val volume: Long?,
    val bidPrice: Double?,
    val askPrice: Double?,
    val dataAsOf: String?,
    val quoteSource: String?
)

fun EtfQuoteDto.toDomain(): EtfQuote = EtfQuote(
    currentPrice = currentPrice,
    change = change,
    changePercent = changePercent,
    openPrice = openPrice,
    highPrice = highPrice,
    lowPrice = lowPrice,
    closePrice = closePrice,
    volume = volume,
    bidPrice = bidPrice,
    askPrice = askPrice,
    dataAsOf = dataAsOf,
    quoteSource = quoteSource
)

data class EtfItem(
    val id: String?,
    val symbol: String,
    val name: String,
    val fundManager: String?,
    val category: String?,
    val benchmarkIndex: String?,
    val isShariahCompliant: Boolean,
    val expenseRatio: Double?,
    val inceptionDate: String?,
    val totalAssetsPkr: Double?,
    val description: String?,
    val isActive: Boolean,
    val quote: EtfQuote?,
    val currentPrice: Double?,
    val change: Double?,
    val changePercent: Double?,
    val volume: Long?,
    val createdAt: String?,
    val updatedAt: String?
)

fun EtfItemDto.toDomain(): EtfItem = EtfItem(
    id = id,
    symbol = symbol,
    name = name ?: symbol,
    fundManager = fundManager,
    category = category,
    benchmarkIndex = benchmarkIndex,
    isShariahCompliant = isShariahCompliant ?: false,
    expenseRatio = expenseRatio,
    inceptionDate = inceptionDate,
    totalAssetsPkr = totalAssetsPkr,
    description = description,
    isActive = isActive ?: true,
    quote = quote?.toDomain(),
    currentPrice = currentPrice ?: quote?.currentPrice,
    change = change ?: quote?.change,
    changePercent = changePercent ?: quote?.changePercent,
    volume = volume ?: quote?.volume,
    createdAt = createdAt,
    updatedAt = updatedAt
)

data class EtfHistoryEntry(
    val date: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

fun EtfHistoryEntryDto.toDomain(): EtfHistoryEntry = EtfHistoryEntry(
    date = date ?: "",
    open = open ?: 0.0,
    high = high ?: 0.0,
    low = low ?: 0.0,
    close = close ?: 0.0,
    volume = volume ?: 0L
)

data class EtfReturns(
    val oneDay: Double?,
    val oneWeek: Double?,
    val oneMonth: Double?,
    val threeMonths: Double?,
    val oneYear: Double?,
    val ytd: Double?
)

fun EtfReturnsDto.toDomain(): EtfReturns = EtfReturns(
    oneDay = oneDay,
    oneWeek = oneWeek,
    oneMonth = oneMonth,
    threeMonths = threeMonths,
    oneYear = oneYear,
    ytd = ytd
)

data class EtfPerformance(
    val symbol: String,
    val name: String?,
    val benchmarkIndex: String?,
    val returns: EtfReturns?,
    val benchmarkReturns: EtfReturns?,
    val trackingDifference1m: Double?,
    val volatilityAnnualized: Double?
)

fun EtfPerformanceDto.toDomain(): EtfPerformance = EtfPerformance(
    symbol = symbol ?: "",
    name = name,
    benchmarkIndex = benchmarkIndex,
    returns = returns?.toDomain(),
    benchmarkReturns = benchmarkReturns?.toDomain(),
    trackingDifference1m = trackingDifference1m,
    volatilityAnnualized = volatilityAnnualized
)
```

---

### 4. Repository Interfaces

#### `IpoRepository.kt`

```kotlin
interface IpoRepository {
    suspend fun getIpos(): Result<List<IpoItem>>
    suspend fun getIpoDetail(symbol: String): Result<IpoItem>
    suspend fun getIpoCalendar(): Result<List<IpoCalendarEvent>>
    suspend fun getIpoPerformance(): Result<IpoPerformanceSummary>
}
```

#### `EtfRepository.kt`

```kotlin
interface EtfRepository {
    suspend fun getEtfs(): Result<EtfsResult>           // wraps total/shariahCount/conventionalCount + list
    suspend fun getEtfDetail(symbol: String): Result<EtfItem>
    suspend fun getEtfHistory(symbol: String, timeframe: String): Result<List<EtfHistoryEntry>>
    suspend fun getEtfPerformance(symbol: String): Result<EtfPerformance>
}

data class EtfsResult(
    val total: Int,
    val shariahCount: Int,
    val conventionalCount: Int,
    val etfs: List<EtfItem>
)
```

---

### 5. Repository Implementations

Both implementations follow the same pattern as `MarketRepositoryImpl` and `WatchlistRepositoryImpl`:
- Accept the API service as a constructor parameter defaulting to the `NetworkClient` lazy property.
- Run on `Dispatchers.IO` via `withContext`.
- Return `Result.success` or `Result.failure` with a human-readable message from `NetworkErrorParser.parseError(response)`.
- Re-throw `CancellationException` to respect structured concurrency.

```kotlin
class IpoRepositoryImpl(
    private val api: IpoApiService = NetworkClient.ipoApiService
) : IpoRepository {

    override suspend fun getIpos(): Result<List<IpoItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getIpos()
            if (response.isSuccessful && response.body() != null) {
                val items = response.body()!!.ipos?.map { it.toDomain() } ?: emptyList()
                Result.success(items)
            } else {
                Result.failure(Exception(NetworkErrorParser.parseError(response)))
            }
        } catch (e: CancellationException) { throw e }
          catch (e: Exception) { Result.failure(e) }
    }

    // getIpoDetail, getIpoCalendar, getIpoPerformance follow the same pattern
}

class EtfRepositoryImpl(
    private val api: EtfApiService = NetworkClient.etfApiService
) : EtfRepository { /* same pattern */ }
```

---

### 6. NetworkClient Additions

Two new `lazy` properties are added to the existing `NetworkClient` singleton, both created from the existing `marketRetrofit` instance — no new OkHttp or Retrofit client is created:

```kotlin
val ipoApiService: IpoApiService by lazy {
    marketRetrofit.create(IpoApiService::class.java)
}

val etfApiService: EtfApiService by lazy {
    marketRetrofit.create(EtfApiService::class.java)
}

val ipoRepository: IpoRepository by lazy {
    IpoRepositoryImpl(ipoApiService)
}

val etfRepository: EtfRepository by lazy {
    EtfRepositoryImpl(etfApiService)
}
```

---

### 7. UI State Models

#### `IpoUiState.kt`

```kotlin
sealed interface IpoDirectoryUiState {
    data object Loading : IpoDirectoryUiState
    data class Success(val items: List<IpoItem>) : IpoDirectoryUiState
    data object Empty : IpoDirectoryUiState
    data class Error(val message: String) : IpoDirectoryUiState
}

sealed interface IpoCalendarUiState {
    data object Loading : IpoCalendarUiState
    data class Success(val events: List<IpoCalendarEvent>) : IpoCalendarUiState
    data object Empty : IpoCalendarUiState
    data class Error(val message: String) : IpoCalendarUiState
}

sealed interface IpoPerformanceUiState {
    data object Loading : IpoPerformanceUiState
    data class Success(val summary: IpoPerformanceSummary) : IpoPerformanceUiState
    data object Empty : IpoPerformanceUiState
    data class Error(val message: String) : IpoPerformanceUiState
}
```

#### `IpoDetailUiState.kt`

```kotlin
sealed interface IpoDetailUiState {
    data object Loading : IpoDetailUiState
    data class Success(val ipo: IpoItem) : IpoDetailUiState
    data class Error(val message: String) : IpoDetailUiState
}
```

#### `EtfDirectoryUiState.kt`

```kotlin
sealed interface EtfDirectoryUiState {
    data object Loading : EtfDirectoryUiState
    data class Success(
        val total: Int,
        val shariahCount: Int,
        val conventionalCount: Int,
        val etfs: List<EtfItem>
    ) : EtfDirectoryUiState
    data object Empty : EtfDirectoryUiState
    data class Error(val message: String) : EtfDirectoryUiState
}
```

#### `EtfDetailUiState.kt`

```kotlin
sealed interface EtfDetailUiState {
    data object Loading : EtfDetailUiState
    data class Success(
        val etf: EtfItem,
        val selectedTab: EtfDetailTab = EtfDetailTab.OVERVIEW,
        val historyState: EtfHistoryUiState = EtfHistoryUiState.Loading,
        val performanceState: EtfPerformanceUiState = EtfPerformanceUiState.Loading,
        val selectedTimeframe: String = "1M"
    ) : EtfDetailUiState
    data class Error(val message: String) : EtfDetailUiState
}

enum class EtfDetailTab { OVERVIEW, HISTORY, PERFORMANCE }

sealed interface EtfHistoryUiState {
    data object Loading : EtfHistoryUiState
    data class Success(val entries: List<EtfHistoryEntry>) : EtfHistoryUiState
    data object Empty : EtfHistoryUiState
    data class Error(val message: String) : EtfHistoryUiState
}

sealed interface EtfPerformanceUiState {
    data object Loading : EtfPerformanceUiState
    data class Success(val performance: EtfPerformance) : EtfPerformanceUiState
    data class Error(val message: String) : EtfPerformanceUiState
}
```

---

### 8. ViewModels

#### `IpoViewModel`

Scoped to `IpoFragment` and shared by child tab Fragments via `by viewModels({ requireParentFragment() })`. Holds three independent state flows — one per tab. Loading is triggered on `init`; retry methods are exposed for each tab.

```kotlin
class IpoViewModel(
    private val repository: IpoRepository = NetworkClient.ipoRepository
) : ViewModel() {
    private val _directoryState = MutableStateFlow<IpoDirectoryUiState>(IpoDirectoryUiState.Loading)
    val directoryState: StateFlow<IpoDirectoryUiState> = _directoryState.asStateFlow()

    private val _calendarState = MutableStateFlow<IpoCalendarUiState>(IpoCalendarUiState.Loading)
    val calendarState: StateFlow<IpoCalendarUiState> = _calendarState.asStateFlow()

    private val _performanceState = MutableStateFlow<IpoPerformanceUiState>(IpoPerformanceUiState.Loading)
    val performanceState: StateFlow<IpoPerformanceUiState> = _performanceState.asStateFlow()

    init {
        loadDirectory()
        loadCalendar()
        loadPerformance()
    }

    fun loadDirectory() { /* viewModelScope.launch { ... } */ }
    fun loadCalendar() { /* viewModelScope.launch { ... } */ }
    fun loadPerformance() { /* viewModelScope.launch { ... } */ }
}
```

#### `IpoDetailViewModel`

Scoped to `IpoDetailFragment`. Receives the symbol via `savedStateHandle` or explicit setter.

```kotlin
class IpoDetailViewModel(
    private val repository: IpoRepository = NetworkClient.ipoRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<IpoDetailUiState>(IpoDetailUiState.Loading)
    val uiState: StateFlow<IpoDetailUiState> = _uiState.asStateFlow()

    fun load(symbol: String) { /* viewModelScope.launch { ... } */ }
}
```

#### `EtfDirectoryViewModel`

```kotlin
class EtfDirectoryViewModel(
    private val repository: EtfRepository = NetworkClient.etfRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<EtfDirectoryUiState>(EtfDirectoryUiState.Loading)
    val uiState: StateFlow<EtfDirectoryUiState> = _uiState.asStateFlow()

    init { load() }
    fun load() { /* viewModelScope.launch { ... } */ }
}
```

#### `EtfDetailViewModel`

Manages the tab state internally. Fetching overview happens on `loadDetail(symbol)`; history and performance are fetched lazily when the user switches to those tabs (or eagerly on first tab switch).

```kotlin
class EtfDetailViewModel(
    private val repository: EtfRepository = NetworkClient.etfRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<EtfDetailUiState>(EtfDetailUiState.Loading)
    val uiState: StateFlow<EtfDetailUiState> = _uiState.asStateFlow()

    fun loadDetail(symbol: String) { /* viewModelScope.launch { ... } */ }
    fun selectTab(tab: EtfDetailTab) { /* updates selectedTab; lazily loads if needed */ }
    fun selectTimeframe(symbol: String, timeframe: String) { /* re-fetches history */ }
    fun retryPerformance(symbol: String) { /* re-fetches performance */ }
}
```

---

### 9. List Adapters

All adapters extend `ListAdapter<T, VH>` with `DiffUtil.ItemCallback` keyed on `symbol` (string equality). This matches the pattern in `MarketStockAdapter` and `ConstituentAdapter`.

```kotlin
class IpoListAdapter(
    private val onItemClick: (IpoItem) -> Unit
) : ListAdapter<IpoItem, IpoListAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<IpoItem>() {
            override fun areItemsTheSame(old: IpoItem, new: IpoItem) = old.symbol == new.symbol
            override fun areContentsTheSame(old: IpoItem, new: IpoItem) = old == new
        }
    }
    // ...
}
```

`IpoCalendarAdapter` and `IpoPerformanceAdapter` follow the same pattern, keyed on `symbol`.
`EtfListAdapter` keys on `etfItem.symbol`.

---

### 10. Fragments

#### `IpoFragment` — ViewPager2 Tab Host

Hosts a `ViewPager2` bound to a `TabLayout`. Three child Fragments are created: `IpoDirectoryFragment`, `IpoCalendarFragment`, `IpoPerformanceFragment`. Each child obtains the shared `IpoViewModel` via `by viewModels({ requireParentFragment() })`.

The fragment exposes a toolbar back button that calls `findNavController().popBackStack()`.

#### `IpoDirectoryFragment` / `IpoCalendarFragment` / `IpoPerformanceFragment`

Each collects from the appropriate `StateFlow` in the shared `IpoViewModel`. On `Loading`, skeleton views are shown via `View.VISIBLE/GONE`. On `Success`, the `RecyclerView` is populated. On `Empty` or `Error`, the respective message and retry button are displayed.

Pull-to-refresh on `IpoDirectoryFragment` and `IpoCalendarFragment` calls the corresponding `loadXxx()` method on the ViewModel.

#### `IpoDetailFragment`

Receives `symbol` from `arguments`. In `onViewCreated` it calls `viewModel.load(symbol)`. The back button calls `findNavController().popBackStack()`. A "View Prospectus" button opens `Intent(Intent.ACTION_VIEW, Uri.parse(url))` only when `prospectusUrl` is non-null and non-blank.

#### `EtfDirectoryFragment`

Displays a summary header row (total, Shariah count, conventional count) above the `RecyclerView`. Chip navigation wires `onItemClick` to navigate to `EtfDetailFragment` with `symbol` as a Bundle argument.

#### `EtfDetailFragment`

Uses three tab buttons (styled like existing `tabGainers`/`tabLosers` in `MarketDashboardFragment`) to switch between Overview, History, and Performance sections within a single Fragment — avoiding nested FragmentManager complexity. Section content is shown/hidden via `View.VISIBLE/GONE` or by inflating content into a container view, matching the `StockDetailFragment` `containerTabContent` pattern.

The History section uses `IntradayChartView.setChartData(primary, secondary)` where `primary` is the list of `close` values extracted from `EtfHistoryEntry`. The OHLCV table is rendered in a `RecyclerView` below the chart.

---

### 11. Navigation Wiring

#### `nav_graph.xml` additions

Four new destinations are added to `nav_graph.xml`:

```xml
<fragment
    android:id="@+id/ipoFragment"
    android:name="com.fyp.basarattradeinvest.feature.ipo.IpoFragment"
    android:label="IPOs"
    tools:layout="@layout/fragment_ipo_directory" />

<fragment
    android:id="@+id/ipoDetailFragment"
    android:name="com.fyp.basarattradeinvest.feature.ipo.IpoDetailFragment"
    android:label="IPO Detail"
    tools:layout="@layout/fragment_ipo_detail" />

<fragment
    android:id="@+id/etfDirectoryFragment"
    android:name="com.fyp.basarattradeinvest.feature.etf.EtfDirectoryFragment"
    android:label="ETFs"
    tools:layout="@layout/fragment_etf_directory" />

<fragment
    android:id="@+id/etfDetailFragment"
    android:name="com.fyp.basarattradeinvest.feature.etf.EtfDetailFragment"
    android:label="ETF Detail"
    tools:layout="@layout/fragment_etf_detail" />
```

Two new actions are added to the `nav_markets` destination:

```xml
<action android:id="@+id/action_markets_to_ipoFragment"
        app:destination="@id/ipoFragment" />
<action android:id="@+id/action_markets_to_etfDirectoryFragment"
        app:destination="@id/etfDirectoryFragment" />
```

An action from `ipoFragment` to `ipoDetailFragment`, and from `etfDirectoryFragment` to `etfDetailFragment`, are also added.

#### `MarketDashboardFragment` chip wiring

In `setupTopBarListeners()`, two new click listeners are added:

```kotlin
binding.chipIpo.setOnClickListener {
    findNavController().navigate(R.id.ipoFragment)
}
binding.chipEtf.setOnClickListener {
    findNavController().navigate(R.id.etfDirectoryFragment)
}
```

---

### 12. Layout Files

#### `fragment_ipo_directory.xml` (replaces empty file)

Contains a `SwipeRefreshLayout` wrapping a `NestedScrollView`:
- Skeleton loading group (`View` with `@drawable/bg_skeleton` background)
- `RecyclerView` for the IPO list
- Empty state `TextView`
- Error state `TextView` + retry `Button`

#### `fragment_ipo_detail.xml`

`NestedScrollView` containing:
- Back arrow `ImageView` + company name `TextView` header
- Status badge `TextView` with colored background
- Price row: floor price, strike price, listing price, current price (each nullable-guarded)
- Date section: book-building, subscription, listing date ranges
- Gain rows: listing gain % and current gain % (colored by sign)
- Description `TextView`
- Shariah badge (visible only if compliant)
- "View Prospectus" `Button` (visibility controlled by ViewModel)
- Skeleton overlay group
- Error state group with retry

#### `fragment_etf_directory.xml`

`SwipeRefreshLayout` wrapping `NestedScrollView`:
- Summary header `ConstraintLayout` (total, Shariah count, conventional count)
- Skeleton group
- `RecyclerView`
- Empty + error states

#### `fragment_etf_detail.xml`

`NestedScrollView` with:
- Header: back arrow, ETF name, symbol
- Price row: current price, change amount, change percent (colored by sign)
- Three tab buttons: "Overview", "History", "Performance"
- Container views for each section (shown/hidden by tab selection)
- Overview section: fund metadata fields + quote fields
- History section: timeframe buttons + `IntradayChartView` + `RecyclerView` for OHLCV rows
- Performance section: returns comparison table + tracking metrics

#### Item layouts

- `item_ipo_card.xml`: `ConstraintLayout` with rank, company name, symbol, sector, status badge, floor price, Shariah indicator
- `item_ipo_calendar_event.xml`: company name, symbol, event type label, event date, status indicator
- `item_ipo_performance.xml`: symbol, company name, sector, listing date, offer price, first-day return %, total return %
- `item_etf_card.xml`: name, symbol, fund manager, category, current price, change % (colored)

---

## Data Flow

```
User taps chipIpo
    → MarketDashboardFragment.setupTopBarListeners()
    → findNavController().navigate(R.id.ipoFragment)
    → IpoFragment.onViewCreated()
    → ViewPager2 creates IpoDirectoryFragment (default tab)
    → IpoDirectoryFragment observes IpoViewModel.directoryState
    → IpoViewModel.init → loadDirectory()
    → viewModelScope.launch { IpoRepositoryImpl.getIpos() }
    → IpoApiService.getIpos() → HTTP GET /api/v1/ipos
    → Response<IposResponseDto>
    → IposResponseDto.ipos.map { it.toDomain() }
    → Result.success(List<IpoItem>)
    → _directoryState.value = IpoDirectoryUiState.Success(items)
    → IpoDirectoryFragment collects Success → IpoListAdapter.submitList(items)
    → RecyclerView renders IPO cards

User taps an IPO card
    → IpoListAdapter.onItemClick(item)
    → IpoDirectoryFragment.findNavController().navigate(R.id.ipoDetailFragment, bundle)
    → IpoDetailFragment.onViewCreated(symbol = "XYZ")
    → IpoDetailViewModel.load("XYZ")
    → IpoApiService.getIpoDetail("XYZ") → HTTP GET /api/v1/ipos/XYZ
    → IpoDetailUiState.Success(ipo) → Fragment binds all fields
```

---

## Error Handling

Errors are handled at the repository level. The `NetworkErrorParser.parseError(response)` utility (already used by `MarketRepositoryImpl` and `WatchlistRepositoryImpl`) extracts a human-readable message from the response body. The resulting `Result.failure(Exception(message))` surfaces to the ViewModel, which emits an `Error(message)` UI state. The Fragment then displays the error message and a retry button.

`CancellationException` is always re-thrown to respect structured concurrency and avoid swallowing coroutine cancellations.

For HTTP 4xx errors (e.g., 404 for an unknown symbol), the message from the backend JSON body is used directly. For network failures (`IOException`), the exception message is used.

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Mapper symbol preservation — IPO

*For any* `IpoItemDto` with a given non-null `symbol`, calling `.toDomain()` should produce an `IpoItem` whose `symbol` field equals the original DTO's `symbol`.

**Validates: Requirements 4.1**

---

### Property 2: Mapper symbol preservation — ETF

*For any* `EtfItemDto` with a given non-null `symbol`, calling `.toDomain()` should produce an `EtfItem` whose `symbol` field equals the original DTO's `symbol`.

**Validates: Requirements 4.2**

---

### Property 3: IPO list adapter count invariant

*For any* non-empty list of `IpoItem` objects submitted to `IpoListAdapter`, the adapter's `itemCount` should equal the size of the submitted list.

**Validates: Requirements 5.3, 15.5**

---

### Property 4: ETF list adapter count invariant

*For any* non-empty list of `EtfItem` objects submitted to `EtfListAdapter`, the adapter's `itemCount` should equal the size of the submitted list.

**Validates: Requirements 9.3, 15.5**

---

### Property 5: DiffUtil symbol identity — IPO

*For any* two `IpoItem` instances, `IpoListAdapter.DIFF.areItemsTheSame(a, b)` should return `true` if and only if `a.symbol == b.symbol`.

**Validates: Requirements 5.3, 15.5**

---

### Property 6: DiffUtil symbol identity — ETF

*For any* two `EtfItem` instances, `EtfListAdapter.DIFF.areItemsTheSame(a, b)` should return `true` if and only if `a.symbol == b.symbol`.

**Validates: Requirements 9.3, 15.5**

---

### Property 7: Return percentage sign-to-color mapping

*For any* non-null `Double` value representing a return percentage, the color resolution function should return `R.color.color_positive` for values strictly greater than zero and `R.color.color_negative` for values strictly less than zero.

**Validates: Requirements 7.6, 8.7, 8.8, 9.5, 10.5, 12.5**

---

### Property 8: IPO calendar event type label mapping

*For any* `eventType` string in the set `{BOOK_BUILDING_START, BOOK_BUILDING_END, PUBLIC_SUBSCRIPTION_START, PUBLIC_SUBSCRIPTION_END, LISTING_DATE}`, the label formatter function should return a non-empty string that does not equal the raw input string (i.e., the enum value is humanized).

**Validates: Requirements 6.4**

---

### Property 9: ETF history close price extraction

*For any* non-empty list of `EtfHistoryEntry` objects, the extracted primary chart data list (close prices) should have the same length as the input list, and each element at index `i` should equal `entries[i].close`.

**Validates: Requirements 11.5**

---

### Property 10: Repository error message non-blank invariant

*For any* HTTP response with a non-successful status code (4xx or 5xx), the error-parsing path in `IpoRepositoryImpl` and `EtfRepositoryImpl` should produce a `Result.failure` whose exception message is non-null and non-blank.

**Validates: Requirements 15.7**

---

### Property 11: IpoItem DTO round-trip deserialization

*For any* valid `IpoItem` domain object constructed from a known `IpoItemDto`, the `symbol` field should survive the full DTO → domain mapper pipeline without modification (identity under mapping for the primary key field).

**Validates: Requirements 3.1, 4.1**

---

### Property 12: ETF mapper quote field propagation

*For any* `EtfItemDto` that contains a non-null `quote` field, the resulting `EtfItem.quote` should be non-null and its `currentPrice` should equal the `EtfQuoteDto.currentPrice`.

**Validates: Requirements 4.2, 10.4**
