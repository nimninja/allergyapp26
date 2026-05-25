package com.ingredientchecker.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.ingredientchecker.app.ui.MainScreen
import com.ingredientchecker.app.ui.MainViewModel
import com.ingredientchecker.app.ui.theme.IngredientCheckerTheme
import java.io.File

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var pendingCameraUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setImage(uri)
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) viewModel.setImage(pendingCameraUri)
    }

    private val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IngredientCheckerTheme {
                MainScreen(
                    viewModel = viewModel,
                    onPickImage = { pickImage.launch(ActivityResultContracts.PickVisualMedia.ImageOnly) },
                    onTakePhoto = ::startCamera,
                )
            }
        }
    }

    private fun startCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        pendingCameraUri = createCameraUri()
        takePhoto.launch(pendingCameraUri)
    }

    private fun createCameraUri(): Uri {
        val file = File.createTempFile("capture_", ".jpg", cacheDir)
        return FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
    }
}
