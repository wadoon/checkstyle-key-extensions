package org.key_project.checkstyle

import com.puppycrawl.tools.checkstyle.AbstractAutomaticBean
import com.puppycrawl.tools.checkstyle.api.BeforeExecutionFileFilter
import org.key_project.checkstyle.Helper.executeCommandReturnLinesFiltered
import java.io.File

@Suppress("unused")
class GitDiffFileFilter : AbstractAutomaticBean(), BeforeExecutionFileFilter {
    val diffFile: File? = null
    var debug: Boolean = false
    var basedir: File = File(".")

    private val changedFiles = mutableSetOf<File>()

    override fun accept(uri: String): Boolean {
        val path = basedir.resolve(uri)
        val result = path in changedFiles
        return result
    }

    override fun finishLocalSetup() {
        val mergeBase = Helper.getMergeBase(basedir)
        val lines = diffFile?.useLines { it.filter(::relevantDiffFiles) }?.toList()
            ?: executeCommandReturnLinesFiltered(
                basedir,
                ::relevantDiffFiles,
                "git", "diff", "-U0", "--name-status", mergeBase
            )
        val files = lines.map(::cleanOutputLine).map { basedir.resolve(it) }
        changedFiles.addAll(files)
    }

    fun relevantDiffFiles(x: String) =
        x.startsWith("M") || x.startsWith("A")

    fun cleanOutputLine(x: String) = x.substring(1).trim()
}

