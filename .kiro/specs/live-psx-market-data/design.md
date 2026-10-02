# Design Document — Live PSX Market Data (WebSocket + REST Fallback)

## Overview

This document describes the architecture for wiring the already-complete `LiveMarketDataSource` / `LiveMarketRepository` data layer into the three visible screens of the Basarat Trade & Invest app: **Home (Dashboard)**, **Market Dashboard**, and **Stock Detail**.

The data layer is fully built and requires no changes. All implementation work is confined to five layers:

1. **UiState** — additive, backward-compatible field additions
2. **ViewModel** — subscribe/unsubscribe lifecycle, flow collection, `copy()`-based state updates
3. **Adapters** — targeted `updateQuote()` methods that avoid full `notifyDataSetChanged` flicker
4. **Fragments** — binding live fields, tracking previous `liveQuotes` map to dispatch targeted adapter calls
5. **XML layouts** — `ConnectionStatusChip` added to two app bars

The implementation never touches the network stack, domain models, or DTOs.

---

## Architecture

### Component Diagram

```
LiveMarketDataSource (singleton, IO scope)
        │
        ▼
LiveMarketRepositoryImpl          ← domain interface
   connectionState: StateFlow<WsConnectionState>
   quoteUpdates:    SharedFlow<LiveQuote>
   quoteCache:      StateFlow<Map<String,LiveQuote>>
   subscribe() / unsubscribe()
        │
   ┌────┴────────────────────────┐
   ▼                             ▼                          ▼
HomeViewModel            MarketDashboardViewModel    StockDetailViewModel
(activityViewModels)     (activityViewModels)        (viewModels)
collects quoteUpdates    collects quoteUpdates        collects quoteUpdates
collects connectionState collects connectionState     collects connectionState
NO unsubscribe           NO unsubscribe               unsubscribe in onCleared
        │                             │                           │
   HomeUiState.Success      MarketDashboardUiState.Success  StockDetailUiState.Success
   + liveQuotes             + liveQuotes                    + livePrice / liveChangePct
   + wsConnectionState      + wsConnectionState             + liveChange / liveVolume
                                                            + isLivePriceStreaming
        │                             │                           │
  HomeFragment            MarketDashboardFragment       StockDetailFragment
  bind watchlist rows     call adapter.updateQuote()    bind header price/change
  show "Live" label       show ConnectionStatusChip     show ConnectionStatusChip
                          ┌────────────────┐
                          │ MarketStockAdapter.updateQuote()  │
                          │ ConstituentAdapter.updateQuote()  │
                          └────────────────┘
```

### Key Design Decisions

**Single shared WebSocket connection**: `LiveMarketDataSource` is an application-singleton. Multiple ViewModels share it. Subscribe calls are additive — no deduplication work needed in ViewModels. The data source handles symbol deduplication internally.

**Activity-scoped ViewModels do not unsubscribe**: `HomeViewModel` and `MarketDashboardViewModel` use `activityViewModels()`. Their subscriptions span the whole user session, which keeps prices flowing while the user navigates between tabs. Only `StockDetailViewModel`, which uses `viewModels()`, unsubscribes in `onCleared()` to release bandwidth for individual stock streams.

**`copy()`-based state mutations**: ViewModels never rebuild the entire `UiState` for live updates — they call `.copy(liveQuotes = ...)` on the existing `Success` state. This prevents expensive rebinding of REST-loaded data on every quote tick.

**Targeted adapter updates**: Fragments track the previous `liveQuotes` map and diff it against the incoming map on each `Success` emission. Only changed symbols trigger `adapter.updateQuote()`, preventing the full list from re-binding on every tick.

**REST fallback is source-transparent**: `LiveQuote.source` is informational only. ViewModels apply all quotes identically regardless of whether `source == WEBSOCKET` or `source == REST_FALLBACK`. The `ConnectionStatusChip` reflects the `WsConnectionState` separately to communicate fallback status to the user.

---

## Component Designs

### 1. UiState Modifications

All changes are additive with default values, preserving source compatibility at all existing call sites.

#### `HomeUiState.Success`

