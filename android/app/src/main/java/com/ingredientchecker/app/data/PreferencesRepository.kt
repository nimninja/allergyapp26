package com.ingredientchecker.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "restrictions")

data class UserRestrictions(
    val activeProfiles: Set<String> = emptySet(),
    val profileToggles: Map<String, Boolean> = emptyMap(),
    val selectedAllergens: Set<String> = emptySet(),
    val manualAllergens: Set<String> = emptySet(),
    val excludedAllergens: Set<String> = emptySet(),
    val vegan: Boolean = false,
    val vegetarian: Boolean = false,
    val extraAvoid: String = "",
    val manualExtraAvoid: String = "",
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val activeProfiles = stringSetPreferencesKey("active_profiles")
        val profileToggles = stringSetPreferencesKey("profile_toggles")
        val allergens = stringSetPreferencesKey("allergens")
        val manualAllergens = stringSetPreferencesKey("manual_allergens")
        val excludedAllergens = stringSetPreferencesKey("excluded_allergens")
        val vegan = booleanPreferencesKey("vegan")
        val vegetarian = booleanPreferencesKey("vegetarian")
        val extraAvoid = stringPreferencesKey("extra_avoid")
        val manualExtraAvoid = stringPreferencesKey("manual_extra_avoid")
    }

    val restrictions: Flow<UserRestrictions> = context.dataStore.data.map { prefs ->
        UserRestrictions(
            activeProfiles = prefs[Keys.activeProfiles] ?: emptySet(),
            profileToggles = decodeToggles(prefs[Keys.profileToggles]),
            selectedAllergens = prefs[Keys.allergens] ?: emptySet(),
            manualAllergens = prefs[Keys.manualAllergens] ?: emptySet(),
            excludedAllergens = prefs[Keys.excludedAllergens] ?: emptySet(),
            vegan = prefs[Keys.vegan] ?: false,
            vegetarian = prefs[Keys.vegetarian] ?: false,
            extraAvoid = prefs[Keys.extraAvoid] ?: "",
            manualExtraAvoid = prefs[Keys.manualExtraAvoid] ?: "",
        )
    }

    suspend fun save(restrictions: UserRestrictions) {
        context.dataStore.edit { prefs ->
            prefs[Keys.activeProfiles] = restrictions.activeProfiles
            prefs[Keys.profileToggles] = encodeToggles(restrictions.profileToggles)
            prefs[Keys.allergens] = restrictions.selectedAllergens
            prefs[Keys.manualAllergens] = restrictions.manualAllergens
            prefs[Keys.excludedAllergens] = restrictions.excludedAllergens
            prefs[Keys.vegan] = restrictions.vegan
            prefs[Keys.vegetarian] = restrictions.vegetarian
            prefs[Keys.extraAvoid] = restrictions.extraAvoid
            prefs[Keys.manualExtraAvoid] = restrictions.manualExtraAvoid
        }
    }

    private fun encodeToggles(map: Map<String, Boolean>): Set<String> =
        map.map { (key, value) -> "$key=$value" }.toSet()

    private fun decodeToggles(raw: Set<String>?): Map<String, Boolean> {
        if (raw.isNullOrEmpty()) return emptyMap()
        return raw.mapNotNull { entry ->
            val idx = entry.lastIndexOf('=')
            if (idx <= 0) return@mapNotNull null
            val key = entry.substring(0, idx)
            val value = entry.substring(idx + 1).toBooleanStrictOrNull() ?: return@mapNotNull null
            key to value
        }.toMap()
    }
}
