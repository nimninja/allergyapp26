package com.ingredientchecker.app.data

import android.content.Context
import org.yaml.snakeyaml.Yaml
import java.io.InputStreamReader

/** Loads YAML rule files bundled in assets/rules/. */
class RulesRepository(context: Context) {
    private val appContext = context.applicationContext
    private val yaml = Yaml()

    private var allergens: Map<String, Map<String, Any>>? = null
    private var vegan: Map<String, Any>? = null
    private var vegetarian: Map<String, Any>? = null
    private var ambiguous: Map<String, Any>? = null

    @Synchronized
    fun allergensData(): Map<String, Map<String, Any>> {
        allergens?.let { return it }
        @Suppress("UNCHECKED_CAST")
        val loaded = loadYaml("rules/allergens.yaml") as? Map<String, Map<String, Any>> ?: emptyMap()
        allergens = loaded
        return loaded
    }

    @Synchronized
    fun veganData(): Map<String, Any> {
        vegan?.let { return it }
        val loaded = loadYaml("rules/vegan.yaml")
        vegan = loaded
        return loaded
    }

    @Synchronized
    fun vegetarianData(): Map<String, Any> {
        vegetarian?.let { return it }
        val loaded = loadYaml("rules/vegetarian.yaml")
        vegetarian = loaded
        return loaded
    }

    @Synchronized
    fun ambiguousData(): Map<String, Any> {
        ambiguous?.let { return it }
        val loaded = loadYaml("rules/ambiguous.yaml")
        ambiguous = loaded
        return loaded
    }

    private fun loadYaml(assetPath: String): Map<String, Any> {
        appContext.assets.open(assetPath).use { stream ->
            @Suppress("UNCHECKED_CAST")
            return (yaml.load(InputStreamReader(stream)) as? Map<String, Any>) ?: emptyMap()
        }
    }
}
