/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.FirModuleBasedSession
import org.jetbrains.kotlin.fir.FirSessionProvider
import org.jetbrains.kotlin.fir.analysis.registerCheckersComponent
import org.jetbrains.kotlin.fir.extensions.BunchOfRegisteredExtensions
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.registerExtensions
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.registerCommonComponents
import org.jetbrains.kotlin.fir.registerResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.jvm.registerJvmCallConflictResolverFactory
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.scopes.wrapScopeWithJvmMapped
import org.jetbrains.kotlin.fir.resolve.transformers.PhasedFirFileResolver
import org.jetbrains.kotlin.fir.scopes.KotlinScopeProvider
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.FirIdeModuleDependencySymbolProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.FirIdeProvider


internal class FirIdeJavaModuleBasedSession private constructor(
    moduleInfo: ModuleInfo,
    sessionProvider: FirSessionProvider,
    val firFileBuilder: FirFileBuilder,
    val cache: ModuleFileCache,
) : FirModuleBasedSession(moduleInfo, sessionProvider) {
    companion object {
        /**
         * Should be invoked only under a [moduleInfo]-based lock
         */
        fun create(
            project: Project,
            cacheProvider: FileFileCacheProvider,
            moduleInfo: ModuleSourceInfo,
            sessionProvider: FirSessionProvider,
        ): FirIdeJavaModuleBasedSession {
            val scopeProvider = KotlinScopeProvider(::wrapScopeWithJvmMapped)
            val firBuilder = FirFileBuilder(sessionProvider as FirIdeSessionProvider, scopeProvider)
            val currentModuleCache = with(cacheProvider) {
                registerCacheForModuleIfNotExists(moduleInfo)
                moduleInfo.dependencies().forEach { dependency ->
                    registerCacheForModuleIfNotExists(dependency)
                }
                getCache(moduleInfo)
            }
            val phasedFirFileResolver = IdePhasedFirFileResolver(firBuilder, currentModuleCache)
            val searchScope = GlobalSearchScope.moduleScope(moduleInfo.module)
            return FirIdeJavaModuleBasedSession(moduleInfo, sessionProvider, firBuilder, currentModuleCache).apply {
                registerCommonComponents()
                registerResolveComponents()
                registerCheckersComponent()

                val firIdeProvider = FirIdeProvider(project, this, firBuilder, moduleInfo, scopeProvider, cache)

                register(FirProvider::class, firIdeProvider)
                register(FirIdeProvider::class, firIdeProvider)
                register(PhasedFirFileResolver::class, phasedFirFileResolver)

                register(
                    FirSymbolProvider::class,
                    FirCompositeSymbolProvider(
                        @OptIn(ExperimentalStdlibApi::class)
                        buildList {
                            add(firIdeProvider)
                            add(JavaSymbolProvider(this@apply, sessionProvider.project, searchScope))
                            moduleInfo.dependencies().mapNotNullTo(this) { dependency ->
                                if (dependency !is ModuleSourceInfo) return@mapNotNullTo null
                                FirIdeModuleDependencySymbolProvider(
                                    project,
                                    dependency,
                                    firFileBuilder,
                                    cacheProvider.getCache(dependency)
                                )
                            }
                            add(FirIdeModuleLibraryDependenciesSymbolProvider(this@apply))
                        }
                    ) as FirSymbolProvider
                )
                registerJvmCallConflictResolverFactory()
                extensionService.registerExtensions(BunchOfRegisteredExtensions.empty())
            }
        }
    }
}