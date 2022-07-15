package org.utbot.intellij.plugin.js.fuzzer.providers

import org.utbot.framework.plugin.api.JsPrimitiveModel
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.providers.StringConstantModelProvider
import org.utbot.intellij.plugin.js.fuzzer.jsStringClassId
import org.utbot.intellij.plugin.js.fuzzer.jsUndefinedClassId
import org.utbot.intellij.plugin.js.fuzzer.toJsClassId
import java.util.function.BiConsumer
import kotlin.random.Random

object JsStringModelProvider : ModelProvider {
    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        val random = Random(72923L)
        description.concreteValues
            .asSequence()
            .filter { (classId, _) -> classId.toJsClassId() == jsStringClassId }
            .forEach { (_, value, op) ->
                listOf(value, StringConstantModelProvider.mutate(random, value as? String, op))
                    .asSequence()
                    .filterNotNull()
                    .map { JsPrimitiveModel(it) }.forEach { model ->
                        description.parametersMap.getOrElse(model.classId) { emptyList() }.forEach { index ->
                            yield(FuzzedParameter(index, model.fuzzed { summary = "%var% = string" }))
                        }
                        description.parametersMap.getOrElse(jsUndefinedClassId) { emptyList() }.forEach { index ->
                            yield(FuzzedParameter(index, model.fuzzed { summary = "%var% = string" }))
                        }
                    }
            }
    }
}