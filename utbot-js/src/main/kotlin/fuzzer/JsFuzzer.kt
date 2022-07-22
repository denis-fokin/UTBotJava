package fuzzer

import com.oracle.js.parser.ir.FunctionNode
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.fuzz
import fuzzer.providers.JsConstantsModelProvider
import fuzzer.providers.JsStringModelProvider
import fuzzer.providers.JsUndefinedModelProvider

object JsFuzzer {
    fun jsFuzzing(
        modelProvider: (ModelProvider) -> ModelProvider = { it },
        method: FunctionNode,
        methodUnderTestDescription: FuzzedMethodDescription
    ): Sequence<List<FuzzedValue>> {
        val modelProviderWithFallback = modelProvider(
            ModelProvider.of(
                JsConstantsModelProvider,
                JsUndefinedModelProvider,
                JsStringModelProvider,
            )
        )
        return fuzz(methodUnderTestDescription, modelProviderWithFallback)
    }
}
