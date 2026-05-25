package com.ingredientchecker.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ingredientchecker.app.data.AllergenOption
import com.ingredientchecker.app.data.ApiClient
import com.ingredientchecker.app.data.PreferencesRepository
import com.ingredientchecker.app.data.ScanResponse
import com.ingredientchecker.app.data.UserRestrictions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

data class MainUiState(
    val loadingAllergens: Boolean = true,
    val allergens: List<AllergenOption> = emptyList(),
    val disclaimer: String = "",
    val restrictions: UserRestrictions = UserRestrictions(),
    val imageUri: Uri? = null,
    val scanning: Boolean = false,
    val error: String? = null,
    val scanResult: ScanResponse? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesRepository(application)
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
        viewModelScope.launch {
            _state.update { it.copy(loadingAllergens = true, error = null) }
            try {
                val response = ApiClient.api.getAllergens()
                _state.update {
                    it.copy(
                        loadingAllergens = false,
                        allergens = response.allergens,
                        disclaimer = response.disclaimer,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loadingAllergens = false,
                        error = "Could not reach API: ${e.message}",
                    )
                }
            }
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
                val file = uriToTempFile(uri)
                val part = MultipartBody.Part.createFormData(
                    "image",
                    file.name,
                    file.asRequestBody("image/jpeg".toMediaType()),
                )
                val r = _state.value.restrictions
                val result = ApiClient.api.scan(
                    image = part,
                    allergens = r.selectedAllergens.joinToString(",").toRequestBody("text/plain".toMediaType()),
                    vegan = r.vegan.toString().toRequestBody("text/plain".toMediaType()),
                    vegetarian = r.vegetarian.toString().toRequestBody("text/plain".toMediaType()),
                    extraAvoid = r.extraAvoid.toRequestBody("text/plain".toMediaType()),
                )
                _state.update { it.copy(scanning = false, scanResult = result) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(scanning = false, error = "Scan failed: ${e.message}")
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

    private fun uriToTempFile(uri: Uri): File {
        val context = getApplication<Application>()
        val out = File.createTempFile("label_", ".jpg", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(out).use { output -> input.copyTo(output) }
        } ?: throw IllegalArgumentException("Could not read image from URI")
        return out
    }
}
