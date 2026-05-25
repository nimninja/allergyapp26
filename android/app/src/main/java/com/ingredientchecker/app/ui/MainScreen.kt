package com.ingredientchecker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ingredientchecker.app.data.ScanResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ingredient Checker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.loadingAllergens) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }

            state.error?.let { msg ->
                Text(msg, color = MaterialTheme.colorScheme.error)
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Your restrictions", fontWeight = FontWeight.SemiBold)
                    state.allergens.forEach { allergen ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = allergen.id in state.restrictions.selectedAllergens,
                                onCheckedChange = { viewModel.toggleAllergen(allergen.id, it) },
                            )
                            Text(allergen.label)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.restrictions.vegan,
                            onCheckedChange = viewModel::setVegan,
                        )
                        Text("Vegan")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.restrictions.vegetarian,
                            onCheckedChange = viewModel::setVegetarian,
                        )
                        Text("Vegetarian")
                    }
                    OutlinedTextField(
                        value = state.restrictions.extraAvoid,
                        onValueChange = viewModel::setExtraAvoid,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Also avoid (comma separated)") },
                        minLines = 2,
                    )
                    if (state.disclaimer.isNotBlank()) {
                        Text(state.disclaimer, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Label photo", fontWeight = FontWeight.SemiBold)
                    state.imageUri?.let { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "Label preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onTakePhoto) { Text("Camera") }
                        OutlinedButton(onClick = onPickImage) { Text("Gallery") }
                    }
                    Button(
                        onClick = viewModel::scan,
                        enabled = state.imageUri != null && !state.scanning,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.scanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.height(0.dp))
                        } else {
                            Text("Analyze label")
                        }
                    }
                    if (state.scanning) {
                        Text("Reading label… first scan can take 20–40 seconds.")
                    }
                }
            }

            state.scanResult?.let { result ->
                ScanResultCard(result)
            }
        }
    }
}

@Composable
private fun ScanResultCard(result: ScanResponse) {
    val summaryColor = when (result.summaryLevel) {
        "violation", "no_text" -> MaterialTheme.colorScheme.error
        "warning" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(result.summary, color = summaryColor, fontWeight = FontWeight.SemiBold)

            if (result.mayContainNotices.isNotEmpty()) {
                Text("May contain / facility", fontWeight = FontWeight.Medium)
                result.mayContainNotices.forEach { Text("• $it") }
            }
            if (result.violations.isNotEmpty()) {
                Text("Violations", fontWeight = FontWeight.Medium)
                result.violations.forEach { Text("• ${it.category}: “${it.keyword}”") }
            }
            if (result.warnings.isNotEmpty()) {
                Text("Review manually", fontWeight = FontWeight.Medium)
                result.warnings.forEach { Text("• ${it.term}: ${it.reason}") }
            }
            Text("OCR text", fontWeight = FontWeight.Medium)
            Text(result.rawText, style = MaterialTheme.typography.bodySmall)
        }
    }
}