```kotlin
// In HomeUiState.kt — add to the Success data class
data class Success(
    // ... all existing fields unchanged ...

    // === NEW live market fields ===
    val liveQuotes: Map<String, LiveQuote> = emptyMap(),
    val wsConnectionState: WsConnectionState = WsConnectionState.Idle
) : HomeUiState
```

No existing construction sites require changes. All existing `HomeUiState.Success(...)` calls continue to compile because the new fields have default values.

#### `MarketDashboardUiState.Success`

```kotlin
// In MarketDashboardUiState.kt — add to the Success data class
data class Success(
    // ... all existing fields unchanged ...

    // === NEW live market fields ===
    val liveQuotes: Map<String, LiveQuote> = emptyMap(),
    val wsConnectionState: WsConnectionState = WsConnectionState.Idle
) : MarketDashboardUiState
```

The existing construction in `MarketDashboardViewModel.loadMarketData()` does not supply these fields, so defaults apply automatically.

#### `StockDetailUiState.Success`

```kotlin
// In StockDetailUiState.kt — add to the Success data class
data class Success(
    // ... all existing fields unchanged ...

    // === NEW live price overlay fields ===
    val livePrice: Double? = null,
    val liveChangePct: Double? = null,
    val liveChange: Double? = null,
    val liveVolume: Long? = null,
    val isLivePriceStreaming: Boolean = false
) : StockDetailUiState
```

All existing `StockDetailUiState.Success(...)` constructions in `StockDetailViewModel.loadStock()` are source-compatible.

---

### 2. ViewModel Modifications

#### `HomeViewModel`

**New constructor parameter:**
```kotlin
class HomeViewModel(
    // ... existing params ...
    private val liveMarketRepository: LiveMarketRepository = NetworkClient.liveMarketRepository,
    autoLoad: Boolean = true
) : ViewModel()
```

`NetworkClient` does not currently expose `liveMarketRepository`. Add it:
```kotlin
// In NetworkClient.kt
val liveMarketRepository: LiveMarketRepository by lazy {
    com.fyp.basarattradeinvest.data.repository.LiveMarketRepositoryImpl(liveMarketDataSource)
}
```

**Flow collection in `init {}`:**
```kotlin
init {
    // Collect live quotes and merge into current Success state
    viewModelScope.launch {
        liveMarketRepository.quoteUpdates.collect { quote ->
            val current = _uiState.value as? HomeUiState.Success ?: return@collect
            val updated = current.liveQuotes.toMutableMap()
            updated[quote.symbol.uppercase()] = quote
            _uiState.value = current.copy(liveQuotes = updated)
        }
    }

    // Collect connection state and merge into current Success state
    viewModelScope.launch {
        liveMarketRepository.connectionState.collect { state ->
            val current = _uiState.value as? HomeUiState.Success ?: return@collect
            _uiState.value = current.copy(wsConnectionState = state)
        }
    }

    if (autoLoad) {
        loadDashboard(forceLoad = false)
    }
}
```

**Subscribe after watchlist loads:** In `loadDashboard()`, after `watchlistDeferred.await()` succeeds and returns a non-empty list:
```kotlin
val watchlistRes = watchlistDeferred.await()
watchlistRes.getOrNull()?.let { watchlist ->
    if (watchlist.items.isNotEmpty()) {
        val symbols = watchlist.items.map { it.symbol.uppercase() }
        liveMarketRepository.subscribe(symbols)
    }
}
// ... then apply partial state as before
```

`HomeViewModel` does **not** call `unsubscribe()` anywhere — not in `onCleared()` and not when the watchlist changes. This is correct by design (activity-scoped ViewModel, shared singleton connection).

---

#### `MarketDashboardViewModel`

**New constructor parameter:**
```kotlin
class MarketDashboardViewModel(
    // ... existing params ...
    private val liveMarketRepository: LiveMarketRepository = NetworkClient.liveMarketRepository
) : ViewModel()
```

**Flow collection in `init {}`:**
```kotlin
init {
    viewModelScope.launch {
        liveMarketRepository.quoteUpdates.collect { quote ->
            val current = _uiState.value as? MarketDashboardUiState.Success ?: return@collect
            val updated = current.liveQuotes.toMutableMap()
            updated[quote.symbol.uppercase()] = quote
            _uiState.value = current.copy(liveQuotes = updated)
        }
    }

    viewModelScope.launch {
        liveMarketRepository.connectionState.collect { connState ->
            val current = _uiState.value as? MarketDashboardUiState.Success ?: return@collect
            _uiState.value = current.copy(wsConnectionState = connState)
        }
    }

    ensureLoaded()
}
```

