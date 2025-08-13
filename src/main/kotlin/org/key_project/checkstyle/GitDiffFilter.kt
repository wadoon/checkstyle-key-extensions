package org.key_project.checkstyle

import com.google.common.collect.Range
import com.google.common.collect.RangeSet
import com.google.common.collect.TreeRangeSet
import com.puppycrawl.tools.checkstyle.api.*
import org.key_project.checkstyle.GitDiffFilterData.debug
import org.key_project.checkstyle.GitDiffFilterData.logLine
import java.io.File
import java.io.PrintWriter
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern
import kotlin.io.path.readText

open class GitDiffBase : Contextualizable {
    override fun contextualize(context: Context) {
        debug = context.getBooleanProperty("GitDiff.debug")

        logLine(context.attributeNames.toList().toString())

        GitDiffFilterData.filenamePrefix =
            context.getProperty("home.dir")?.let { Paths.get(it) }
                ?: Paths.get(".")

        logLine("GitDiffBase: prefix = ${GitDiffFilterData.filenamePrefix}")

        GitDiffFilterData.init()
    }

    fun Context.getProperty(name: String): String? = get(name)?.toString()
    fun Context.getBooleanProperty(name: String): Boolean = getProperty(name) == "true"
}

@Suppress("unused")
class GitDiffFileFilter : GitDiffBase(), BeforeExecutionFileFilter {
    override fun accept(uri: String): Boolean {
        GitDiffFilterData.init()
        val path = GitDiffFilterData.resolve(uri)

        val result = path in GitDiffFilterData.changedLines

        if (debug) {
            logLine("GitDiffFileFilter: $path ($result)")
        }

        return result
    }
}

@Suppress("unused")
class GitDiffLineFilter : GitDiffBase(), Filter {
    override fun accept(event: AuditEvent): Boolean {
        GitDiffFilterData.init()
        val filename: String = event.fileName ?: return false

        val path = GitDiffFilterData.resolve(filename)
        val result = GitDiffFilterData.hasLineChanged(path, event.line)

        if (debug) {
            logLine("GitDiffLineFilter: $path ($result)")
        }

        return result
    }
}

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
object GitDiffFilterData {
    private val FILENAME_PATTERN: Pattern = Pattern.compile("\\+\\+\\+ b/(.*)")
    private val CHANGE_PATTERN: Pattern = Pattern.compile("@@ -[^ ]+ \\+(\\d+)(?:,(\\d+))? @@.*")
    private val EMPTY_SET: RangeSet<Int> = TreeRangeSet.create()
    val output = PrintWriter(File("/tmp/GitDiff.log").writer(), true)

    var debug: Boolean = false
        set(value) {
            field = value
            logLine("GitDiffBase::debug: $field set")
        }
        get() = true

    fun logLine(x: String) {
        output.write(x)
        output.write("\n")
        output.flush()
    }

    internal var diffFilename: Path? = null
        set(value) {
            field = value
            computeChangedLines()
        }

    internal var filenamePrefix: Path = Paths.get(".")

    internal val mergeBase by lazy {
        executeCommandReturnOutput("git", "merge-base", "HEAD", "origin/main")
    }

    init {
        logLine("GitDiff: initialized")
        logLine("GitDiff: ${Paths.get(".").toAbsolutePath()}")
        logLine("GitDiff: $mergeBase")
    }


    private fun executeCommandReturnOutput(vararg command: String): String {
        val pb = ProcessBuilder(*command)
        pb.directory(filenamePrefix.toFile()).redirectOutput()
        val process = pb.start()
        process.waitFor()
        return process.inputReader().use {
            it.readText().trim()
        }
    }

    private val diffContent by lazy {
        val result = if (diffFilename != null) {
            diffFilename!!.readText()
        } else {
            executeCommandReturnOutput("git", "diff", "-U0", mergeBase)
        }
        if (debug) {
            logLine("diffContent: $result")
        }
        result
    }

    internal val changedLines: MutableMap<Path, RangeSet<Int>> = HashMap()
    internal var initialized = false

    private fun computeChangedLines() {
        changedLines.clear()
        var lastRange: RangeSet<Int>? = null

        for (line in diffContent.lineSequence()) {
            val fileMatch = FILENAME_PATTERN.matcher(line)
            if (fileMatch.matches()) {
                val filename = fileMatch.group(1)
                val path = filenamePrefix.resolve(filename) ?: Paths.get(filename)
                lastRange = changedLines.computeIfAbsent(path) { TreeRangeSet.create() }
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
        initialized = true

        if (debug) {
            changedLines.forEach { (path, range) ->
                logLine("GitDiffFilterData: $path ==> $range")
            }
        }

    }

    fun init() {
        if (!initialized) computeChangedLines()
    }

    fun resolve(filename: String): Path =
        filenamePrefix.resolve(filename) ?: Paths.get(filename)

    fun hasLineChanged(path: Path, line: Int): Boolean {
        val intervals = changedLines.getOrDefault(path, EMPTY_SET)
        return intervals.contains(line)
    }

    fun hasLineChanged(path: String, line: Int): Boolean = hasLineChanged(resolve(path), line)
}