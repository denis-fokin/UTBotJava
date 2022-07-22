package fuzzer.providers

import org.utbot.framework.plugin.api.JsPrimitiveModel
import org.utbot.framework.plugin.api.util.jsStringClassId
import org.utbot.framework.plugin.api.util.jsUndefinedClassId
import org.utbot.framework.plugin.api.util.toJsClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.providers.StringConstantModelProvider
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