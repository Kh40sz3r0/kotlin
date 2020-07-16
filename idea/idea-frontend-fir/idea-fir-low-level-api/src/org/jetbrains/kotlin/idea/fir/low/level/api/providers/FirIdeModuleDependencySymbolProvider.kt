/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.providers

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.IndexHelper
import org.jetbrains.kotlin.idea.fir.low.level.api.PackageExistenceCheckerForSingleModule
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class FirIdeModuleDependencySymbolProvider(
    project: Project,
    moduleInfo: ModuleSourceInfo,
    firFileBuilder: FirFileBuilder,
    cache: ModuleFileCache,
) : FirSymbolProvider() {
    private val indexHelper = IndexHelper(project, GlobalSearchScope.moduleScope(moduleInfo.module))
    private val packageExistenceChecker = PackageExistenceCheckerForSingleModule(project, moduleInfo)

    private val providerHelper = FirProviderHelper(
        cache,
        firFileBuilder,
        indexHelper,
        packageExistenceChecker,
    )

    override fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>? =
        providerHelper.getFirClassifierByFqName(classId)?.symbol

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> =
        providerHelper.getTopLevelCallableSymbols(packageFqName, name)

    override fun getPackage(fqName: FqName): FqName? =
        providerHelper.getPackage(fqName)

    override fun getNestedClassifierScope(classId: ClassId): FirScope? =
        providerHelper.getNestedClassifierScope(classId)
}