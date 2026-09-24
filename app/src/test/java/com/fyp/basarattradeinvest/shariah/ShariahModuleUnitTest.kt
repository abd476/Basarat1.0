package com.fyp.basarattradeinvest.shariah

import com.fyp.basarattradeinvest.data.mapper.ShariahDtoMapper
import com.fyp.basarattradeinvest.data.remote.api.ShariahApiService
import com.fyp.basarattradeinvest.data.remote.dto.shariah.Kmi30ConstituentDto
import com.fyp.basarattradeinvest.data.remote.dto.shariah.ShariahCriteriaResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.shariah.ShariahCriterionDto
import com.fyp.basarattradeinvest.data.remote.dto.shariah.ShariahKmi30ResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.shariah.ShariahPurificationResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.shariah.ShariahScreeningResponseDto
import com.fyp.basarattradeinvest.data.repository.ShariahRepositoryImpl
import com.fyp.basarattradeinvest.domain.usecase.shariah.CalculateDividendPurificationUseCase
import com.fyp.basarattradeinvest.domain.usecase.shariah.GetKmi30ConstituentsUseCase
import com.fyp.basarattradeinvest.domain.usecase.shariah.GetStockShariahCriteriaUseCase
import com.fyp.basarattradeinvest.domain.usecase.shariah.GetStockShariahScreeningUseCase
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ShariahModuleUnitTest {

    private class FakeShariahApiService : ShariahApiService {
        var shouldFail: Boolean = false

        override suspend fun getKmi30Constituents(): Response<ShariahKmi30ResponseDto> {
            if (shouldFail) {
                return Response.error(500, "{\"detail\":\"Internal Error\"}".toResponseBody("application/json".toMediaTypeOrNull()))
            }
            val constituents = listOf(
                Kmi30ConstituentDto(
                    symbol = "LUCK",
                    name = "Lucky Cement Limited",
                    current = 885.20,
                    change = 12.5,
                    changePct = 1.45,
                    weightPct = 8.50,
                    sector = "Cement",
                    purificationRate = 0.0012,
                    isShariahCompliant = true
                ),
                Kmi30ConstituentDto(
                    symbol = "SYS",
                    name = "Systems Limited",
                    current = 442.80,
                    change = 24.5,
                    changePct = 5.86,
                    weightPct = 6.20,
                    sector = "Technology",
                    purificationRate = 0.0,
                    isShariahCompliant = true
                )
            )
            return Response.success(
                ShariahKmi30ResponseDto(
                    index = "KMI-30",
                    totalConstituents = 2,
                    constituents = constituents
                )
            )
        }

        override suspend fun getShariahScreening(symbol: String): Response<ShariahScreeningResponseDto> {
            if (shouldFail) {
                return Response.error(404, "{\"detail\":\"Symbol not found\"}".toResponseBody("application/json".toMediaTypeOrNull()))
            }
            val isCompliant = !symbol.equals("HBL", ignoreCase = true)
            return Response.success(
                ShariahScreeningResponseDto(
                    symbol = symbol,
                    isShariahCompliant = isCompliant,
                    overallScore = if (isCompliant) 94.5 else 32.0,
                    screeningMethod = "AAOIFI & KMI-30 Standard",
                    screenedAt = "2026-09-24T00:00:00Z",
                    sector = if (isCompliant) "Cement" else "Conventional Banking",
                    purificationRate = if (isCompliant) 0.0012 else 0.0,
                    complianceSummary = if (isCompliant) "Meets all KMI criteria." else "Non-compliant business activity."
                )
            )
        }

        override suspend fun getShariahCriteria(symbol: String): Response<ShariahCriteriaResponseDto> {
            if (shouldFail) {
                return Response.error(500, "{\"detail\":\"Server error\"}".toResponseBody("application/json".toMediaTypeOrNull()))
            }
            val isCompliant = !symbol.equals("HBL", ignoreCase = true)
            val criteria = listOf(
                ShariahCriterionDto("Core Business Activity", 0.0, 0.0, isCompliant, "Halal core operations"),
                ShariahCriterionDto("Debt to Total Assets", 37.0, if (isCompliant) 18.4 else 85.0, isCompliant, "≤ 37% debt"),
                ShariahCriterionDto("Non-Compliant Investments", 33.0, if (isCompliant) 8.2 else 45.0, isCompliant, "≤ 33% non-compliant investments"),
                ShariahCriterionDto("Non-Compliant Income", 5.0, if (isCompliant) 0.45 else 65.0, isCompliant, "≤ 5% non-compliant income"),
                ShariahCriterionDto("Illiquid Assets Ratio", 25.0, 58.0, true, "≥ 25% tangible assets"),
                ShariahCriterionDto("Net Liquid Assets to Market Cap", 100.0, 32.0, true, "≤ 100% market cap")
            )
            return Response.success(
                ShariahCriteriaResponseDto(
                    symbol = symbol,
                    isShariahCompliant = isCompliant,
                    criteria = criteria
                )
            )
        }

        override suspend fun getShariahPurification(
            symbol: String,
            holdingQty: Int,
            holdingValue: Double
        ): Response<ShariahPurificationResponseDto> {
            if (shouldFail) {
                return Response.error(400, "{\"detail\":\"Invalid holding\"}".toResponseBody("application/json".toMediaTypeOrNull()))
            }
            val rate = 0.0012
            val amount = holdingValue * rate
            return Response.success(
                ShariahPurificationResponseDto(
                    symbol = symbol,
                    holdingQty = holdingQty,
                    holdingValue = holdingValue,
                    purificationAmount = amount,
                    purificationRate = rate,
                    notes = "Deduct from dividend income."
                )
            )
        }
    }

    private lateinit var fakeApi: FakeShariahApiService
    private lateinit var repository: ShariahRepositoryImpl

    @Before
    fun setUp() {
        fakeApi = FakeShariahApiService()
        repository = ShariahRepositoryImpl(fakeApi)
    }

    @Test
    fun testShariahDtoMapper_mapsKmi30ResponseCorrectly() {
        val dto = ShariahKmi30ResponseDto(
            index = "KMI-30",
            totalConstituents = 1,
            constituents = listOf(
                Kmi30ConstituentDto(
                    symbol = "LUCK",
                    name = "Lucky Cement Limited",
                    current = 885.20,
                    change = 12.5,
                    changePct = 1.45,
                    weightPct = 8.50,
                    sector = "Cement",
                    purificationRate = 0.0012,
                    isShariahCompliant = true
                )
            )
        )

        val summary = ShariahDtoMapper.mapKmi30Response(dto)
        assertEquals("KMI-30", summary.index)
        assertEquals(1, summary.totalConstituents)
        assertEquals(1, summary.constituents.size)

        val item = summary.constituents.first()
        assertEquals("LUCK", item.symbol)
        assertEquals(885.20, item.currentPrice, 0.001)
        assertEquals(1.45, item.changePct, 0.001)
        assertEquals("0.1200%", item.purificationRate)
        assertTrue(item.isCompliant)
    }

    @Test
    fun testShariahDtoMapper_mapsScreeningAndCriteria() {
        val screeningDto = ShariahScreeningResponseDto(
            symbol = "OGDC",
            isShariahCompliant = true,
            overallScore = 98.0,
            screeningMethod = "KMI-30",
            screenedAt = "2026-09-24",
            sector = "Oil & Gas",
            purificationRate = 0.0,
            complianceSummary = "Fully compliant"
        )
        val compliance = ShariahDtoMapper.mapScreeningResponse(screeningDto)
        assertEquals("OGDC", compliance.symbol)
        assertTrue(compliance.isCompliant)
        assertEquals(98.0, compliance.overallScore!!, 0.001)

        val criteriaDto = ShariahCriteriaResponseDto(
            symbol = "OGDC",
            isShariahCompliant = true,
            criteria = listOf(
                ShariahCriterionDto("Debt to Total Assets", 37.0, 5.0, true, "Low debt")
            )
        )
        val criteriaResult = ShariahDtoMapper.mapCriteriaResponse(criteriaDto)
        assertEquals(1, criteriaResult.criteria.size)
        assertTrue(criteriaResult.criteria.first().passed)
        assertEquals(37.0, criteriaResult.criteria.first().threshold, 0.001)
    }

    @Test
    fun testShariahDtoMapper_mapsPurification() {
        val purifDto = ShariahPurificationResponseDto(
            symbol = "LUCK",
            holdingQty = 500,
            holdingValue = 250000.0,
            purificationAmount = 300.0,
            purificationRate = 0.0012,
            notes = "Purify dividend"
        )
        val domain = ShariahDtoMapper.mapPurificationResponse(purifDto)
        assertEquals(500, domain.holdingQty)
        assertEquals(250000.0, domain.holdingValue, 0.001)
        assertEquals(300.0, domain.purificationAmount, 0.001)
        assertEquals(0.0012, domain.purificationRate, 0.0001)
        assertEquals("Purify dividend", domain.notes)
    }

    @Test
    fun testRepository_getKmi30Constituents_success() = runBlocking {
        val result = repository.getKmi30Constituents()
        assertTrue(result.isSuccess)
        val summary = result.getOrNull()
        assertNotNull(summary)
        assertEquals(2, summary?.constituents?.size)
        assertEquals("LUCK", summary?.constituents?.first()?.symbol)
    }

    @Test
    fun testRepository_calculatePurification_success() = runBlocking {
        val result = repository.calculatePurification("LUCK", 100, 50000.0)
        assertTrue(result.isSuccess)
        val purif = result.getOrNull()
        assertNotNull(purif)
        assertEquals(60.0, purif?.purificationAmount!!, 0.01)
    }

    @Test
    fun testRepository_calculatePurification_failureOnApiError() = runBlocking {
        fakeApi.shouldFail = true
        val result = repository.calculatePurification("LUCK", 100, 50000.0)
        assertTrue(result.isFailure)
    }

    @Test
    fun testUseCases_executeSuccessfully() = runBlocking {
        val kmiUseCase = GetKmi30ConstituentsUseCase(repository)
        val screeningUseCase = GetStockShariahScreeningUseCase(repository)
        val criteriaUseCase = GetStockShariahCriteriaUseCase(repository)
        val purifUseCase = CalculateDividendPurificationUseCase(repository)

        val kmiResult = kmiUseCase()
        assertTrue(kmiResult.isSuccess)

        val screeningResult = screeningUseCase("LUCK")
        assertTrue(screeningResult.isSuccess)
        assertTrue(screeningResult.getOrNull()?.isCompliant == true)

        val nonCompliantResult = screeningUseCase("HBL")
        assertTrue(nonCompliantResult.isSuccess)
        assertFalse(nonCompliantResult.getOrNull()?.isCompliant == true)

        val criteriaResult = criteriaUseCase("LUCK")
        assertTrue(criteriaResult.isSuccess)
        assertEquals(6, criteriaResult.getOrNull()?.criteria?.size)

        val purifResult = purifUseCase("LUCK", 200, 100000.0)
        assertTrue(purifResult.isSuccess)
        assertEquals(120.0, purifResult.getOrNull()?.purificationAmount!!, 0.01)
    }

    @Test
    fun testPurificationUseCase_validatesInput() = runBlocking {
        val purifUseCase = CalculateDividendPurificationUseCase(repository)
        try {
            purifUseCase("LUCK", 0, 1000.0)
            org.junit.Assert.fail("Expected IllegalArgumentException for zero quantity")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("quantity") == true)
        }

        try {
            purifUseCase("LUCK", 10, -50.0)
            org.junit.Assert.fail("Expected IllegalArgumentException for negative holding value")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("value") == true)
        }
    }

    @Test
    fun testRepository_fallbackToSampleData_whenApiFails() = runBlocking {
        fakeApi.shouldFail = true
        val result = repository.getShariahCompliantStocks()
        assertTrue(result.isSuccess)
        val stocks = result.getOrNull()
        assertNotNull(stocks)
        assertTrue(stocks!!.isNotEmpty())
        assertEquals("MEBL", stocks.first().symbol)
        assertTrue(stocks.any { it.symbol == "LUCK" })
    }

    @Test
    fun testRepository_purificationCalculationFailure_whenApiFails() = runBlocking {
        fakeApi.shouldFail = true
        val result = repository.calculatePurification("LUCK", 100, 50000.0)
        assertTrue(result.isFailure)
    }
}
