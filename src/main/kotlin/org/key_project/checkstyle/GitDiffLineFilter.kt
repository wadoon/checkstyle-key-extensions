package org.key_project.checkstyle

import com.google.common.collect.Range
import com.google.common.collect.RangeSet
import com.google.common.collect.TreeRangeSet
import com.puppycrawl.tools.checkstyle.AbstractAutomaticBean
import com.puppycrawl.tools.checkstyle.api.AuditEvent
import com.puppycrawl.tools.checkstyle.api.Filter
import org.key_project.checkstyle.Helper.executeCommandReturnLinesFiltered
import java.io.File
import java.util.regex.Pattern

private val FILENAME_PATTERN: Regex = Pattern.compile("\\+\\+\\+ b/(.*)").toRegex()
private val CHANGE_PATTERN: Regex = Pattern.compile("@@ -[^ ]+ \\+(\\d+)(?:,(\\d+))? @@.*").toRegex()
private val RELEVANT_LINE_PATTERN = Pattern.compile("""^(\+\+\+|@@)""").toRegex()
private val EMPTY_SET: RangeSet<Int> = TreeRangeSet.create()

fun relevantDiffLine(line: String): Boolean =
    RELEVANT_LINE_PATTERN.matchesAt(line, 0)


@Suppress("unused")
class GitDiffLineFilter : AbstractAutomaticBean(), Filter {
    private val changedLines: MutableMap<File, RangeSet<Int>> = HashMap()

    val diffFile: File? = null
    var debug: Boolean = false
    var basedir: File = File(".")

    override fun accept(event: AuditEvent): Boolean {
        val filename: String = event.fileName ?: return false
        val file = resolve(filename)
        return hasLineChanged(file, event.line)
    }

    // computeChangedLines() {
    override fun finishLocalSetup() {
        changedLines.clear()
        var lastRange: RangeSet<Int>? = null

        val mergeBase = Helper.getMergeBase(basedir)
        val diffContent = readDiffContent(mergeBase, basedir, diffFile)

        for (line in diffContent) {
            val fileMatch = FILENAME_PATTERN.matchAt(line, 0)
            if (fileMatch != null) {
                val filename = fileMatch.groupValues[1]
                val file = resolve(filename)
                lastRange = changedLines.computeIfAbsent(file) { TreeRangeSet.create() }
                continue
            }

            val change = CHANGE_PATTERN.matchAt(line, 0)
            if (change != null) {
                val from = change.groupValues[1].toInt()
                val len = change.groupValues.getOrNull(2).ifBlankOrNull("1").toInt()
                // store this interval only if it is not a deletion.
                if (len > 0) {
                    lastRange?.add(Range.closed(from, from + len - 1))
                }
            }
        }

        if (debug) {
            changedLines.forEach { (file, range) ->
                println("GitDiffFilterData: $file ==> $range")
            }
        }
    }

    fun resolve(filename: String): File = basedir.resolve(filename)

    fun hasLineChanged(file: File, line: Int): Boolean {
        val intervals = changedLines.getOrDefault(file, EMPTY_SET)
        return intervals.contains(line)
    }

    fun hasLineChanged(file: String, line: Int): Boolean = hasLineChanged(resolve(file), line)

    fun readDiffContent(mergeBase: String, basedir: File, diffFile: File?): List<String> {
        return diffFile?.useLines { it.filter(::relevantDiffLine) }?.toList()
            ?: executeCommandReturnLinesFiltered(
                basedir,
                ::relevantDiffLine,
                "git", "diff", "-U0", mergeBase
            )
    }
}

private fun String?.ifBlankOrNull(default: String): String =
    if (isNullOrBlank()) default else this
