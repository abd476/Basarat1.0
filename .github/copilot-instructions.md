# BASARAT — GitHub Copilot Instructions

## 1. PROJECT OVERVIEW

BASARAT is an Android application for Pakistan Stock Exchange (PSX) market research, stock analysis, forecasting, sentiment, portfolio management, risk analytics, Shariah screening, alerts, news, community features, and AI assistance.

The Android application is the frontend of the BASARAT system.

The project must be developed as a professional production-style financial application suitable for an FYP final demonstration.

---

# 2. ANDROID TECHNOLOGY STACK

Use the existing project stack.

Primary technologies:

- Kotlin
- XML layouts
- ViewBinding
- MVVM
- Repository pattern
- Clean Architecture where already implemented
- ViewModel
- StateFlow
- Kotlin Coroutines
- Retrofit
- Material Components
- RecyclerView
- Existing charting library/components
- Existing authentication/token architecture

## IMPORTANT

Do NOT migrate XML screens to Jetpack Compose unless explicitly requested.

Do NOT introduce a new architecture simply because another architecture is preferred.

Always inspect the existing implementation before changing architecture.

---

# 3. CORE ARCHITECTURE

The preferred flow is:

```text
XML / Fragment
        ↓
ViewModel
        ↓
Use Case (if already used)
        ↓
Repository
        ↓
Retrofit API
        ↓
Backend
```

Fragments should primarily:

- display UI
- collect StateFlow
- handle user interaction
- perform navigation

Fragments must NOT contain business logic.

Fragments must NOT directly call Retrofit APIs.

Fragments must NOT perform complex data transformation.

ViewModels should manage:

- UI state
- loading
- success
- empty
- error
- user actions
- orchestration/use cases

Repositories should manage data access.

---

# 4. EXISTING ARCHITECTURE HAS PRIORITY

Before implementing anything:

1. Inspect the existing code.
2. Understand how the relevant feature currently works.
3. Reuse existing components.
4. Reuse existing repositories.
5. Reuse existing API services.
6. Reuse existing ViewModels where appropriate.
7. Reuse existing state models.
8. Make the smallest change required.

Do NOT rewrite working code unnecessarily.

Do NOT perform broad refactoring while implementing a feature.

---

# 5. BACKEND IS THE SOURCE OF TRUTH

BASARAT uses a real backend.

Swagger:

```text
http://16.16.26.247:8000/docs
```

Alternative deployed backend:

```text
https://basarat-fyp-official.onrender.com/docs
```

Always inspect the backend/API contract and the existing Retrofit implementation before implementing backend-dependent functionality.

Never assume API fields.

Never invent response structures.

Never invent request bodies.

Never invent endpoint behavior.

If the API documentation and existing implementation appear inconsistent, investigate before changing the code.

---

# 6. ABSOLUTE NO-MOCK-DATA RULE

## NEVER USE MOCK FINANCIAL DATA IN THE APPLICATION

Backend responses are the source of truth.

Never hardcode or fabricate:

- stock prices
- index values
- percentage changes
- RSI
- MACD
- Bollinger Bands
- EPS
- P/E
- ROE
- market capitalization
- forecast prices
- forecast percentages
- sentiment scores
- sentiment labels
- chart points
- market movers
- gainers
- losers
- most-active stocks
- news headlines
- news dates
- watchlist stocks
- portfolio values
- P/L
- risk metrics
- VaR
- Monte Carlo results
- Shariah metrics
- AI-generated financial insights

Example values in design prompts are NOT real data.

If the backend does not provide a value:

- hide the value,
- display `N/A`,
- or show an appropriate empty state.

Never fabricate a value just to make the UI look complete.

---

# 7. BACKEND → UI DATA FLOW

Backend data should follow:

```text
Backend Response
      ↓
DTO
      ↓
Domain Model
      ↓
Repository
      ↓
ViewModel
      ↓
StateFlow / UI State
      ↓
XML UI
```

Every important financial value should be traceable through this chain.

Do not bypass the repository layer.

Do not put backend parsing directly inside Fragments.

Do not hardcode API responses inside ViewModels.

---

# 8. API CONTRACT VERIFICATION

Before implementing a new API feature:

