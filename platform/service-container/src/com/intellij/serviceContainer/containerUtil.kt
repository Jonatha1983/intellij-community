// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
@file:ApiStatus.Internal

package com.intellij.serviceContainer

import com.intellij.openapi.progress.Cancellation
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.annotations.ApiStatus

internal fun checkCanceledIfNotInClassInit() {
  try {
    ProgressManager.checkCanceled()
  }
  catch (e: ProcessCanceledException) {
    // otherwise ExceptionInInitializerError happens and the class is screwed forever
    if (!e.stackTrace.any { it.methodName == "<clinit>" }) {
      throw e
    }
  }
}

internal fun isUnderIndicatorOrJob(): Boolean {
  return ProgressIndicatorProvider.getGlobalProgressIndicator() != null || Cancellation.currentJob() != null
}

@ApiStatus.Internal
fun throwAlreadyDisposedError(serviceDescription: String, componentManager: ComponentManagerImpl) {
  throw AlreadyDisposedException("Cannot create $serviceDescription because container is already disposed (container=${componentManager})")
}

internal fun doNotUseConstructorInjectionsMessage(where: String): String {
  return "Please, do not use constructor injection: it slows down initialization and may lead to performance problems ($where). " +
         "See https://plugins.jetbrains.com/docs/intellij/plugin-services.html for details."
}