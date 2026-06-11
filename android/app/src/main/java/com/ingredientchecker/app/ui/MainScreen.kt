package com.ingredientchecker.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ingredientchecker.app.data.AllergenOption
import com.ingredientchecker.app.data.DietProfile
import com.ingredientchecker.app.data.ScanResponse
import com.ingredientchecker.app.data.UserRestrictions
import com.ingredientchecker.app.data.Violation
import com.ingredientchecker.app.data.Warning
import com.ingredientchecker.app.domain.DietProfileResolver
import com.ingredientchecker.app.domain.MatchMethod
import com.ingredientchecker.app.ui.theme.AuraDarkGradientBottom
import com.ingredientchecker.app.ui.theme.AuraDarkGradientTop
import com.ingredientchecker.app.ui.theme.AuraEmerald
import com.ingredientchecker.app.ui.theme.AuraError
import com.ingredientchecker.app.ui.theme.AuraErrorContainer
import com.ingredientchecker.app.ui.theme.AuraForest
import com.ingredientchecker.app.ui.theme.AuraGradientBottom
import com.ingredientchecker.app.ui.theme.AuraGradientMid
import com.ingredientchecker.app.ui.theme.AuraGradientTop
import com.ingredientchecker.app.ui.theme.AuraSuccessContainer
import com.ingredientchecker.app.ui.theme.AuraWarning
import com.ingredientchecker.app.ui.theme.AuraWarningContainer

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val dark = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (dark) {
                        listOf(AuraDarkGradientTop, AuraDarkGradientBottom)
                    } else {
                        listOf(AuraGradientTop, AuraGradientMid, AuraGradientBottom)
                    },
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppHeader()
            OfflineBadge()

            state.error?.let { msg ->
                ErrorBanner(msg)
            }

            RestrictionsSection(
                allergens = state.allergens,
                dietProfiles = state.dietProfiles,
                restrictions = state.restrictions,
                disclaimer = state.disclaimer,
                onToggleAllergen = viewModel::toggleAllergen,
                onToggleProfile = viewModel::toggleProfile,
                onProfileToggle = viewModel::setProfileToggle,
                onExtraAvoidChange = viewModel::setExtraAvoid,
            )

            ScanSection(
                imageUri = state.imageUri,
                scanning = state.scanning,
                onTakePhoto = onTakePhoto,
                onPickImage = onPickImage,
                onScan = viewModel::scan,
            )

            AnimatedVisibility(
                visible = state.scanResult != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                state.scanResult?.let { ScanResultCard(it) }
            }
        }
    }
}

@Composable
private fun AppHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Ingredient Checker",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Scan labels. Stay confident.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OfflineBadge() {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                "On-device scan · works offline",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = AuraErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AuraError.copy(alpha = 0.2f), MaterialTheme.shapes.medium),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = AuraError,
                modifier = Modifier.size(20.dp),
            )
            Text(message, style = MaterialTheme.typography.bodyMedium, color = AuraError)
        }
    }
}