**Subscribe after data loads:** In `loadMarketData()`, after the constituent and top-symbol results are resolved:
```kotlin
// After constituentsResult and topSymbolsResult are available
val allSymbols = mutableSetOf<String>()
constituentsResult.getOrDefault(emptyList()).mapTo(allSymbols) { it.symbol.uppercase() }
topSymbolsResult.getOrDefault(emptyList()).mapTo(allSymbols) { it.symbol.uppercase() }
if (allSymbols.isNotEmpty()) {
    liveMarketRepository.subscribe(allSymbols)
}
```

Also subscribe when filter or constituent index changes:
```kotlin
// In setFilter(), after topSymbolsResult resolves:
topSymbolsResult.getOrDefault(emptyList())
    .map { it.symbol.uppercase() }
    .let { if (it.isNotEmpty()) liveMarketRepository.subscribe(it) }

// In selectConstituentIndex(), after constituents resolve:
result.getOrDefault(emptyList())
    .map { it.symbol.uppercase() }
    .let { if (it.isNotEmpty()) liveMarketRepository.subscribe(it) }
```

`MarketDashboardViewModel` does **not** call `unsubscribe()` anywhere.

---

#### `StockDetailViewModel`

**New constructor parameter and exposed state:**
```kotlin
class StockDetailViewModel(
    // ... existing params ...
    private val liveMarketRepository: LiveMarketRepository = NetworkClient.liveMarketRepository
) : ViewModel() {

    // Expose the repository's connection state directly
    val liveConnectionState: StateFlow<WsConnectionState> = liveMarketRepository.connectionState
```

**Flow collection in `init {}`:**
```kotlin
init {
    viewModelScope.launch {
        liveMarketRepository.quoteUpdates.collect { quote ->
            if (quote.symbol.uppercase() != currentSymbol.uppercase()) return@collect
            val current = _uiState.value as? StockDetailUiState.Success ?: return@collect
            _uiState.value = current.copy(
                livePrice = quote.current,
                liveChangePct = quote.changePct,
                liveChange = quote.change,
                liveVolume = quote.volume,
                isLivePriceStreaming = true
            )
        }
    }
}
```

**Subscribe in `loadStock()`:** After the REST fetch succeeds and `_uiState.value` is set to `StockDetailUiState.Success`:
```kotlin
fun loadStock(symbol: String, forceRefresh: Boolean = false) {
    val previousSymbol = currentSymbol
    // If switching symbols, unsubscribe from the old one
    if (previousSymbol.isNotBlank() && previousSymbol != symbol.uppercase()) {
        liveMarketRepository.unsubscribe(listOf(previousSymbol))
    }
    // ... existing loading logic ...

    // After success:
    liveMarketRepository.subscribe(listOf(symbol.uppercase()))
}
```

**Unsubscribe in `onCleared()`:**
```kotlin
override fun onCleared() {
    super.onCleared()
    if (currentSymbol.isNotBlank()) {
        liveMarketRepository.unsubscribe(listOf(currentSymbol))
    }
}
```

---

### 3. Adapter Modifications

Both adapters follow the same targeted-update pattern. The key constraint is that `updateQuote()` must **not** call `submitList()` or `notifyDataSetChanged()` — only `notifyItemChanged(position)` for the single affected row. `ListAdapter`'s `currentList` is the backing read-only snapshot.

#### `MarketStockAdapter.updateQuote()`

```kotlin
/**
 * Updates the price and changePct of a single row in-place.
 * Bypasses DiffUtil entirely for real-time ticks to eliminate list flicker.
 */
fun updateQuote(symbol: String, quote: LiveQuote) {
    val position = currentList.indexOfFirst {
        it.symbol.equals(symbol, ignoreCase = true)
    }
    if (position == -1) return  // symbol not in current list — no-op, no throw

    // Replace the item in an internal mutable shadow list
    val item = currentList[position]
    val updated = item.copy(
        price = quote.current ?: item.price,
        changePercent = quote.changePct ?: item.changePercent
    )
    // Mutate the shadow list and notify
    internalList[position] = updated
    notifyItemChanged(position, PAYLOAD_LIVE_QUOTE)  // payload avoids full rebind
}

companion object {
    const val PAYLOAD_LIVE_QUOTE = "live_quote"
}
```

