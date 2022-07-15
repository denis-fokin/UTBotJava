package org.utbot.intellij.plugin.js.codegen

import com.oracle.js.parser.ir.FunctionNode
import org.utbot.framework.plugin.api.JsUtModel
import org.utbot.fuzzer.FuzzedValue

data class JsTest(
    val method: FunctionNode,
    val params: List<FuzzedValue>,
    val returnValue: JsUtModel,
)