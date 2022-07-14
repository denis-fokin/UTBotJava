package org.utbot.intellij.plugin.js.fuzzer.providers

import org.utbot.framework.plugin.api.JsPrimitiveModel
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.intellij.plugin.js.fuzzer.jsUndefinedClassId
import java.util.function.BiConsumer

object JsUndefinedModelProvider : ModelProvider {

    override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, FuzzedValue>) {
        val parameters = description.parametersMap.getOrDefault(jsUndefinedClassId, emptyList())
        val primitives: List<FuzzedValue> = generateValues()
        primitives.forEach { model ->
            parameters.forEach { index ->
                consumer.accept(index, model)
            }
        }
    }

    private fun generateValues() =
        listOf(
            JsPrimitiveModel(false).fuzzed { summary = "%var% = false" },
            JsPrimitiveModel(true).fuzzed { summary = "%var% = false" },

            JsPrimitiveModel(0).fuzzed { summary = "%var% = 0" },
            JsPrimitiveModel(-1).fuzzed { summary = "%var% < 0" },
            JsPrimitiveModel(1).fuzzed { summary = "%var% > 0" },
            JsPrimitiveModel(9007199254740991).fuzzed { summary = "%var% = Integer.MAX_SAFE_VALUE" },
            JsPrimitiveModel(-9007199254740991).fuzzed { summary = "%var% = Integer.MIN_SAFE_VALUE" },

            JsPrimitiveModel(0.0).fuzzed { summary = "%var% = 0.0" },
            JsPrimitiveModel(-1.0).fuzzed { summary = "%var% < 0.0" },
            JsPrimitiveModel(1.0).fuzzed { summary = "%var% > 0.0" },
        )
}