**Shadow list management:** `ListAdapter` manages `currentList` immutably. To support `updateQuote()`, the adapter maintains a parallel `internalList: MutableList<MarketStock>` that is kept in sync with `submitList()` calls. `onBindViewHolder` reads from `internalList` when a live-quote payload is present, otherwise binds from `getItem(position)`.

```kotlin
private val internalList = mutableListOf<MarketStock>()

override fun submitList(list: List<MarketStock>?) {
    internalList.clear()
    list?.let { internalList.addAll(it) }
    super.submitList(list)
}

override fun onBindViewHolder(holder: StockViewHolder, position: Int, payloads: List<Any>) {
    if (payloads.contains(PAYLOAD_LIVE_QUOTE)) {
        holder.bindLiveUpdate(internalList[position])
    } else {
        super.onBindViewHolder(holder, position, payloads)
    }
}
```

`bindLiveUpdate()` updates only the price and change text views, leaving rank, symbol, and sector unchanged.

#### `ConstituentAdapter.updateQuote()`

Identical pattern to `MarketStockAdapter`, but the item type is `IndexConstituent`:

```kotlin
fun updateQuote(symbol: String, quote: LiveQuote) {
    val position = currentList.indexOfFirst {
        it.symbol.equals(symbol, ignoreCase = true)
    }
    if (position == -1) return

    val item = currentList[position]
    val updated = item.copy(
        current = quote.current ?: item.current,
        changePct = quote.changePct ?: item.changePct
    )
    internalList[position] = updated
    notifyItemChanged(position, PAYLOAD_LIVE_QUOTE)
}
```

`bindLiveUpdate()` updates `tvConstituentPrice` and `tvConstituentChange` only.

---

### 4. Fragment Modifications

#### `HomeFragment`

In `bindSuccessState()`, after `bindWatchlist(state.watchlist)` applies REST-fetched data, apply live overlays:

```kotlin
private fun bindSuccessState(state: HomeUiState.Success) {
    // ... existing bindings ...
    bindWatchlist(state.watchlist)
    applyLiveQuotesToWatchlist(state)
    bindLiveConnectionLabel(state.wsConnectionState)
}

private fun applyLiveQuotesToWatchlist(state: HomeUiState.Success) {
    val stocks = (state.watchlist as? WatchlistUiState.Success)?.watchlist?.items ?: return
    stocks.forEachIndexed { index, item ->
        val quote = state.liveQuotes[item.symbol.uppercase()] ?: return@forEachIndexed
        val (priceView, changeView) = when (index) {
            0 -> binding.tvWatchlistPrice1 to binding.tvWatchlistChange1
            1 -> binding.tvWatchlistPrice2 to binding.tvWatchlistChange2
            else -> return@forEachIndexed
        }
        quote.current?.let { priceView.text = priceFormat.format(it) }
        quote.changePct?.let { pct ->
            val isPos = pct >= 0
            val prefix = if (isPos) "+" else ""
            changeView.text = String.format(Locale.US, "%s%.2f%%", prefix, pct)
            val color = ContextCompat.getColor(
                requireContext(),
                if (isPos) R.color.color_positive else R.color.color_negative
            )
            changeView.setTextColor(color)
        }
    }
}

private fun bindLiveConnectionLabel(state: WsConnectionState) {
    // Update the watchlist section "last updated" label
    // Show a "● Live" suffix when actively streaming
    val isLive = state is WsConnectionState.Live
    val baseText = binding.tvWatchlistLastUpdated.tag as? String ?: ""
    binding.tvWatchlistLastUpdated.text = if (isLive) "$baseText  ● Live" else baseText
}
```

**Note:** `tvWatchlistLastUpdated.tag` should be set to the REST-fetched timestamp string when `bindWatchlist()` first renders, so the live-label function can append to it without losing the time.

---

#### `MarketDashboardFragment`

