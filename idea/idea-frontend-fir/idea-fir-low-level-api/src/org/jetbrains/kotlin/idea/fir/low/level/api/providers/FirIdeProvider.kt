/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.providers

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProviderInternals
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.KotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.IndexHelper
import org.jetbrains.kotlin.idea.fir.low.level.api.PackageExistenceCheckerForSingleModule
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionByPackageIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionFqnNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelPropertyByPackageIndex
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtNamedFunction

internal class FirIdeProvider(
    val project: Project,
    val session: FirSession,
    firFileBuilder: FirFileBuilder,
    moduleInfo: ModuleSourceInfo,
    private val kotlinScopeProvider: KotlinScopeProvider,
    private val cache: ModuleFileCache,
) : FirProvider() {
    private val packageExistenceChecker = PackageExistenceCheckerForSingleModule(project, moduleInfo)

    private val indexHelper = IndexHelper(project, GlobalSearchScope.moduleScope(moduleInfo.module))

    private val providerHelper = FirProviderHelper(
        cache,
        firFileBuilder,
        indexHelper,
        packageExistenceChecker,
    )

    override val isPhasedFirAllowed: Boolean get() = true

    override fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration<*>? =
        providerHelper.getFirClassifierByFqName(classId)

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> =
        providerHelper.getTopLevelCallableSymbols(packageFqName, name)

    override fun getPackage(fqName: FqName): FqName? =
        providerHelper.getPackage(fqName)

    override fun getNestedClassifierScope(classId: ClassId): FirScope? =
        providerHelper.getNestedClassifierScope(classId)

    override fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>? {
        return getFirClassifierByFqName(classId)?.symbol
    }

    override fun getFirClassifierContainerFile(fqName: ClassId): FirFile {
        return getFirClassifierContainerFileIfAny(fqName)
            ?: error("Couldn't find container for ${fqName}")
    }

    override fun getFirClassifierContainerFileIfAny(fqName: ClassId): FirFile? {
        getFirClassifierByFqName(fqName) // Necessary to ensure cacheProvider contains this classifier
        return cache.classifierToContainerContainer(fqName)
    }

    override fun getFirClassifierContainerFile(symbol: FirClassLikeSymbol<*>): FirFile {
        return getFirClassifierContainerFileIfAny(symbol)
            ?: error("Couldn't find container for ${symbol.classId}")
    }

    override fun getFirClassifierContainerFileIfAny(symbol: FirClassLikeSymbol<*>): FirFile? =
        getFirClassifierContainerFileIfAny(symbol.classId)


    override fun getFirCallableContainerFile(symbol: FirCallableSymbol<*>): FirFile? {
        symbol.overriddenSymbol?.let {
            return getFirCallableContainerFile(it)
        }
        if (symbol is FirAccessorSymbol) {
            val fir = symbol.fir
            if (fir is FirSyntheticProperty) {
                return getFirCallableContainerFile(fir.getter.delegate.symbol)
            }
        }
        return cache.callableToContainer(symbol)
    }

    override fun getFirFilesByPackage(fqName: FqName): List<FirFile> = error("Should not be called in FIR IDE")


    // TODO move out of here
    // used only for completion
    fun buildFunctionWithBody(ktNamedFunction: KtNamedFunction): FirFunction<*> {
        return RawFirBuilder(session, kotlinScopeProvider, stubMode = false).buildFunctionWithBody(ktNamedFunction)
    }


    @FirProviderInternals
    override fun recordGeneratedClass(owner: FirAnnotatedDeclaration, klass: FirRegularClass) {
        TODO()
    }

    @FirProviderInternals
    override fun recordGeneratedMember(owner: FirAnnotatedDeclaration, klass: FirDeclaration) {
        TODO()
    }

    // TODO this should be reworked because [FirIdeProvider] should not have such method
    // used only in completion
    override fun getAllCallableNamesInPackage(fqName: FqName): Set<Name> {
        return hashSetOf<Name>().apply {
            indexHelper.getTopLevelPropertiesInPackage(fqName).mapNotNullTo(this) { it.nameAsName }
            indexHelper.getTopLevelFunctionsInPackage(fqName).mapNotNullTo(this) { it.nameAsName }
        }
    }
}

internal val FirSession.firIdeProvider: FirIdeProvider by FirSession.sessionComponentAccessor()