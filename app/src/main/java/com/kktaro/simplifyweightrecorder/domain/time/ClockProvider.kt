package com.kktaro.simplifyweightrecorder.domain.time

import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

interface ClockProvider {
    fun nowInTokyo(): ZonedDateTime
}

class SystemClockProvider @Inject constructor() : ClockProvider {
    override fun nowInTokyo(): ZonedDateTime =
        ZonedDateTime.now(ZoneId.of("Asia/Tokyo"))
}
