package com.example.logbook.localization

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.content.res.AssetManager
import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** Presentation only: persisted names, units and exchange values stay canonical. */
object I18n {
    var language by mutableStateOf("ru")
        private set
    private var application: Context? = null
    private var localized: Context? = null
    private var localizedLanguage = ""

    @Synchronized fun initialize(context: Context) {
        if (application != null) return
        application = context.applicationContext
        language = application!!.getSharedPreferences("app_locale", Context.MODE_PRIVATE)
            .getString("language", "ru") ?: "ru"
    }

    @Synchronized fun changeLanguage(value: String) {
        val next = if (value.equals("en", true) || value.equals("English", true)) "en" else "ru"
        if (language == next) return
        language = next
        localized = null
        application?.getSharedPreferences("app_locale", Context.MODE_PRIVATE)?.edit()
            ?.putString("language", next)?.apply()
    }

    @Synchronized fun tr(@StringRes id: Int, vararg args: Any?): String {
        val current = language
        val app = application
        if (app == null) {
            val template = (if (current == "en") englishTexts[id] else defaultTexts[id]) ?: ""
            return format(template, current, args)
        }
        if (localized == null || localizedLanguage != current) {
            localized = app.createConfigurationContext(Configuration(app.resources.configuration).apply {
                setLocales(LocaleList.forLanguageTags(current))
            })
            localizedLanguage = current
        }
        return format(localized!!.resources.getString(id), current, args)
    }

    private fun format(template: String, locale: String, args: Array<out Any?>): String =
        String.format(Locale.forLanguageTag(locale), template.replace("%%", "%")
            .replace(Regex("%(?![0-9]+\\\$s)"), "%%"), *args)

    /** Use only for application-owned captions and retained status messages, never user text. */
    private val captions by lazy { (defaultTexts.entries + englishTexts.entries).associate { it.value to it.key } }
    private data class Template(val id: Int, val pattern: Regex, val arguments: List<Int>)
    private val templates by lazy {
        (defaultTexts.entries + englishTexts.entries).filter { it.value.contains(Regex("%[0-9]+\\\$s")) }.map { (id, text) ->
            val matches = Regex("%[0-9]+\\\$s").findAll(text).toList()
            var start = 0
            val pattern = buildString {
                append("^")
                matches.forEach { m -> append(Regex.escape(text.substring(start, m.range.first)));append("(.*?)");start=m.range.last+1 }
                append(Regex.escape(text.substring(start)));append("$")
            }
            Template(id, Regex(pattern, RegexOption.DOT_MATCHES_ALL), matches.map { it.value.substring(1).substringBefore('$').toInt()-1 })
        }
    }
    fun display(value: String): String {
        captions[value]?.let { return tr(it) }
        templates.forEach { template -> template.pattern.matchEntire(value)?.let {
            val args=Array(template.arguments.max()+1) { "" }
            template.arguments.forEachIndexed { group, argument -> args[argument]=it.groupValues[group+1] }
            return tr(template.id, *args)
        } }
        return value
    }

    fun duration(value: String): String {
        if (language != "en") return value
        return value.replace("г.", "y ").replace("мес.", "mo").replace("дн.", "days")
    }
}

@Composable
fun AppLocaleHost(selected: String?, content: @Composable () -> Unit) {
    SideEffect { if (selected != null) I18n.changeLanguage(selected) }
    val language = I18n.language
    val base = LocalContext.current
    val configuration = Configuration(LocalConfiguration.current).apply {
        setLocales(LocaleList.forLanguageTags(language))
    }
    val context = remember(base, language, configuration) {
        LocalizedActivityContext(base, base.createConfigurationContext(configuration))
    }
    CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides configuration, content = content)
}

/** Keep the Activity in the wrapper chain for result launchers and other Android owners. */
private class LocalizedActivityContext(base: Context, private val localized: Context) : ContextWrapper(base) {
    override fun getResources(): Resources = localized.resources
    override fun getAssets(): AssetManager = localized.assets
}
