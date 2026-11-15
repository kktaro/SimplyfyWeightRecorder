package com.kktaro.simplifyweightrecorder.domain.model

import java.time.LocalDate

/** 体重推移を表示する期間。 */
enum class TrendPeriod {
    WEEK,
    MONTH,
    THREE_MONTHS,
    YEAR;

    /** [today] を末日とした表示期間の初日。 */
    fun startDate(today: LocalDate): LocalDate = when (this) {
        WEEK -> today.minusDays(6)
        MONTH -> today.minusMonths(1).plusDays(1)
        THREE_MONTHS -> today.minusMonths(3).plusDays(1)
        YEAR -> today.minusYears(1).plusDays(1)
    }
}
