package codegen

import com.oracle.js.parser.ir.FunctionNode
import org.utbot.framework.plugin.api.JsUtModel
import org.utbot.fuzzer.FuzzedValue

@Suppress("unused", "UNUSED_PARAMETER") // Not impl yet
object JsTestCodeGenerator {

    fun generateTestCode(
        method: FunctionNode,
        params: List<FuzzedValue>,
        returnValue: JsUtModel,
    ): String {
        TODO("Not impl yet")
    }
}