The fragment must track the previous `liveQuotes` snapshot to dispatch only changed quotes to the adapters.

```kotlin
private var previousLiveQuotes: Map<String, LiveQuote> = emptyMap()

// In observeUiState(), inside the Success branch:
is MarketDashboardUiState.Success -> {
    // ... existing bindings ...
    applyLiveQuoteUpdates(state.liveQuotes)
    bindConnectionChip(state.wsConnectionState)
    previousLiveQuotes = state.liveQuotes
}
```

```kotlin
private fun applyLiveQuoteUpdates(liveQuotes: Map<String, LiveQuote>) {
    liveQuotes.forEach { (symbol, quote) ->
        if (previousLiveQuotes[symbol] !== quote) {   // reference check for same object
            stockAdapter.updateQuote(symbol, quote)
            constituentAdapter.updateQuote(symbol, quote)
        }
    }
}

private fun bindConnectionChip(state: WsConnectionState) {
    when (state) {
        WsConnectionState.Live -> {
            binding.chipConnectionStatus.visibility = View.VISIBLE
            binding.chipConnectionStatus.text = "Live"
            binding.chipConnectionStatus.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.color_basarat_green)
            )
        }
        is WsConnectionState.Connecting, is WsConnectionState.Reconnecting -> {
            binding.chipConnectionStatus.visibility = View.VISIBLE
            binding.chipConnectionStatus.text = "Connecting"
            binding.chipConnectionStatus.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.color_warning)
            )
        }
        is WsConnectionState.Delayed -> {
            binding.chipConnectionStatus.visibility = View.VISIBLE
            binding.chipConnectionStatus.text = "Delayed"
            binding.chipConnectionStatus.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.color_warning)
            )
        }
        WsConnectionState.Idle, WsConnectionState.Closed -> {
            binding.chipConnectionStatus.visibility = View.GONE
        }
    }
}
```

The `chipConnectionStatus` does not intercept touch events (`android:clickable="false"`, `android:focusable="false"`).

---

#### `StockDetailFragment`

`StockDetailFragment` collects two flows in `observeUiState()`:

1. `viewModel.uiState` — already collected; extend to apply live fields
2. `viewModel.liveConnectionState` — new collection for chip visibility

```kotlin
// In observeUiState(), inside the repeatOnLifecycle block:
launch {
    viewModel.uiState.collect { state ->
        when (state) {
            is StockDetailUiState.Success -> {
                bindHeader(state.detail)          // existing
                // ... other existing bindings ...
                applyLiveOverlay(state)           // new
            }
            // ... other states unchanged ...
        }
    }
}

launch {
    viewModel.liveConnectionState.collect { connState ->
        val isStreaming = (_uiState.value as? StockDetailUiState.Success)
            ?.isLivePriceStreaming == true
        bindDetailConnectionChip(connState, isStreaming)
    }
}
```

```kotlin
private fun applyLiveOverlay(state: StockDetailUiState.Success) {
    if (!state.isLivePriceStreaming) return

    state.livePrice?.let { price ->
        binding.tvPrice.text = String.format(Locale.US, "%.2f", price)
        val isPos = (state.liveChange ?: 0.0) >= 0
        binding.tvPrice.setTextColor(
            ContextCompat.getColor(requireContext(), if (isPos) R.color.color_positive else R.color.color_negative)
        )
    }

    val change = state.liveChange
    val changePct = state.liveChangePct
    if (change != null && changePct != null) {
        val isPos = change >= 0
        val prefix = if (isPos) "+" else ""
        binding.tvChangeAmount.text = String.format(Locale.US, "%s%.2f", prefix, change)
        binding.tvChangePercent.text = String.format(Locale.US, "%s%.2f%%", prefix, changePct)
        val color = ContextCompat.getColor(requireContext(), if (isPos) R.color.color_positive else R.color.color_negative)
        binding.tvChangeAmount.setTextColor(color)
        binding.tvChangePercent.setTextColor(color)
    }

    state.liveVolume?.let { vol ->
        binding.tvVolCurrent.text = String.format(Locale.US, "%,d", vol)
    }
}

private fun bindDetailConnectionChip(connState: WsConnectionState, isStreaming: Boolean) {
    if (!isStreaming) {
        binding.chipConnectionStatus.visibility = View.GONE
        return
    }
    when (connState) {
        WsConnectionState.Live -> {
            binding.chipConnectionStatus.visibility = View.VISIBLE
            binding.chipConnectionStatus.text = "Live"
            binding.chipConnectionStatus.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.color_basarat_green)
            )
        }
        is WsConnectionState.Connecting, is WsConnectionState.Reconnecting -> {
            binding.chipConnectionStatus.visibility = View.VISIBLE
            binding.chipConnectionStatus.text = "Connecting"
            binding.chipConnectionStatus.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.color_warning)
            )
        }
        is WsConnectionState.Delayed -> {
            binding.chipConnectionStatus.visibility = View.VISIBLE
            binding.chipConnectionStatus.text = "Delayed"
            binding.chipConnectionStatus.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.color_warning)
            )
        }
        WsConnectionState.Idle, WsConnectionState.Closed -> {
            binding.chipConnectionStatus.visibility = View.GONE
        }
    }
}
```

