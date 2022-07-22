package codegen

import com.oracle.js.parser.ir.FunctionNode
import org.utbot.framework.plugin.api.JsUtModel
import org.utbot.fuzzer.FuzzedValue

data class JsTestModel(
    val method: FunctionNode,
    val params: List<FuzzedValue>,
    val returnValue: JsUtModel,
)