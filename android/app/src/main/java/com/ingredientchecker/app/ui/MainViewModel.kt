package com.ingredientchecker.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ingredientchecker.app.data.AllergenOption
import com.ingredientchecker.app.data.PreferencesRepository
import com.ingredientchecker.app.data.ScanResponse
import com.ingredientchecker.app.data.UserRestrictions
import com.ingredientchecker.app.domain.AppConstants
import com.ingredientchecker.app.domain.LocalScanService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val allergens: List<AllergenOption> = emptyList(),
    val disclaimer: String = AppConstants.DISCLAIMER,
    val restrictions: UserRestrictions = UserRestrictions(),
    val imageUri: Uri? = null,
    val scanning: Boolean = false,
    val error: String? = null,
    val scanResult: ScanResponse? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesRepository(application)
    private val scanService = LocalScanService(application)
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.restrictions.collect { saved ->
                _state.update { it.copy(restrictions = saved) }
            }
        }
        loadAllergens()
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

    fun toggleAllergen(id: String, checked: Boolean) {
        _state.update { current ->
            val next = current.restrictions.selectedAllergens.toMutableSet()
            if (checked) next.add(id) else next.remove(id)
            val updated = current.restrictions.copy(selectedAllergens = next)
            persist(updated)
            current.copy(restrictions = updated)
        }
    }

    fun setVegan(checked: Boolean) = updateRestrictions { it.copy(vegan = checked) }
    fun setVegetarian(checked: Boolean) = updateRestrictions { it.copy(vegetarian = checked) }
    fun setExtraAvoid(text: String) = updateRestrictions { it.copy(extraAvoid = text) }

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
}
