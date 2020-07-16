/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteActionInEdt
import org.jetbrains.kotlin.psi.KtElement

class KtApplicableComputation<T>(
    private val context: KtElement,
    private val analyse: KtAnalysisSession.() -> T,
    private val apply: (T) -> Unit,
    private val exceptionHandler: ((Throwable) -> Unit)?,
    private val onCodeChangedBehaviour: KtApplicableComputationBehaviour
) {
    private val modificationTracker = KotlinModificationTrackerService.getInstance(context.project).modificationTracker

    fun run() {
        var applied = false
        while (!applied) {
            val (analyseResult, timestampOnAnalysed) = runReadAction {
                val analysisSession = getAnalysisSessionFor(context)
                val result = try {
                    analysisSession.analyse()
                } catch (e: Throwable) {
                    exceptionHandler?.invoke(e) ?: throw e
                    null
                }
                val timestamp = modificationTracker.modificationCount
                result to timestamp
            }
            if (analyseResult == null) {
                return
            }
            runWriteActionInEdt {
                val worldChanged = modificationTracker.modificationCount != timestampOnAnalysed
                if (worldChanged) {
                    applied = when (onCodeChangedBehaviour) {
                        KtApplicableComputationBehaviour.RERUN_ANALYSIS_WHEN_CODE_CHANGED -> {
                            false
                        }
                        KtApplicableComputationBehaviour.DO_NOT_APPLY_ON_CODE_CHANGE -> {
                            true
                        }
                        KtApplicableComputationBehaviour.APPLY_ON_CHANGED_CODE -> {
                            apply(analyseResult)
                            true
                        }
                    }
                } else {
                    apply(analyseResult)
                }
            }
        }
    }
}

class KtApplicableComputationBuilder<T>
internal constructor(private val analyse: KtAnalysisSession.() -> T) {
    internal var context: KtElement? = null
    internal var apply: ((T) -> Unit)? = null
    internal var exceptionHandler: ((Throwable) -> Unit)? = null
    internal var onCodeChangedBehaviour: KtApplicableComputationBehaviour? = null
    internal fun build() = KtApplicableComputation(context!!, analyse, apply!!, exceptionHandler, onCodeChangedBehaviour!!)
}

fun <T> preAnalyze(
    onCodeChangedBehaviour: KtApplicableComputationBehaviour = KtApplicableComputationBehaviour.DO_NOT_APPLY_ON_CODE_CHANGE,
    analyse: KtAnalysisSession.() -> T
): KtApplicableComputationBuilder<T> =
    KtApplicableComputationBuilder(analyse).apply { this.onCodeChangedBehaviour = onCodeChangedBehaviour }

infix fun <T> KtApplicableComputationBuilder<T>.andThenApply(
    apply: ((T) -> Unit)
): KtApplicableComputationBuilder<T> {
    check(this.apply == null) { "Apply action is already set" }
    this.apply = apply
    return this
}

infix fun <T> KtApplicableComputationBuilder<T>.withExceptionHandler(exceptionHandler: (Throwable) -> Unit): KtApplicableComputationBuilder<T> {
    check(this.exceptionHandler == null) { "Exception handler is already set" }
    this.exceptionHandler = exceptionHandler
    return this
}


infix fun <T> KtApplicableComputationBuilder<T>.executeInContextOf(context: KtElement) {
    build().run()
}


enum class KtApplicableComputationBehaviour {
    RERUN_ANALYSIS_WHEN_CODE_CHANGED,
    DO_NOT_APPLY_ON_CODE_CHANGE,
    APPLY_ON_CHANGED_CODE
}
