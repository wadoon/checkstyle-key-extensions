package org.key_project.checkstyle

import com.google.common.collect.Range
import com.google.common.collect.RangeSet
import com.google.common.collect.TreeRangeSet
import com.puppycrawl.tools.checkstyle.api.AuditEvent
import com.puppycrawl.tools.checkstyle.api.BeforeExecutionFileFilter
import com.puppycrawl.tools.checkstyle.api.Filter
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern
import kotlin.io.path.readText

/**
 * This class implements a checkstyle filter which filters all messages
 * which correspond to lines which have been recently changed according
 * to a git-diff file provided to the filter.
 *
 * ## Diff file
 * The git-diff file must be provided and is not produced by the filter.
 * You may create it using
 *
 * ```sh
 * git diff -U0 $MERGE_BASE > diffFile
 * ```
 *
 * For `MERGE_BASE` the assignment
 * ```
 * MERGE_BASE=`git merge-base HEAD origin/main`
 * ```
 * proved sensible if merging against the main branch.
 * The `diffFile` can then be provided to the filter as
 * ```xml
 * <module name="GitDiffFilter">
 *   <property name="diffFilename" value="diffFile" />
 * </module>
 * ```
 *
 * @author Mattias Ulbrich
 * @version 1
 * @since Mar 2017
 */
@Suppress("unused")
class GitDiffFilter : Filter, BeforeExecutionFileFilter {
    companion object {
        private val FILENAME_PATTERN: Pattern = Pattern.compile("\\+\\+\\+ b/(.*)")
        private val CHANGE_PATTERN: Pattern = Pattern.compile("@@ -[^ ]+ \\+(\\d+)(?:,(\\d+))? @@.*")
        private val EMPTY_SET: RangeSet<Int> = TreeRangeSet.create()
    }

    private var diffFilename: Path? = null
        set(value) {
            field = value
            computeChangedLines()
        }

    private var filenamePrefix: Path? = null
        set(value) {
            field = value
            computeChangedLines()
        }

    private val mergeBase by lazy {
        val pb = ProcessBuilder("git", "merge-base", "HEAD", "origin/main")
        pb.redirectOutput()
        val process = pb.start()
        process.waitFor()
        process.inputReader().use {
            it.readText()
        }
    }

    private val diffContent by lazy {
        if (diffFilename != null) {
            diffFilename!!.readText()
        } else {
            val pb = ProcessBuilder("git", "diff", "-U0", mergeBase)
            pb.redirectOutput()
            val process = pb.start()
            process.waitFor()
            process.inputReader().use {
                it.readText()
            }
        }
    }

    private var changedLines: MutableMap<String, RangeSet<Int>> = HashMap()

    override fun accept(uri: String): Boolean {
        return uri in changedLines
    }

    override fun accept(event: AuditEvent): Boolean {
        val filename = event.fileName
        if (filename == null) {
            return false
        }

        val intervals = changedLines.getOrDefault(filename, EMPTY_SET)

        //require(find(intervals, event.line) == findSimple(intervals, event.line))

        return intervals.contains(event.line)
    }

    private fun computeChangedLines() {
        changedLines.clear()
        var lastRange: RangeSet<Int>? = null

        for (line in diffContent.lineSequence()) {
            val fileMatch = FILENAME_PATTERN.matcher(line)
            if (fileMatch.matches()) {
                val filename = fileMatch.group(1)
                val path = filenamePrefix?.resolve(filename) ?: Paths.get(filename)
                val uri = path.toUri().toString()
                lastRange = changedLines.put(uri, TreeRangeSet.create())
                continue
            }

            val change = CHANGE_PATTERN.matcher(line)
            if (change.matches()) {
                val from = change.group(1).toInt()
                val toString = change.group(2)
                val len = toString?.toInt() ?: 1

                // store this interval only if it is not a deletion.
                if (len > 0) {
                    lastRange?.add(Range.closed(from, from + len - 1))
                }
            }
        }
    }
}