---

### 5. ConnectionStatusChip XML

Add the chip to both app bar layouts. The chip is a styled `TextView` with rounded corners and white text. It does not intercept user interactions.

**`fragment_market_dashboard.xml`** — add inside the app bar `ConstraintLayout`, constrained to `end_toStartOf` the notifications icon:

```xml
<TextView
    android:id="@+id/chipConnectionStatus"
    android:layout_width="wrap_content"
    android:layout_height="24dp"
    android:paddingStart="10dp"
    android:paddingEnd="10dp"
    android:gravity="center"
    android:textColor="@color/white"
    android:textSize="11sp"
    android:textStyle="bold"
    android:background="@drawable/bg_connection_chip"
    android:clickable="false"
    android:focusable="false"
    android:visibility="gone"
    tools:text="Live"
    tools:visibility="visible"
    app:layout_constraintEnd_toStartOf="@id/ivTopNotifications"
    app:layout_constraintTop_toTopOf="@id/ivTopNotifications"
    app:layout_constraintBottom_toBottomOf="@id/ivTopNotifications"
    android:layout_marginEnd="8dp" />
```

**`fragment_stock_detail.xml`** — add near the back/share icons in the header, constrained to the `start_toEndOf` the `tvTopCode` or within its own row:

```xml
<TextView
    android:id="@+id/chipConnectionStatus"
    android:layout_width="wrap_content"
    android:layout_height="22dp"
    android:paddingStart="8dp"
    android:paddingEnd="8dp"
    android:gravity="center"
    android:textColor="@color/white"
    android:textSize="10sp"
    android:textStyle="bold"
    android:background="@drawable/bg_connection_chip"
    android:clickable="false"
    android:focusable="false"
    android:visibility="gone"
    tools:text="Live"
    tools:visibility="visible"
    android:layout_marginStart="8dp" />
```

**`bg_connection_chip.xml`** (new drawable):

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <corners android:radius="12dp" />
    <solid android:color="@color/color_basarat_green" />
</shape>
```

The background color is overridden programmatically in the fragment. The drawable simply provides the shape (rounded pill). A `@color/color_warning` amber token should already exist in the BASARAT color system; if not, define it as `#F59E0B`.

---

### 6. NetworkClient Addition

`LiveMarketRepository` is not yet exposed from `NetworkClient`. Add:

```kotlin
// In NetworkClient.kt
val liveMarketRepository: LiveMarketRepository by lazy {
    com.fyp.basarattradeinvest.data.repository.LiveMarketRepositoryImpl(liveMarketDataSource)
}
```

This assumes `LiveMarketRepositoryImpl` takes `LiveMarketDataSource` as its constructor parameter and delegates all calls through to it — which is how the data layer is already built.

---

### 7. Data Flow — End-to-End Sequence

#### Home screen watchlist live update

