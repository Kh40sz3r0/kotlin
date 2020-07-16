/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.builder

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtFile
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.annotation.concurrent.ThreadSafe
import kotlin.concurrent.withLock

/**
 * Caches mapping [KtFile] -> [FirFile] of module [moduleInfo]
 */
@ThreadSafe
internal abstract class ModuleFileCache {
    abstract val moduleInfo: ModuleInfo

    /**
     * Maps [ClassId] to corresponding classifiers
     * If classifier with required [ClassId] is not found in given module then map contains [Optional.EMPTY]
     */
    abstract val classifierByClassId: ConcurrentHashMap<ClassId, Optional<FirClassLikeDeclaration<*>>>

    /**
     * Maps [CallableId] to corresponding callable
     * If callable with required [CallableId]] is not found in given module then map contains emptyList
     */
    abstract val callableByCallableId: ConcurrentHashMap<CallableId, List<FirCallableSymbol<*>>>

    /**
     * @return null is no cache present (that means an error), containing [FirFile] otherwise
     * [recordParentingInfo] on containing [FirFile] should be invoked before running [callableToContainer]
     */
    abstract fun classifierToContainerContainer(classId: ClassId): FirFile?

    /**
     * @return null is no cache present (that means an error), containing [FirFile] otherwise
     * [recordParentingInfo] on containing [FirFile] should be invoked before running [callableToContainer]
     */
    abstract fun callableToContainer(callableSymbol: FirCallableSymbol<*>): FirFile?

    /**
     * @return [FirFile] by [file] if it was previously built or runs [createValue] otherwise
     * The [createValue] is run under the lock so [createValue] is executed at most once for each [KtFile]
     */
    abstract fun fileCached(file: KtFile, createValue: () -> FirFile): FirFile

    /**
     * Saves [file] as containing file on each declaration in [file]
     * That information will be later used in [classifierToContainerContainer] & [callableToContainer]
     */
    abstract fun recordParentingInfo(file: FirFile)

    // todo make it ReadWriteLock and allow access fir elements only under read lock
    // for now locks only held for resolve
    // but there can be a situation when we are accessing some fir element in one thread without lock
    // in the same time other thread performs resolve of it
    // which can cause weird errors on user side
    abstract val firFileLockProvider: LockProvider<FirFile, ReentrantLock>
}

internal class ModuleFileCacheImpl(override val moduleInfo: ModuleInfo) : ModuleFileCache() {
    private val ktFileToFirFile = ConcurrentHashMap<KtFile, FirFile>()

    private val stateAccessLock = ReentrantReadWriteLock()

    private val state = FirProviderImpl.StateImpl()

    override fun classifierToContainerContainer(classId: ClassId): FirFile? = stateAccessLock.readLock().withLock {
        state.classifierContainerFileMap[classId]
    }

    override fun callableToContainer(callableSymbol: FirCallableSymbol<*>): FirFile? = stateAccessLock.readLock().withLock {
        state.callableContainerMap[callableSymbol]
    }

    override val classifierByClassId: ConcurrentHashMap<ClassId, Optional<FirClassLikeDeclaration<*>>> = ConcurrentHashMap()
    override val callableByCallableId: ConcurrentHashMap<CallableId, List<FirCallableSymbol<*>>> = ConcurrentHashMap()

    override fun fileCached(file: KtFile, createValue: () -> FirFile): FirFile =
        ktFileToFirFile.computeIfAbsent(file) { createValue() }

    override fun recordParentingInfo(file: FirFile) {
        stateAccessLock.writeLock().withLock {
            file.acceptChildren(FirProviderImpl.FirRecorder, state to file)
        }
    }

    override val firFileLockProvider: LockProvider<FirFile, ReentrantLock> = LockProvider { ReentrantLock() }
}
