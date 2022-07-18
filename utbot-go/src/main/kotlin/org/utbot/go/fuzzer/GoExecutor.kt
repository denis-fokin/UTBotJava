package org.utbot.go.fuzzer

import org.utbot.framework.plugin.api.GoUtModel
import org.utbot.framework.plugin.api.GoUtPrimitiveModel
import org.utbot.fuzzer.FuzzedValue
import org.utbot.go.GoFunctionOrMethodNode

@Suppress("UNUSED_PARAMETER")
fun executeGo(
    functionOrMethodNode: GoFunctionOrMethodNode,
    fuzzedParametersValues: List<FuzzedValue>
): GoUtModel {
//    TODO()
    println("Executed $functionOrMethodNode with go")
    return GoUtPrimitiveModel(0)
}