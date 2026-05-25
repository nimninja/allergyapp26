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
    val selectedAllergens: Set<String> = emptySet(),
    val vegan: Boolean = false,
    val vegetarian: Boolean = false,
    val extraAvoid: String = "",
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val allergens = stringSetPreferencesKey("allergens")
        val vegan = booleanPreferencesKey("vegan")
        val vegetarian = booleanPreferencesKey("vegetarian")
        val extraAvoid = stringPreferencesKey("extra_avoid")
    }

    val restrictions: Flow<UserRestrictions> = context.dataStore.data.map { prefs ->
        UserRestrictions(
            selectedAllergens = prefs[Keys.allergens] ?: emptySet(),
            vegan = prefs[Keys.vegan] ?: false,
            vegetarian = prefs[Keys.vegetarian] ?: false,
            extraAvoid = prefs[Keys.extraAvoid] ?: "",
        )
    }

    suspend fun save(restrictions: UserRestrictions) {
        context.dataStore.edit { prefs ->
            prefs[Keys.allergens] = restrictions.selectedAllergens
            prefs[Keys.vegan] = restrictions.vegan
            prefs[Keys.vegetarian] = restrictions.vegetarian
            prefs[Keys.extraAvoid] = restrictions.extraAvoid
        }
    }
}
