package org.key_project.checkstyle

import com.puppycrawl.tools.checkstyle.api.AbstractCheck
import com.puppycrawl.tools.checkstyle.api.DetailAST
import com.puppycrawl.tools.checkstyle.api.TokenTypes.*
import com.puppycrawl.tools.checkstyle.utils.TokenUtil
import java.util.*

/**
 * This class implements a checkstyle rule which asserts that increment and
 * decrement expressions do only occur as individual commands.
 *
 * This is meant to avoid rather unreadable code like
 *
 * ```
 * for (int k = left; ++left <= right; k = ++left)
 * ```
 *
 * (taken from JDK's DualPivotQuicksort.java).
 *
 * ## Check
 *
 * The check scans all occurrences of pre- and postincrements and checks their
 * parents and grandparents in the AST. First parent is checked (see
 * "admissibleParents" in configuration below). If the parent is EXPR, then
 * the grandparent AST type is also checked.
 *
 * ## Configuration
 *
 * The check can be configured from the style file as follows:
 *
 * ```xml
 * <module name="NoEmbeddedPlusPlus">
 *   <property name="admissibleParents" value="EXPR"/>
 *   <property name="admissibleGrandParents" value="SLIST, ELIST, LITERAL_WHILE, LITERAL_FOR, LITERAL_IF"/>
 *   <message key="parent" value="Unallowed increment/decrement operation."/>
 *   <message key="grandParent" value="Unallowed increment/decrement operation."/>
 * </module>
 * ```
 *
 * This lists also the default values.
 *
 * @author Mattias Ulbrich
 * @version 1
 * @since May 2017
 */
@Suppress("unused")
class NoEmbeddedPlusPlusCheck : AbstractCheck() {
    private val admissibleParents = BitSet()
    private val admissibleGrandParents = BitSet()
    private var parentMessage: String? = DEFAULT_PARENT_MESSAGE
    private var grandParentMessage: String? = DEFAULT_GRAND_PARENT_MESSAGE

    init {
        setBits(this.admissibleParents, ADMISSIBLE_PARENTS)
        setBits(this.admissibleGrandParents, ADMISSIBLE_GRAND_PARENTS)
    }

    private fun setBits(bitset: BitSet, bits: IntArray) {
        for (bit in bits) {
            bitset.set(bit)
        }
    }

    override fun visitToken(ast: DetailAST) {
        val parent: DetailAST? = ast.parent

        if (parent != null) {
            val id: Int = parent.type
            if (!admissibleParents.get(id)) {
                log(
                    ast.lineNo, ast.columnNo,
                    parentMessage
                )
            }

            if (id == EXPR) {
                val gid: Int = parent.parent.type
                if (!admissibleGrandParents.get(gid)) {
                    log(
                        ast.lineNo, ast.columnNo,
                        grandParentMessage
                    )
                }
            }
        }
    }

    fun setAdmissibleParents(vararg parentTokens: String?) {
        admissibleParents.clear()
        for (i in parentTokens.indices) {
            admissibleParents.set(TokenUtil.getTokenId(parentTokens[i]))
        }
    }

    fun setParentMessage(parentMessage: String?) {
        this.parentMessage = parentMessage
    }

    fun setGrandAdmissibleParents(vararg parentTokens: String?) {
        admissibleGrandParents.clear()
        for (i in parentTokens.indices) {
            admissibleGrandParents.set(TokenUtil.getTokenId(parentTokens[i]))
        }
    }

    fun setGrandParentMessage(grandParentMessage: String?) {
        this.grandParentMessage = grandParentMessage
    }

    val _defaultTokens: IntArray = intArrayOf(DEC, INC, POST_DEC, POST_INC)

    override fun getDefaultTokens(): IntArray = _defaultTokens
    override fun getAcceptableTokens(): IntArray = _defaultTokens
    override fun getRequiredTokens(): IntArray = _defaultTokens

    companion object {
        private val ADMISSIBLE_PARENTS = intArrayOf(EXPR)
        private val ADMISSIBLE_GRAND_PARENTS = intArrayOf(SLIST, ELIST, LITERAL_WHILE, LITERAL_FOR, LITERAL_IF)
        private const val DEFAULT_PARENT_MESSAGE = "Unallowed increment/decrement operation."
        private const val DEFAULT_GRAND_PARENT_MESSAGE = "Unallowed increment/decrement operation."
    }
}
