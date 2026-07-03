package zhu.filer

import android.content.Context
import android.content.res.Configuration
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.eclipse.tm4e.core.registry.IThemeSource

object CodeEditorTextMate {

    private const val LIGHT_THEME = "quietlight"
    private const val DARK_THEME = "darcula"

    @Volatile
    private var initialized = false

    private val extToScope = mapOf(
        "java" to "source.java",
        "kt" to "source.kotlin",
        "kts" to "source.kotlin",
        "py" to "source.python",
        "xml" to "text.xml",
        "html" to "text.html.basic",
        "htm" to "text.html.basic",
        "js" to "source.js",
        "md" to "text.html.markdown",
        "json" to "source.json",
        "css" to "source.css",
        "c" to "source.c",
        "h" to "source.c",
        "cpp" to "source.c++",
        "hpp" to "source.c++",
        "cc" to "source.c++",
        "cxx" to "source.c++"
    )

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        try {
            val assets = context.assets
            FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(assets))

            loadTheme(context, LIGHT_THEME, "textmate/quietlight.json", false)
            loadTheme(context, DARK_THEME, "textmate/darcula.json", true)

            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
        } catch (e: Exception) {
        }
        initialized = true
        applyTheme(context)
    }

    private fun loadTheme(context: Context, name: String, assetPath: String, isDark: Boolean) {
        val stream = context.assets.open(assetPath)
        val themeSource = IThemeSource.fromInputStream(stream, "$name.json", null)
        val model = ThemeModel(themeSource, name).apply { this.isDark = isDark }
        ThemeRegistry.getInstance().loadTheme(model)
    }

    fun applyTheme(context: Context) {
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        ThemeRegistry.getInstance().setTheme(if (isDark) DARK_THEME else LIGHT_THEME)
    }

    fun languageForExtension(ext: String): Language {
        val scope = extToScope[ext.lowercase()]
        if (scope != null) {
            return try {
                TextMateLanguage.create(scope, false)
            } catch (e: Exception) {
                EmptyLanguage()
            }
        }
        return EmptyLanguage()
    }

    fun createColorScheme(): EditorColorScheme {
        return TextMateColorScheme.create(ThemeRegistry.getInstance())
    }
}