@Composable
private fun AuraCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (dark) 0.92f else 0.95f),
        shadowElevation = if (dark) 0.dp else 2.dp,
        tonalElevation = if (dark) 2.dp else 0.dp,
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.large,
                )
                .padding(18.dp),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestrictionsSection(
    allergens: List<AllergenOption>,
    dietProfiles: List<DietProfile>,
    restrictions: UserRestrictions,
    disclaimer: String,
    onToggleAllergen: (String, Boolean) -> Unit,
    onToggleProfile: (String) -> Unit,
    onProfileToggle: (String, String, Boolean) -> Unit,
    onExtraAvoidChange: (String) -> Unit,
) {
    var expandedProfileId by remember { mutableStateOf<String?>(null) }

    AuraCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle("Your restrictions")

            Text(
                "Diet profiles",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Tap a profile, then expand to customize what it checks for.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                dietProfiles.forEach { profile ->
                    val selected = profile.id in restrictions.activeProfiles
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val wasSelected = selected
                            onToggleProfile(profile.id)
                            expandedProfileId = when {
                                wasSelected -> if (expandedProfileId == profile.id) null else expandedProfileId
                                else -> profile.id
                            }
                        },
                        label = { Text(profile.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AuraEmerald,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }

            restrictions.activeProfiles.forEach { profileId ->
                dietProfiles.find { it.id == profileId }?.let { profile ->
                    ProfileDetailPanel(
                        profile = profile,
                        restrictions = restrictions,
                        expanded = expandedProfileId == profile.id,
                        onExpandClick = {
                            expandedProfileId = if (expandedProfileId == profile.id) null else profile.id
                        },
                        onProfileToggle = onProfileToggle,
                    )
                }
            }

            Text(
                "Allergens",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                allergens.forEach { allergen ->
                    val selected = allergen.id in restrictions.selectedAllergens
                    FilterChip(
                        selected = selected,
                        onClick = { onToggleAllergen(allergen.id, !selected) },
                        label = {
                            Text(
                                allergen.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }

            OutlinedTextField(
                value = restrictions.extraAvoid,
                onValueChange = onExtraAvoidChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Also avoid") },
                placeholder = { Text("e.g. palm oil, carmine") },
                minLines = 2,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                ),
            )

            if (disclaimer.isNotBlank()) {
                Text(
                    disclaimer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileDetailPanel(
    profile: DietProfile,
    restrictions: UserRestrictions,
    expanded: Boolean,
    onExpandClick: () -> Unit,
    onProfileToggle: (String, String, Boolean) -> Unit,
) {
    val details = DietProfileResolver.resolvedDetails(
        profile = profile,
        profileToggles = restrictions.profileToggles,
        allergenLabel = { id ->
            com.ingredientchecker.app.domain.AppConstants.ALLERGEN_OPTIONS
                .find { it.first == id }?.second ?: id
        },
    )

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        profile.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (profile.description.isNotBlank()) {
                        Text(
                            profile.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = onExpandClick) {
                    Icon(
                        if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (profile.includes.isNotEmpty()) {
                        Text(
                            "Includes",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        profile.includes.forEach { line ->
                            BulletItem(line)
                        }
                    }

                    if (details.activeAllergenLabels.isNotEmpty()) {
                        Text(
                            "Allergen checks active",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            details.activeAllergenLabels.forEach { label ->
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Text(
                                        label,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                    }

                    if (details.activeKeywords.isNotEmpty()) {
                        Text(
                            "Keyword checks active",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            details.activeKeywords.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    if (details.effect.vegan || details.effect.vegetarian) {
                        val flags = buildList {
                            if (details.effect.vegan) add("Vegan rules")
                            if (details.effect.vegetarian) add("Vegetarian rules")
                        }
                        Text(
                            flags.joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium,
                            color = AuraEmerald,
                        )
                    }

                    if (profile.toggles.isNotEmpty()) {
                        Text(
                            "Customize",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        profile.toggles.forEach { toggle ->
                            val on = details.toggleStates[toggle.id] ?: toggle.default
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(toggle.label, style = MaterialTheme.typography.bodyMedium)
                                    if (toggle.description.isNotBlank()) {
                                        Text(
                                            toggle.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Switch(
                                    checked = on,
                                    onCheckedChange = { enabled ->
                                        onProfileToggle(profile.id, toggle.id, enabled)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = AuraEmerald,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanSection(
    imageUri: android.net.Uri?,
    scanning: Boolean,
    onTakePhoto: () -> Unit,
    onPickImage: () -> Unit,
    onScan: () -> Unit,
) {
    AuraCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle("Label photo")

            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Label preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            MaterialTheme.shapes.medium,
                        ),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            shape = MaterialTheme.shapes.medium,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        )
                        Text(
                            "Capture or upload a label",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onTakePhoto,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Camera")
                }
                OutlinedButton(
                    onClick = onPickImage,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Gallery")
                }
            }

            Button(
                onClick = onScan,
                enabled = imageUri != null && !scanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuraForest,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                if (scanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Reading label…")
                } else {
                    Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Analyze label", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (scanning) {
                Text(
                    "Processing on your device — usually a few seconds.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ScanResultCard(result: ScanResponse) {
    val (icon, tint, containerColor) = when (result.summaryLevel) {
        "violation", "no_text" -> Triple(Icons.Outlined.ErrorOutline, AuraError, AuraErrorContainer)
        "warning" -> Triple(Icons.Outlined.WarningAmber, AuraWarning, AuraWarningContainer)
        else -> Triple(Icons.Outlined.CheckCircle, AuraForest, AuraSuccessContainer)
    }

    AuraCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(containerColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                }
                Text(
                    result.summary,
                    style = MaterialTheme.typography.titleMedium,
                    color = tint,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (result.mayContainNotices.isNotEmpty()) {
                ResultSubsection(
                    title = "May contain / facility",
                    accent = AuraWarning,
                ) {
                    result.mayContainNotices.forEach { notice ->
                        BulletItem(notice)
                    }
                }
            }

            if (result.violations.isNotEmpty()) {
                ResultSubsection(
                    title = "Violations (${result.violations.size})",
                    accent = AuraError,
                ) {
                    result.violations.forEach { violation ->
                        ViolationItem(violation)
                    }
                }
            }

            if (result.warnings.isNotEmpty()) {
                ResultSubsection(
                    title = "Review manually",
                    accent = AuraWarning,
                ) {
                    result.warnings.forEach { warning ->
                        WarningItem(warning)
                    }
                }
            }

            OcrExpandable(
                rawText = result.rawText,
                normalizedRaw = result.normalizedRaw,
                normalized = result.normalized,
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun ResultSubsection(
    title: String,
    accent: Color,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(4.dp, 16.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(accent),
            )
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun BulletItem(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ViolationItem(violation: Violation) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", color = AuraError)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    violation.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                violation.matchMethod?.let { methodId ->
                    MatchMethodBadge(methodId)
                }
            }
            Text(
                "Rule keyword: “${violation.keyword}”",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            if (!violation.matchedText.isNullOrBlank() &&
                violation.matchedText != violation.keyword &&
                violation.matchMethod != MatchMethod.DIRECT.id
            ) {
                Text(
                    "Detected in label: “${violation.matchedText}”",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            violation.matchDetail?.let { detail ->
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun MatchMethodBadge(methodId: String) {
    val method = MatchMethod.fromId(methodId)
    val (bg, fg) = when (method) {
        MatchMethod.DIRECT -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        MatchMethod.OCR_FIX -> AuraWarningContainer to AuraWarning
        MatchMethod.SYNONYM -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        null -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = MaterialTheme.shapes.small, color = bg) {
        Text(
            method?.label ?: methodId,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
        )
    }
}

@Composable
private fun WarningItem(warning: Warning) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", color = AuraWarning)
        Column {
            Text(warning.term, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                warning.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OcrExpandable(
    rawText: String,
    normalizedRaw: String?,
    normalized: String,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (expanded) "Hide OCR text" else "Show OCR text",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Raw OCR", style = MaterialTheme.typography.labelMedium)
                Text(
                    rawText.ifBlank { "(no text detected)" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!normalizedRaw.isNullOrBlank() && normalizedRaw != normalized) {
                    Text("Before OCR fixes", style = MaterialTheme.typography.labelMedium)
                    Text(
                        normalizedRaw,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("After OCR fixes", style = MaterialTheme.typography.labelMedium)
                    Text(
                        normalized,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