```
1. HomeViewModel.loadDashboard() completes
2. watchlistDeferred resolves → non-empty Watchlist
3. liveMarketRepository.subscribe(["OGDC","LUCK","MCB",...])
4. LiveMarketDataSource opens WebSocket (if not already open)
5. WS server sends quote_update for "OGDC"
6. LiveMarketDataSource emits LiveQuote(symbol="OGDC",...) on quoteUpdates
7. HomeViewModel.quoteUpdates collector fires
8. _uiState.value = current.copy(liveQuotes = {"OGDC" → quote})
9. HomeFragment.observeUiState() collects new Success state
10. applyLiveQuotesToWatchlist() finds "OGDC" at position 0
11. tvWatchlistPrice1.text = "152.35"  (live price)
12. tvWatchlistChange1.text = "+1.25%"
```

#### Market Dashboard targeted adapter update

```
1. MarketDashboardViewModel.loadMarketData() completes
2. liveMarketRepository.subscribe(allSymbols)  ← constituents + top symbols
3. WS server sends board_update with 10 quotes
4. LiveMarketDataSource processes list → emits 10 LiveQuotes one by one
5. For each quote, MarketDashboardViewModel collector fires:
   _uiState.value = current.copy(liveQuotes = previous + (symbol → quote))
6. MarketDashboardFragment.observeUiState() collects new Success state
7. applyLiveQuoteUpdates() diffs new liveQuotes vs previousLiveQuotes
8. For each changed symbol:
   stockAdapter.updateQuote(symbol, quote)        ← if in topSymbols list
   constituentAdapter.updateQuote(symbol, quote)  ← if in constituents list
9. Each adapter calls notifyItemChanged(position, PAYLOAD_LIVE_QUOTE)
10. ViewHolder.onBindViewHolder(holder, pos, payloads) called
11. Only tvPrice and tvChange updated — no layout remeasure for rank/symbol/sector
```

#### Stock Detail symbol change

```
1. User navigates from StockDetailFragment (OGDC) to StockDetailFragment (LUCK)
2. viewModel.loadStock("LUCK") called
3. previousSymbol = "OGDC" ≠ "LUCK"
4. liveMarketRepository.unsubscribe(["OGDC"])
5. REST fetch for "LUCK" completes
6. _uiState.value = StockDetailUiState.Success(detail=LUCK, livePrice=null, ...)
7. liveMarketRepository.subscribe(["LUCK"])
8. WS server sends quote_update for "LUCK"
9. StockDetailViewModel.quoteUpdates collector fires
10. quote.symbol == currentSymbol → copy live fields, isLivePriceStreaming = true
11. StockDetailFragment.applyLiveOverlay() updates tvPrice, tvChangeAmount, etc.
```

---

## Error Handling

| Scenario | Behavior |
|---|---|
| WebSocket fails to connect | `WsConnectionState.Delayed` → REST fallback polls every 15 s (or server-recommended interval). Chip shows "Delayed". |
| REST fallback quote received | Processed identically to WebSocket quote. `source == REST_FALLBACK` is ignored by ViewModels. |
| `updateQuote()` called for unknown symbol | Returns silently. No exception, no side effects. |
| ViewModel receives quote for non-current symbol (StockDetail) | Filtered out by `quote.symbol != currentSymbol` check. |
| Fragment collects live state before first Success | `as? HomeUiState.Success ?: return@collect` guard in ViewModel ensures no crash. |
| `onCleared()` called before any symbol loaded | `if (currentSymbol.isNotBlank())` guard prevents empty unsubscribe call. |
| WS reconnects after delay | `WsConnectionState.Live` emitted → all ViewModels update `wsConnectionState` → Chip returns to green "Live". REST polling stops automatically. |

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Quote Emission Round-Trip (Home)

*For any* `LiveQuote` emitted by `LiveMarketRepository.quoteUpdates` while `HomeViewModel` has an active `HomeUiState.Success` state, the resulting `HomeUiState.Success.liveQuotes` map **must** contain that quote keyed by the quote's `symbol.uppercase()`.

**Validates: Requirements 2.3, 14.2**

---

### Property 2: Quote Emission Round-Trip (Market Dashboard)

*For any* `LiveQuote` emitted by `LiveMarketRepository.quoteUpdates` while `MarketDashboardViewModel` has an active `MarketDashboardUiState.Success` state, the resulting `MarketDashboardUiState.Success.liveQuotes` map **must** contain that quote keyed by the quote's `symbol.uppercase()`.

**Validates: Requirements 5.4, 14.3**

---

### Property 3: Quote Emission Round-Trip (Stock Detail)

