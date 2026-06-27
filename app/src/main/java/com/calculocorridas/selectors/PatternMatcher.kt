package com.calculocorridas.selectors

import android.view.accessibility.AccessibilityNodeInfo
import com.calculocorridas.domain.entities.SelectorPattern
import com.calculocorridas.domain.entities.SelectorType
import com.calculocorridas.utils.findAllTexts
import com.calculocorridas.utils.findNodeById
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatternMatcher @Inject constructor() {

    fun match(root: AccessibilityNodeInfo, patterns: List<SelectorPattern>): String? {
        val sorted = patterns.sortedByDescending { it.priority }
        for (pattern in sorted) {
            val result = tryMatch(root, pattern)
            if (!result.isNullOrBlank()) return result
        }
        return null
    }

    // Versão que busca em lista de strings puras (ex: textos de notificação)
    fun matchFromTexts(texts: List<String>, patterns: List<SelectorPattern>): String? {
        val sorted = patterns.sortedByDescending { it.priority }
        for (pattern in sorted) {
            if (pattern.type != SelectorType.REGEX) continue
            val compiled = runCatching { Pattern.compile(pattern.value) }.getOrNull() ?: continue
            for (text in texts) {
                val m = compiled.matcher(text)
                if (m.find()) return if (m.groupCount() > 0) m.group(1) else m.group(0)
            }
        }
        return null
    }

    private fun tryMatch(root: AccessibilityNodeInfo, pattern: SelectorPattern): String? =
        when (pattern.type) {
            SelectorType.ACCESSIBILITY_ID -> matchByViewId(root, pattern.value)
            SelectorType.REGEX            -> matchByRegex(root, pattern.value)
            SelectorType.CONTENT_DESC     -> matchByContentDesc(root, pattern.value)
            SelectorType.CLASS_NAME       -> matchByClassName(root, pattern.value)
        }

    private fun matchByViewId(root: AccessibilityNodeInfo, viewId: String): String? =
        root.findNodeById(viewId)?.let { node ->
            node.text?.toString() ?: node.contentDescription?.toString()
        }

    private fun matchByRegex(root: AccessibilityNodeInfo, regex: String): String? {
        val pattern = runCatching { Pattern.compile(regex) }.getOrNull() ?: return null
        val allTexts = root.findAllTexts()
        for (text in allTexts) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return if (matcher.groupCount() > 0) matcher.group(1) else matcher.group(0)
            }
        }
        return null
    }

    private fun matchByContentDesc(root: AccessibilityNodeInfo, desc: String): String? {
        val nodes = root.findAccessibilityNodeInfosByText(desc)
        return nodes?.firstOrNull()?.let {
            it.text?.toString() ?: it.contentDescription?.toString()
        }
    }

    private fun matchByClassName(root: AccessibilityNodeInfo, className: String): String? {
        if (root.className?.toString() == className) return root.text?.toString()
        for (i in 0 until root.childCount) {
            val result = matchByClassName(root.getChild(i) ?: continue, className)
            if (!result.isNullOrBlank()) return result
        }
        return null
    }
}
