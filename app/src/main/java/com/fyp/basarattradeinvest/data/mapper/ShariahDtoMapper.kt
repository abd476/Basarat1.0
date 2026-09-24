package com.fyp.basarattradeinvest.data.mapper

import com.fyp.basarattradeinvest.data.remote.dto.shariah.Kmi30ConstituentDto
import com.fyp.basarattradeinvest.data.remote.dto.shariah.ShariahCriteriaResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.shariah.ShariahCriterionDto
import com.fyp.basarattradeinvest.data.remote.dto.shariah.ShariahKmi30ResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.shariah.ShariahPurificationResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.shariah.ShariahScreeningResponseDto
import com.fyp.basarattradeinvest.domain.model.ShariahStock
import com.fyp.basarattradeinvest.domain.model.shariah.DividendPurificationResult
import com.fyp.basarattradeinvest.domain.model.shariah.Kmi30Constituent
import com.fyp.basarattradeinvest.domain.model.shariah.Kmi30IndexSummary
import com.fyp.basarattradeinvest.domain.model.shariah.ShariahCriterionItem
import com.fyp.basarattradeinvest.domain.model.shariah.StockShariahCompliance
import com.fyp.basarattradeinvest.domain.model.shariah.StockShariahCriteria
import java.util.Locale

object ShariahDtoMapper {

    fun mapKmi30Response(dto: ShariahKmi30ResponseDto): Kmi30IndexSummary {
        val list = dto.constituents.map { mapConstituent(it) }
        return Kmi30IndexSummary(
            index = dto.index ?: "KMI-30",
            totalConstituents = dto.totalConstituents ?: list.size,
            constituents = list
        )
    }

    fun mapConstituent(dto: Kmi30ConstituentDto): Kmi30Constituent {
        val price = dto.current ?: dto.ldcp ?: 0.0
        val changePct = dto.changePct ?: 0.0
        val change = dto.change ?: 0.0
        val weight = dto.weightPct ?: 0.0
        val purifRateStr = if (dto.purificationRate != null) {
            String.format(Locale.US, "%.4f%%", dto.purificationRate * 100)
        } else {
            "0.0000%"
        }

        return Kmi30Constituent(
            symbol = dto.symbol,
            name = dto.name ?: dto.symbol,
            currentPrice = price,
            change = change,
            changePct = changePct,
            weightPct = weight,
            sector = dto.sector ?: "PSX Constituent",
            isCompliant = dto.isShariahCompliant ?: true,
            purificationRate = purifRateStr
        )
    }

    fun mapConstituentToLegacyStock(item: Kmi30Constituent): ShariahStock {
        return ShariahStock(
            symbol = item.symbol,
            code = item.symbol,
            companyName = item.name,
            sector = item.sector,
            price = item.currentPrice,
            changePercent = item.changePct,
            purificationRate = item.purificationRate,
            isKmi30 = true,
            hasDividend = item.purificationRate != "0.0000%"
        )
    }

    fun mapScreeningResponse(dto: ShariahScreeningResponseDto): StockShariahCompliance {
        return StockShariahCompliance(
            symbol = dto.symbol,
            isCompliant = dto.isShariahCompliant,
            overallScore = dto.overallScore,
            screeningMethod = dto.screeningMethod ?: "AAOIFI & KMI-30 Standard",
            screenedAt = dto.screenedAt,
            sector = dto.sector ?: "General",
            purificationRate = dto.purificationRate,
            complianceSummary = dto.complianceSummary ?: if (dto.isShariahCompliant) {
                "Meets all financial and qualitative screening criteria under the KMI Shariah standard."
            } else {
                "Does not meet one or more required Shariah compliance criteria."
            }
        )
    }

    fun mapCriteriaResponse(dto: ShariahCriteriaResponseDto): StockShariahCriteria {
        val mappedCriteria = dto.criteria.map { mapCriterion(it) }
        return StockShariahCriteria(
            symbol = dto.symbol,
            isCompliant = dto.isShariahCompliant,
            criteria = mappedCriteria
        )
    }

    fun mapCriterion(dto: ShariahCriterionDto): ShariahCriterionItem {
        return ShariahCriterionItem(
            name = dto.name,
            threshold = dto.threshold,
            value = dto.value,
            passed = dto.passed,
            description = dto.description ?: ""
        )
    }

    fun mapPurificationResponse(dto: ShariahPurificationResponseDto): DividendPurificationResult {
        return DividendPurificationResult(
            symbol = dto.symbol,
            holdingQty = dto.holdingQty,
            holdingValue = dto.holdingValue,
            purificationAmount = dto.purificationAmount,
            purificationRate = dto.purificationRate,
            notes = dto.notes
        )
    }
}
