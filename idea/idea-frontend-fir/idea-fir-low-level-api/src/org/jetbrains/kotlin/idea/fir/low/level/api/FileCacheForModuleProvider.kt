/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCacheImpl
import java.util.concurrent.ConcurrentHashMap

internal class FileCacheForModuleProvider {
    private val map = ConcurrentHashMap<ModuleInfo, ModuleFileCache>()

    fun registerCacheForModuleIfNotExists(moduleInfo: IdeaModuleInfo) {
        map.getOrPut(moduleInfo) { ModuleFileCacheImpl(moduleInfo) }
    }

    fun getCache(module: ModuleInfo): ModuleFileCache {
        return map[module]
            ?: error("Cache for module $module is not found")
    }
}