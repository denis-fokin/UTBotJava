package fuzzer

import fuzzer.providers.*
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.fuzz

object JsFuzzer {
    fun jsFuzzing(
        modelProvider: (ModelProvider) -> ModelProvider = { it },
        methodUnderTestDescription: FuzzedMethodDescription
    ): Sequence<List<FuzzedValue>> {
        val modelProviderWithFallback = modelProvider(
            ModelProvider.of(
                JsConstantsModelProvider,
                JsUndefinedModelProvider,
                JsStringModelProvider,
                JsMultipleTypesModelProvider,
                JsPrimitivesModelProvider,
            )
        )
        return fuzz(methodUnderTestDescription, modelProviderWithFallback)
    }
}
