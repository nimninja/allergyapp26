package com.ingredientchecker.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ingredientchecker.app.data.AllergenOption
import com.ingredientchecker.app.data.DietProfile
import com.ingredientchecker.app.data.DietProfileRepository
import com.ingredientchecker.app.data.PreferencesRepository
import com.ingredientchecker.app.data.ScanResponse
import com.ingredientchecker.app.data.UserRestrictions
import com.ingredientchecker.app.domain.AppConstants
import com.ingredientchecker.app.domain.DietProfileResolver
import com.ingredientchecker.app.domain.LocalScanService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val allergens: List<AllergenOption> = emptyList(),
    val dietProfiles: List<DietProfile> = emptyList(),
    val disclaimer: String = AppConstants.DISCLAIMER,
    val restrictions: UserRestrictions = UserRestrictions(),
    val imageUri: Uri? = null,
    val scanning: Boolean = false,
    val error: String? = null,
    val scanResult: ScanResponse? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesRepository(application)
    private val profileRepo = DietProfileRepository(application)
    private val scanService = LocalScanService(application)
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.restrictions.collect { saved ->
                _state.update { current ->
                    current.copy(restrictions = reapplyProfiles(saved))
                }
            }
        }
        loadAllergens()
        loadProfiles()
    }

    fun loadAllergens() {
        val options = AppConstants.ALLERGEN_OPTIONS.map { (id, label) ->
            AllergenOption(id, label)
        }
        _state.update {
            it.copy(
                allergens = options,
                disclaimer = AppConstants.DISCLAIMER,
                error = null,
            )
        }
    }

    private fun loadProfiles() {
        _state.update { it.copy(dietProfiles = profileRepo.allProfiles()) }
    }

    fun toggleProfile(profileId: String) {
        updateRestrictions { current ->
            val active = current.activeProfiles.toMutableSet()
            val toggles = current.profileToggles.toMutableMap()
            if (profileId in active) {
                active.remove(profileId)
            } else {
                active.add(profileId)
                profileRepo.profileById(profileId)?.let { profile ->
                    toggles.putAll(DietProfileResolver.defaultTogglesForProfile(profile))
                }
            }
            reapplyProfiles(
                current.copy(
                    activeProfiles = active,
                    profileToggles = toggles,
                ),
            )
        }
    }

    fun setProfileToggle(profileId: String, toggleId: String, enabled: Boolean) {
        updateRestrictions { current ->
            val toggles = current.profileToggles.toMutableMap()
            toggles[DietProfileResolver.toggleKey(profileId, toggleId)] = enabled
            reapplyProfiles(current.copy(profileToggles = toggles))
        }
    }

    fun toggleAllergen(id: String, checked: Boolean) {
        updateRestrictions { current ->
            val manual = current.manualAllergens.toMutableSet()
            val excluded = current.excludedAllergens.toMutableSet()
            val profileAllergens = profileAllergensFor(current)
            if (checked) {
                excluded.remove(id)
                if (id !in profileAllergens) manual.add(id)
            } else {
                manual.remove(id)
                if (id in profileAllergens) excluded.add(id)
            }
            reapplyProfiles(
                current.copy(
                    manualAllergens = manual,
                    excludedAllergens = excluded,
                ),
            )
        }
    }

    fun setExtraAvoid(text: String) {
        updateRestrictions { current ->
            reapplyProfiles(current.copy(manualExtraAvoid = text))
        }
    }

    fun setImage(uri: Uri?) {
        _state.update { it.copy(imageUri = uri, scanResult = null, error = null) }
    }

    fun scan() {
        val uri = _state.value.imageUri ?: return
        viewModelScope.launch {
            _state.update { it.copy(scanning = true, error = null, scanResult = null) }
            try {
                val result = scanService.scan(uri, _state.value.restrictions)
                _state.update { it.copy(scanning = false, scanResult = result) }
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown error"
                _state.update {
                    it.copy(scanning = false, error = "Scan failed: $msg")
                }
            }
        }
    }

    private fun updateRestrictions(transform: (UserRestrictions) -> UserRestrictions) {
        _state.update { current ->
            val updated = transform(current.restrictions)
            persist(updated)
            current.copy(restrictions = updated)
        }
    }

    private fun persist(restrictions: UserRestrictions) {
        viewModelScope.launch { prefs.save(restrictions) }
    }

    private fun reapplyProfiles(base: UserRestrictions): UserRestrictions {
        val profiles = _state.value.dietProfiles.ifEmpty { profileRepo.allProfiles() }
        val effects = base.activeProfiles.mapNotNull { id ->
            profiles.find { it.id == id }?.let {
                DietProfileResolver.effectForProfile(it, base.profileToggles)
            }
        }
        val merged = DietProfileResolver.mergeEffects(effects)
        val allergens = merged.allergens + base.manualAllergens - base.excludedAllergens
        val manualTerms = AppConstants.parseExtraAvoid(base.manualExtraAvoid)
        val extraAvoid = (merged.extraAvoid + manualTerms).distinct().joinToString(", ")
        return base.copy(
            selectedAllergens = allergens,
            vegan = merged.vegan,
            vegetarian = merged.vegetarian,
            extraAvoid = extraAvoid,
        )
    }

    private fun profileAllergensFor(restrictions: UserRestrictions): Set<String> {
        val profiles = _state.value.dietProfiles.ifEmpty { profileRepo.allProfiles() }
        val effects = restrictions.activeProfiles.mapNotNull { id ->
            profiles.find { it.id == id }?.let {
                DietProfileResolver.effectForProfile(it, restrictions.profileToggles)
            }
        }
        return DietProfileResolver.mergeEffects(effects).allergens
    }
}
