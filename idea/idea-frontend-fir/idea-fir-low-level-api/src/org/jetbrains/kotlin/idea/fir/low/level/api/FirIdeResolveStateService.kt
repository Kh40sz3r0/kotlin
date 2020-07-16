/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.fir.low.level.api.util.collectTransitiveDependenciesWithoutSelf
import org.jetbrains.kotlin.idea.util.psiModificationTrackerBasedCachedValue
import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.kotlin.fir.java.FirLibrarySession
import org.jetbrains.kotlin.idea.caches.project.*
import org.jetbrains.kotlin.idea.caches.resolve.IDEPackagePartProvider

internal class FirIdeResolveStateService(private val project: Project) {
    private val stateCache by psiModificationTrackerBasedCachedValue(project) {
        ConcurrentHashMap<IdeaModuleInfo, FirModuleResolveStateImpl>()
    }

    private fun createResolveStateFor(moduleInfo: IdeaModuleInfo): FirModuleResolveStateImpl {
        val cacheProvider = FileCacheForModuleProvider()
        val sessionProvider = FirIdeSessionProvider(project)
        val session = FirIdeJavaModuleBasedSession.create(
            project,
            cacheProvider,
            moduleInfo as ModuleSourceInfo,
            sessionProvider,
        ).apply { sessionProvider.registerSession(moduleInfo, this) }

        createSessionForModuleDependencies(moduleInfo, cacheProvider, sessionProvider)

        return FirModuleResolveStateImpl(moduleInfo, session, sessionProvider, session.firFileBuilder, session.cache)
    }

    private fun createSessionForModuleDependencies(
        moduleInfo: IdeaModuleInfo,
        cacheProvider: FileCacheForModuleProvider,
        sessionProvider: FirIdeSessionProvider
    ) {
        val dependenciesWithoutSelf = moduleInfo.collectTransitiveDependenciesWithoutSelf()
        dependenciesWithoutSelf.forEach { dependency ->
            val dependencySession = createSessionFor(dependency, cacheProvider, sessionProvider)
            sessionProvider.registerSession(dependency, dependencySession)
        }
    }

    private fun createSessionFor(
        moduleInfo: IdeaModuleInfo,
        cacheProvider: FileCacheForModuleProvider,
        sessionProvider: FirIdeSessionProvider
    ) = when {
        moduleInfo is ModuleSourceInfo -> FirIdeJavaModuleBasedSession.create(project, cacheProvider, moduleInfo, sessionProvider)
        moduleInfo.isLibraryClasses() -> FirLibrarySession.create(
            moduleInfo,
            sessionProvider,
            moduleInfo.contentScope(),
            project,
            IDEPackagePartProvider(moduleInfo.contentScope())
        )
        else -> error("Cannot create a session for $moduleInfo")
    }

    fun getResolveState(moduleInfo: IdeaModuleInfo): FirModuleResolveStateImpl =
        stateCache.getOrPut(moduleInfo) { createResolveStateFor(moduleInfo) }

    companion object {
        fun getInstance(project: Project): FirIdeResolveStateService = project.service()
    }
}