1. Inspect Swagger.
2. Identify endpoint.
3. Identify HTTP method.
4. Inspect authentication requirements.
5. Inspect request parameters/body.
6. Inspect response schema.
7. Inspect actual existing Retrofit interface.
8. Inspect DTOs.
9. Inspect repository.
10. Map response to UI state.
11. Map UI state to XML.

Do not create models based only on assumptions.

Use the actual backend field names and types.

---

# 9. STATE MANAGEMENT

Use clean UI states.

Prefer:

```text
Loading
Success
Empty
Error
```

Avoid unnecessary combinations of boolean flags such as:

```text
isLoading
hasData
hasError
isEmpty
isRefreshing
isSomethingElse
```

when a proper state model can represent the situation.

Use the existing project's state architecture when one already exists.

---

# 10. NETWORKING

Reuse the existing:

- Retrofit instance
- OkHttp configuration
- authentication interceptor
- token refresh mechanism
- API service
- error handling
- authentication/session management

Do NOT create another Retrofit client or authentication system.

Do NOT duplicate API interfaces.

Do NOT bypass token handling.

---

# 11. AUTHENTICATION

The existing authentication architecture is authoritative.

Reuse:

- access token handling
- refresh token handling
- token rotation
- logout behavior
- authentication interceptor/authenticator
- existing local token storage

Do not implement a second authentication mechanism.

Do not store tokens insecurely.

Do not log access tokens, refresh tokens, passwords, OTPs, or sensitive authentication data.

---

# 12. NAVIGATION RULES

Do NOT modify existing navigation unless the task explicitly requires it.

Do NOT:

- rename existing routes
- delete destinations
- create duplicate destinations
- change bottom navigation structure
- introduce alternative navigation paths

If navigation must change:

1. inspect the current navigation graph,
2. determine the minimum required change,
3. preserve all existing destinations and behavior.

---

# 13. UI / UX RULES

BASARAT should feel like a professional modern financial application.

Prioritize:

- clear information hierarchy
- readable financial numbers
- compact but useful data presentation
- consistent spacing
- professional typography
- meaningful charts
- restrained card usage
- Material Components
- responsive XML layouts
- light/dark theme compatibility where already supported

Avoid:

- excessive cards
- excessive rounded containers
- random gradients
- unnecessary animations
- decorative elements that reduce information clarity
- huge empty spaces
- cluttered dashboards

Do not blindly copy external designs.

Use references only for visual/UX inspiration.

Maintain BASARAT's own design system.

---

# 14. XML RULES

The project uses XML.

Use:

- ConstraintLayout where appropriate
- RecyclerView for lists
- ViewBinding
- reusable XML components
- existing dimensions/colors/styles
- Material Components
- responsive constraints

Avoid:

- fixed screen-specific widths
- unnecessary deeply nested layouts
- duplicated XML components
- duplicated styles
- hardcoded visual values when an existing resource exists

Reuse existing resources before creating new ones.

---

# 15. HOME SCREEN

Home is a professional stock-market discovery/start screen.

Home is NOT the Portfolio screen.

Home should emphasize:

- stock search
- market pulse
- compact market trend
- watchlist preview
- market movers
- market news
- BASARAT intelligence/AI entry

Portfolio should handle:

- holdings
- portfolio value
- P/L
- allocations
- transactions
- detailed portfolio analytics

Markets should handle:

- full PSX market overview
- indices
- complete market charts
- gainers
- losers
- most active
- market sentiment
- broader market research

Do not turn Home into Portfolio 2.0.

Do not turn Home into Markets 2.0.

---

# 16. STOCK ANALYSIS

Stock Analysis contains:

- Technical
- Fundamentals
- Forecast
- Sentiment

Prefer one Stock Analysis destination with compact analysis tabs unless the existing architecture explicitly requires otherwise.

Technical should use actual technical-indicator backend data.

Fundamentals should use actual stock overview/fundamental backend data.

Forecast should use actual forecast backend data.

Sentiment should use actual sentiment backend data.

Do not fabricate missing metrics.

---

# 17. LOADING / PERFORMANCE

Do not make the entire application wait for every backend request.

For screens containing independent API sections:

