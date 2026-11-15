package com.kktaro.simplifyweightrecorder.data.repository

import com.kktaro.simplifyweightrecorder.domain.model.WeightMeasurement
import java.time.Instant

interface WeightHistoryRepository {
    /** [start] 以上 [end] 未満の体重記録を読む。失敗時は [com.kktaro.simplifyweightrecorder.domain.model.WeightReadError]。 */
    suspend fun readWeights(start: Instant, end: Instant): Result<List<WeightMeasurement>>

    suspend fun hasReadPermission(): Boolean

    /** 体重推移の表示に必要な権限（端末が対応していれば履歴読み取り権限も含む）。 */
    suspend fun readPermissionsToRequest(): Set<String>
}
