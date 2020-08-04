/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.description.ConeConditionalEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeContractRenderer
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.types.coneType

object FirImpliesReturnsAnalyzer : FirControlFlowChecker() {

    override fun analyze(graph: ControlFlowGraph, reporter: DiagnosticReporter) {
        val function = (graph.declaration as? FirFunction<*>) ?: return

        if(function.symbol.callableId.callableName.identifier == "test"){
           // graph.traverse(TraverseDirection.Forward, Checker())
        }

        val contractDescription =
            (function as? FirContractDescriptionOwner)?.contractDescription as? FirResolvedContractDescription ?: return

       // println(function.symbol.callableId.callableName.identifier)

//        contractDescription.effects.filterIsInstance<ConeConditionalEffectDeclaration>().forEach { effect ->
//
//            println(
//                "  ${
//                    buildString {
//                        effect.accept(ConeContractRenderer(this), null)
//                    }
//                }"
//            )
//        }
    }

    class Checker : ControlFlowGraphVisitorVoid() {
        override fun visitNode(node: CFGNode<*>) {}

        override fun visitVariableDeclarationNode(node: VariableDeclarationNode) {
            println("val ${node.fir.returnTypeRef.coneType}")
            if(node.fir.initializer != null){
                println("init ${node.fir.initializer?.typeRef?.coneType}")
            }
        }
    }
}