package io.gitlab.arturbosch.detekt.rules.style

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.config
import io.gitlab.arturbosch.detekt.api.internal.Configuration
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression

/**
 * This rule detects `if` statements which do not comply with the specified rules.
 * Keeping braces consistent would improve readability and avoid possible errors.
 *
 * The available options are:
 *  * `always`: forces braces on all `if` and `else` branches in the whole codebase.
 *  * `consistent`: ensures that braces are consistent within each `if`-`else if`-`else` chain.
 *    If there's a brace on one of the branches, all branches should have it.
 *  * `necessary`: forces no braces on any `if` and `else` branches in the whole codebase
 *    except where necessary for multi-statement branches.
 *  * `never`: forces no braces on any `if` and `else` branches in the whole codebase.
 *
 * SingleLine if-statement has no line break (\n):
 * ```
 * if (a) b else c
 * ```
 * MultiLine if-statement has at least one line break (\n):
 * ```
 * if (a) b
 * else c
 * ```
 *
 * <noncompliant>
 *
 * // singleLine = 'never'
 * if (a) { b } else { c }
 *
 * if (a) { b } else c
 *
 * if (a) b else { c; d }
 *
 * // multiLine = 'never'
 * if (a) {
 *    b
 * } else {
 *    c
 * }
 *
 * // singleLine = 'always'
 * if (a) b else c
 *
 * if (a) { b } else c
 *
 * // multiLine = 'always'
 * if (a) {
 *    b
 * } else
 *    c
 *
 * // singleLine = 'consistent'
 * if (a) b else { c }
 * if (a) b else if (c) d else { e }
 *
 * // multiLine = 'consistent'
 * if (a)
 *    b
 * else {
 *    c
 * }
 * </noncompliant>
 *
 * <compliant>
 * // singleLine = 'never'
 * if (a) b else c
 *
 * // multiLine = 'never'
 * if (a)
 *    b
 * else
 *    c
 *
 * // singleLine = 'always'
 * if (a) { b } else { c }
 *
 * if (a) { b } else if (c) { d }
 *
 * // multiLine = 'always'
 * if (a) {
 *    b
 * } else {
 *    c
 * }
 *
 * if (a) {
 *    b
 * } else if (c) {
 *    d
 * }
 *
 * // singleLine = 'consistent'
 * if (a) { b } else { c }
 *
 * if (a) b else c
 *
 * if (a) { b } else { c; d }
 *
 * // multiLine = 'consistent'
 * if (a) {
 *    b
 * } else {
 *    c
 * }
 *
 * if (a) b
 * else c
 * </compliant>
 */
class BracesOnIfStatements(config: Config = Config.empty) : Rule(config) {

    override val issue = Issue(
        javaClass.simpleName,
        Severity.Style,
        "Braces do not comply with the specified policy",
        Debt.FIVE_MINS
    )

    @Configuration("single-line braces policy")
    private val singleLine: BracePolicy by config("never") { BracePolicy.getValue(it) }

    @Configuration("multi-line braces policy")
    private val multiLine: BracePolicy by config("always") { BracePolicy.getValue(it) }

    override fun visitIfExpression(expression: KtIfExpression) {
        super.visitIfExpression(expression)

        val parent = expression.parent.parent
        // Ignore `else` branches, they're handled by the initial `if`'s visit.
        // But let us process `then` branches and conditions, because they might be `if` themselves.
        if (parent is KtIfExpression && parent.`else` === expression) return

        val branches: List<KtExpression> = walk(expression)
        validate(branches, policy(expression))
    }

    private fun walk(expression: KtExpression): List<KtExpression> {
        val list = mutableListOf<KtExpression>()
        var current: KtExpression? = expression
        while (current is KtIfExpression) {
            current.then?.let { list.add(it) }
            // Don't add `if` because it's an `else if` which we treat as one unit.
            current.`else`?.takeIf { it !is KtIfExpression }?.let { list.add(it) }
            current = current.`else`
        }
        return list
    }

    private fun validate(list: List<KtExpression>, policy: BracePolicy) {
        when (policy) {
            BracePolicy.Always -> {
                list.filter { !hasBraces(it) }.report(policy)
            }

            BracePolicy.Necessary -> {
                list.filter { !isMultiStatement(it) && hasBraces(it) }.report(policy)
            }

            BracePolicy.Never -> {
                list.filter { hasBraces(it) }.report(policy)
            }

            BracePolicy.Consistent -> {
                val braces = list.count { hasBraces(it) }
                val noBraces = list.count { !hasBraces(it) }
                if (braces != 0 && noBraces != 0) {
                    list.take(1).report(policy)
                }
            }
        }
    }

    private fun List<KtExpression>.report(policy: BracePolicy) {
        this.forEach { report(it, policy) }
    }

    private fun report(violator: KtExpression, policy: BracePolicy) {
        val iff = violator.parent.parent as KtIfExpression
        val reported = when {
            iff.then === violator -> iff.ifKeyword
            iff.`else` === violator -> iff.elseKeyword
            else -> error("Violating element (${violator.text}) is not part of this if (${iff.text})")
        }
        val message = when (policy) {
            BracePolicy.Always -> "Missing braces on this branch, add them."
            BracePolicy.Consistent -> "Inconsistent braces, make sure all branches either have or don't have braces."
            BracePolicy.Necessary -> "Extra braces exist on this branch, remove them (ignore multi-statement)."
            BracePolicy.Never -> "Extra braces exist on this branch, remove them."
        }
        report(CodeSmell(issue, Entity.from(reported ?: violator), message))
    }

    private fun isMultiStatement(expression: KtExpression) =
        expression is KtBlockExpression && expression.statements.size > 1

    private fun policy(expression: KtExpression): BracePolicy =
        if (expression.textContains('\n')) multiLine else singleLine

    private fun hasBraces(expression: KtExpression): Boolean = expression is KtBlockExpression

    enum class BracePolicy(val config: String) {
        Always("always"),
        Consistent("consistent"),
        Necessary("necessary"),
        Never("never");

        companion object {
            fun getValue(arg: String): BracePolicy =
                values().singleOrNull { it.config == arg }
                    ?: error("Unknown value $arg, allowed values are: ${values().joinToString("|")}")
        }
    }
}
