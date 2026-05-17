package com.kktaro.simplifyweightrecorder.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.firstOrNull

private val Context.lastWeightDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "last_weight_preferences")

private val LAST_WEIGHT_KG_KEY = floatPreferencesKey("last_weight_kg")

@Singleton
class DataStoreLastWeightRepository @Inject constructor(
    @ApplicationContext context: Context
) : LastWeightRepository {

    private val store: DataStore<Preferences> = context.lastWeightDataStore

    override suspend fun getLastWeight(): Double? =
        store.data.firstOrNull()?.get(LAST_WEIGHT_KG_KEY)?.toDouble()

    override suspend fun setLastWeight(kg: Double) {
        store.edit { it[LAST_WEIGHT_KG_KEY] = kg.toFloat() }
    }
}