*For any* `LiveQuote` emitted by `LiveMarketRepository.quoteUpdates` whose `symbol` matches `StockDetailViewModel.currentSymbol`, the resulting `StockDetailUiState.Success` **must** have `livePrice == quote.current`, `liveChangePct == quote.changePct`, `liveChange == quote.change`, `liveVolume == quote.volume`, and `isLivePriceStreaming == true`.

**Validates: Requirements 10.3, 10.4, 14.4**

---

### Property 4: Source Transparency

*For any* two `LiveQuote` values that are identical in all fields except `source` (one with `WEBSOCKET`, one with `REST_FALLBACK`), all three ViewModels (Home, MarketDashboard, StockDetail) **must** produce identical state updates — the `source` field **must** have no observable effect on UiState.

**Validates: Requirements 14.2, 14.3, 14.4**

---

### Property 5: Connection State Propagation

*For any* `WsConnectionState` value emitted by `LiveMarketRepository.connectionState`, each ViewModel that has an active `Success` UiState **must** update its `wsConnectionState` field to exactly that value on the next collection.

**Validates: Requirements 2.4, 5.5**

---

### Property 6: Subscribe-on-Load (Home Watchlist)

*For any* non-empty `Watchlist` successfully returned by `watchlistRepository.getDefaultWatchlist()` during `HomeViewModel.loadDashboard()`, `LiveMarketRepository.subscribe()` **must** be called with a collection containing exactly the symbols from that watchlist.

**Validates: Requirements 2.2**

---

### Property 7: Subscribe-on-Load (Market Dashboard)

*For any* non-empty list of constituents or top-symbols successfully loaded by `MarketDashboardViewModel`, `LiveMarketRepository.subscribe()` **must** be called with a collection containing exactly those symbols (uppercased).

**Validates: Requirements 5.2, 5.3**

---

### Property 8: Subscribe-on-Load (Stock Detail)

*For any* symbol string passed to `StockDetailViewModel.loadStock()` that resolves to a successful REST fetch, `LiveMarketRepository.subscribe()` **must** be called with `listOf(symbol.uppercase())`.

**Validates: Requirements 10.2**

---

### Property 9: Unsubscribe-on-Symbol-Change

*For any* pair of distinct symbols `(A, B)` where `loadStock(A)` was called first and then `loadStock(B)` is called, `LiveMarketRepository.unsubscribe(listOf(A))` **must** be called before `LiveMarketRepository.subscribe(listOf(B))`.

**Validates: Requirements 10.5, 13.4**

---

### Property 10: Unsubscribe-on-Clear

*For any* symbol that was loaded by `StockDetailViewModel` (i.e., `currentSymbol` is non-empty), calling `onCleared()` **must** result in `LiveMarketRepository.unsubscribe(listOf(currentSymbol))` being called.

**Validates: Requirements 10.6, 13.1**

---

### Property 11: Adapter Targeted Update — Match

*For any* `MarketStockAdapter` (or `ConstituentAdapter`) holding a list of `n` items, and any `symbol` that is present in the list at position `p`, calling `updateQuote(symbol, quote)` **must** update the item at position `p` with the quote's price/change values and call `notifyItemChanged(p)` — and **must not** call `notifyDataSetChanged()` or modify any other position.

**Validates: Requirements 6.2, 7.2**

---

### Property 12: Adapter No-Op for Missing Symbol

*For any* `MarketStockAdapter` (or `ConstituentAdapter`) holding a list of items, and any `symbol` string that is **not** present in the list, calling `updateQuote(symbol, quote)` **must** leave the list state entirely unchanged and **must not** throw any exception.

**Validates: Requirements 6.3, 7.3**

---

### Property 13: UiState Default Compatibility

*For any* existing construction of `HomeUiState.Success`, `MarketDashboardUiState.Success`, or `StockDetailUiState.Success` that does not supply the new live fields, the resulting instance **must** have `liveQuotes == emptyMap()`, `wsConnectionState == WsConnectionState.Idle`, and all `livePrice`/`liveChangePct`/`liveChange`/`liveVolume` fields `== null`, and `isLivePriceStreaming == false`.

**Validates: Requirements 1.3, 4.3, 9.6**
