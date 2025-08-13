package org.key_project.checkstyle

import java.io.File

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
object Helper {

    fun getMergeBase(basedir: File) =
        executeCommandReturnOutput(basedir, "git", "merge-base", "HEAD", "origin/main")

    fun executeCommandReturnOutput(basedir: File, vararg command: String): String {
        val pb = ProcessBuilder(*command)
        pb.directory(basedir).redirectOutput()
        val process = pb.start()
        process.waitFor()
        return process.inputReader().use {
            it.readText().trim()
        }
    }

    fun executeCommandReturnLinesFiltered(
        filenamePrefix: File,
        filter: (String) -> Boolean,
        vararg command: String
    ): List<String> {
        val pb = ProcessBuilder(*command)
        pb.directory(filenamePrefix).redirectOutput()
        val process = pb.start()
        val result = process.inputReader().useLines { lines ->
            lines.filter(filter).toList()
        }
        process.waitFor()
        return result
    }
}