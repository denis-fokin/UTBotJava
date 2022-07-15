package org.utbot.intellij.plugin.js.codegen

import jdk.nashorn.internal.ir.FunctionNode
import org.utbot.framework.plugin.api.UtModel
import org.utbot.fuzzer.FuzzedValue

data class JsTest(
    val method: FunctionNode,
    val params: List<FuzzedValue>,
    val returnValue: UtModel,
)