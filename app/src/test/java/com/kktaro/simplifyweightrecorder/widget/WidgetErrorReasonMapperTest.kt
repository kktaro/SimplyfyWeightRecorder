package com.kktaro.simplifyweightrecorder.widget

import com.kktaro.simplifyweightrecorder.domain.model.WeightSaveError
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetErrorReasonMapperTest {

    @Test
    fun `maps PermissionDenied`() {
        assertEquals(
            WidgetErrorReason.PermissionDenied,
            WeightSaveError.PermissionDenied.toWidgetErrorReason()
        )
    }

    @Test
    fun `maps HealthConnectUnavailable`() {
        assertEquals(
            WidgetErrorReason.HealthConnectUnavailable,
            WeightSaveError.HealthConnectUnavailable.toWidgetErrorReason()
        )
    }

    @Test
    fun `maps Unknown to Unknown with cause message`() {
        val cause = IOException("io error")
        assertEquals(
            WidgetErrorReason.Unknown("io error"),
            WeightSaveError.Unknown(cause).toWidgetErrorReason()
        )
    }

    @Test
    fun `maps generic Throwable to Unknown with throwable message`() {
        val throwable = IllegalStateException("oops")
        assertEquals(
            WidgetErrorReason.Unknown("oops"),
            throwable.toWidgetErrorReason()
        )
    }
}