- load independent requests concurrently where appropriate
- progressively render sections
- use section-level loading states
- prevent duplicate API calls
- avoid unnecessary reloads
- cache only where appropriate
- lazy-load expensive below-the-fold content when appropriate
- keep existing data visible during refresh
- avoid blocking the main thread
- optimize chart rendering
- use efficient RecyclerView/ListAdapter/DiffUtil patterns where appropriate

Do NOT solve performance problems by adding mock data.

Fast UI must still use real backend data.

---

# 18. ERROR HANDLING

Handle:

```text
Loading
Success
Empty
Error
```

appropriately.

If one independent API fails, do not automatically blank the entire screen.

Example:

```text
Market → Success
Watchlist → Success
News → Error
```

The user should still be able to use Market and Watchlist.

Only the News section should display an error/retry state.

Never silently replace API failures with fake data.

---

# 19. CHART RULES

Charts must use real backend data.

Do not generate artificial chart points.

Do not create random values to make a chart look populated.

Do not blindly transform scales.

If backend returns a score/value in a specific unit or scale, preserve that documented meaning.

Only calculate derived values if the product/backend architecture explicitly requires it.

---

# 20. RECYCLERVIEW PERFORMANCE

For lists:

Prefer:

- ListAdapter
- DiffUtil
- efficient ViewHolders
- reusable item layouts

Avoid unnecessary:

```text
notifyDataSetChanged()
```

Avoid recreating adapters on every state emission.

Update only the relevant data.

---

# 21. COROUTINE RULES

Use structured concurrency.

Do NOT use:

```text
GlobalScope
```

Do not launch unmanaged coroutines.

Respect ViewModel/Fragment lifecycle.

Cancel work appropriately when no longer needed.

Do not create duplicate API requests from multiple lifecycle callbacks.

---

# 22. SCOPE CONTROL

When asked to implement a feature:

ONLY modify what is required.

Do NOT:

- refactor unrelated modules
- rename unrelated classes
- redesign unrelated screens
- replace working libraries
- migrate architecture
- change navigation unnecessarily
- update dependencies without reason
- modify backend contracts without authorization

If you discover an unrelated issue:

Report it instead of silently changing it.

---

# 23. EXISTING CODE TAKES PRIORITY OVER ASSUMPTIONS

Before changing anything, inspect the actual repository.

Do not assume:

- file names
- package names
- class names
- navigation IDs
- ViewModel names
- API response fields
- repository names
- XML IDs
- architecture structure

Use the codebase as the source of truth.

---

# 24. BEFORE IMPLEMENTING A TASK

Follow this workflow:

### Step 1 — Understand

Read the relevant source files.

### Step 2 — Inspect

Find existing architecture and reusable components.

### Step 3 — Verify

Verify API/backend requirements if applicable.

### Step 4 — Plan

Identify the smallest set of files that need modification.

### Step 5 — Implement

Make the requested change.

### Step 6 — Test

Build/test the affected functionality.

### Step 7 — Review

Check for:

- hardcoded values
- duplicate logic
- architecture violations
- broken navigation
- unnecessary API calls
- lifecycle issues
- unrelated modifications

### Step 8 — Report

Briefly explain:

- what changed
- which files changed
- what was tested
- any remaining issue

---

# 25. NEVER CLAIM SOMETHING WAS TESTED IF IT WAS NOT

If an API was not actually called, do not say it was tested.

If the app was not built, do not say the build passed.

If a screen was not manually verified, do not claim it was verified.

Be explicit about what was:

- inspected
- implemented
- compiled
- tested
- manually verified

---

# 26. GIT SAFETY

Before making a large or risky change:

Inspect the current Git status.

Do not overwrite unrelated user changes.

Do not reset/discard existing work without explicit permission.

Do not force-push.

Do not rewrite Git history unless explicitly requested.

Prefer focused commits for major features.

Recommended branch naming:

```text
feature/<feature-name>
fix/<issue-name>
refactor/<scope>
```

---

# 27. FINAL RULE

The existing BASARAT project is a working application.

Your job is to:

**UNDERSTAND → PRESERVE → IMPROVE**

not:

**REWRITE → REPLACE → RESTRUCTURE**

Always prefer the smallest safe change that satisfies the requested task.

When uncertain about an existing behavior, inspect the code first rather than inventing a new behavior.