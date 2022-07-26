package org.utbot.go.executor

import org.utbot.framework.plugin.api.GoUtModel

interface GoUtExecutionResult

interface GoUtExecutionCompleted : GoUtExecutionResult {
    val models: List<GoUtModel>
}

data class GoUtExecutionSuccess(override val models: List<GoUtModel>) : GoUtExecutionCompleted

data class GoUtExecutionWithNonNullError(override val models: List<GoUtModel>) : GoUtExecutionCompleted

class GoUtPanicFailure(val failureMessage: String?): GoUtExecutionResult