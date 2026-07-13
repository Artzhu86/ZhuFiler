package zhu.filer

import android.content.Context
import android.content.res.Configuration
import com.google.android.material.R as materialR
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

// 代码编辑器TextMate配置
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
        "cxx" to "source.c++",
        "lua" to "source.lua"
    )

    // 初始化TextMate
    @Synchronized
    fun init(context: Context) {
        if (!initialized) {
            try {
                FileProviderRegistry.getInstance().addFileProvider(
                    AssetsFileResolver(context.applicationContext.assets)
                )

                val themes = arrayOf("darcula", "ayu-dark", "quietlight", "solarized_dark")
                val themeRegistry = ThemeRegistry.getInstance()
                for (name in themes) {
                    val path = "textmate/$name.json"
                    val themeSource = IThemeSource.fromInputStream(
                        FileProviderRegistry.getInstance().tryGetInputStream(path),
                        path,
                        null
                    )
                    themeRegistry.loadTheme(
                        ThemeModel(themeSource, name).apply {
                            if (name != "quietlight") {
                                isDark = true
                            }
                        }
                    )
                }

                GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
            } catch (e: Exception) {
                android.util.Log.e("CodeEditorTextMate", "init failed", e)
            }
            initialized = true
        }
    }

    // 应用主题
    fun applyTheme(context: Context) {
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        ThemeRegistry.getInstance().setTheme(if (isDark) DARK_THEME else LIGHT_THEME)
    }

    // 根据扩展名获取语言
    fun languageForExtension(ext: String): Language {
        val scope = extToScope[ext.lowercase()]
        if (scope != null) {
            return try {
                TextMateLanguage.create(scope, true)
            } catch (e: Exception) {
                android.util.Log.e("CodeEditorTextMate", "languageForExtension failed: $scope", e)
                EmptyLanguage()
            }
        }
        return EmptyLanguage()
    }

    // 创建颜色方案
    fun createColorScheme(context: Context): EditorColorScheme {
        return TextMateColorScheme.create(ThemeRegistry.getInstance())
    }

    // 应用编辑器颜色
    fun applyEditorColors(editor: io.github.rosemoe.sora.widget.CodeEditor, context: Context) {
        val scheme = editor.colorScheme
        scheme.setColor(
            EditorColorScheme.LINE_NUMBER_BACKGROUND,
            getThemeColor(context, materialR.attr.colorSurfaceVariant)
        )
        scheme.setColor(
            EditorColorScheme.LINE_DIVIDER,
            getThemeColor(context, materialR.attr.colorOutlineVariant)
        )
        scheme.setColor(
            EditorColorScheme.LINE_NUMBER,
            getThemeColor(context, materialR.attr.colorOnSurfaceVariant)
        )
        scheme.setColor(
            EditorColorScheme.LINE_NUMBER_CURRENT,
            getThemeColor(context, android.R.attr.colorPrimary)
        )
        scheme.setColor(
            EditorColorScheme.WHOLE_BACKGROUND,
            getThemeColor(context, android.R.attr.colorBackground)
        )
        scheme.setColor(
            EditorColorScheme.CURRENT_LINE,
            getThemeColor(context, materialR.attr.colorPrimaryContainer)
        )
        scheme.setColor(
            EditorColorScheme.SELECTION_INSERT,
            getThemeColor(context, android.R.attr.colorPrimary)
        )
        scheme.setColor(
            EditorColorScheme.MATCHED_TEXT_BACKGROUND,
            getThemeColor(context, materialR.attr.colorPrimaryContainer)
        )
        val guideBase = getThemeColor(context, materialR.attr.colorOnSurfaceVariant) and 0x00FFFFFF
        scheme.setColor(EditorColorScheme.BLOCK_LINE, guideBase or 0x33000000)
        scheme.setColor(EditorColorScheme.BLOCK_LINE_CURRENT, guideBase or 0x99000000.toInt())
    }
}
