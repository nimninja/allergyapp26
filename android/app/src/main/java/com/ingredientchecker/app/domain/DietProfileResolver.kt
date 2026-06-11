package com.ingredientchecker.app.domain

import com.ingredientchecker.app.data.DietProfile
import com.ingredientchecker.app.data.DietProfileToggle
import com.ingredientchecker.app.data.ProfileEffect
import com.ingredientchecker.app.data.ResolvedProfileDetails
import com.ingredientchecker.app.data.UserRestrictions

object DietProfileResolver {

    fun toggleKey(profileId: String, toggleId: String): String = "$profileId:$toggleId"

    fun parseToggleKey(key: String): Pair<String, String>? {
        val parts = key.split(":", limit = 2)
        if (parts.size != 2) return null
        return parts[0] to parts[1]
    }

    fun toggleState(
        profile: DietProfile,
        profileToggles: Map<String, Boolean>,
        toggle: DietProfileToggle,
    ): Boolean = profileToggles[toggleKey(profile.id, toggle.id)] ?: toggle.default

    fun effectForProfile(
        profile: DietProfile,
        profileToggles: Map<String, Boolean>,
    ): ProfileEffect {
        var allergens = profile.allergens.toMutableSet()
        val extra = profile.extraAvoid.toMutableList()
        var vegan = profile.setsVegan
        var vegetarian = profile.setsVegetarian

        for (toggle in profile.toggles) {
            val on = toggleState(profile, profileToggles, toggle)
            if (on) {
                allergens.addAll(toggle.whenEnabledAllergens)
                extra.addAll(toggle.whenEnabledKeywords)
                allergens.removeAll(toggle.whenEnabledRemoveAllergens)
            }
        }

        return ProfileEffect(
            allergens = allergens,
            extraAvoid = extra.distinct(),
            vegan = vegan,
            vegetarian = vegetarian,
        )
    }

    fun mergeEffects(effects: List<ProfileEffect>): ProfileEffect {
        return ProfileEffect(
            allergens = effects.flatMap { it.allergens }.toSet(),
            extraAvoid = effects.flatMap { it.extraAvoid }.distinct(),
            vegan = effects.any { it.vegan },
            vegetarian = effects.any { it.vegetarian },
        )
    }

    fun subtractEffect(base: ProfileEffect, remove: ProfileEffect): ProfileEffect {
        return ProfileEffect(
            allergens = base.allergens - remove.allergens,
            extraAvoid = base.extraAvoid.filter { it !in remove.extraAvoid.toSet() },
            vegan = if (remove.vegan) false else base.vegan,
            vegetarian = if (remove.vegetarian) false else base.vegetarian,
        )
    }

    fun mergeEffects(a: ProfileEffect, b: ProfileEffect): ProfileEffect =
        mergeEffects(listOf(a, b))

    /** Recompute restrictions from active profiles; preserve manual allergen/extra edits on top. */
    fun applyProfilesToRestrictions(
        profiles: List<DietProfile>,
        activeProfileIds: Set<String>,
        profileToggles: Map<String, Boolean>,
        manualAllergens: Set<String>,
        manualExtraAvoid: List<String>,
    ): UserRestrictions {
        val effects = activeProfileIds.mapNotNull { id ->
            profiles.find { it.id == id }?.let { effectForProfile(it, profileToggles) }
        }
        val merged = mergeEffects(effects)
        val extraMerged = (merged.extraAvoid + manualExtraAvoid).distinct()
        return UserRestrictions(
            activeProfiles = activeProfileIds,
            profileToggles = profileToggles,
            selectedAllergens = merged.allergens + manualAllergens,
            vegan = merged.vegan,
            vegetarian = merged.vegetarian,
            extraAvoid = extraMerged.joinToString(", "),
            manualAllergens = manualAllergens,
            manualExtraAvoid = manualExtraAvoid.joinToString(", "),
        )
    }

    fun resolvedDetails(
        profile: DietProfile,
        profileToggles: Map<String, Boolean>,
        allergenLabel: (String) -> String,
    ): ResolvedProfileDetails {
        val effect = effectForProfile(profile, profileToggles)
        val labels = effect.allergens.map(allergenLabel)
        return ResolvedProfileDetails(
            profile = profile,
            toggleStates = profile.toggles.associate { toggle ->
                toggle.id to toggleState(profile, profileToggles, toggle)
            },
            effect = effect,
            activeKeywords = effect.extraAvoid,
            activeAllergenLabels = labels,
        )
    }

    fun defaultTogglesForProfile(profile: DietProfile): Map<String, Boolean> =
        profile.toggles.associate { toggle ->
            toggleKey(profile.id, toggle.id) to toggle.default
        }
}
