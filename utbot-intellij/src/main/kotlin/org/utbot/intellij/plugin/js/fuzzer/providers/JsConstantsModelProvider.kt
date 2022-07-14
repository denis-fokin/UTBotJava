package org.utbot.intellij.plugin.js.fuzzer.providers

import org.utbot.framework.plugin.api.JsPrimitiveModel
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedOp
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.intellij.plugin.js.fuzzer.isPrimitive
import org.utbot.intellij.plugin.js.fuzzer.jsUndefinedClassId
import org.utbot.intellij.plugin.js.fuzzer.toJsClassId
import java.util.function.BiConsumer

object JsConstantsModelProvider : ModelProvider {

    override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, FuzzedValue>) {
        description.concreteValues
            .asSequence()
            .filter { (classId, _) ->
                (classId.toJsClassId()).isPrimitive }
            .forEach { (_, value, op) ->
                sequenceOf(
                    JsPrimitiveModel(value).fuzzed { summary = "%var% = $value" },
                    modifyValue(value, op)
                )
                    .filterNotNull()
                    .forEach { m ->
                        description.parametersMap.getOrElse(m.model.classId) { emptyList() }.forEach { index ->
                            consumer.accept(index, m)
                        }
                        description.parametersMap.getOrElse(jsUndefinedClassId) { emptyList() }.forEach { index ->
                            consumer.accept(index, m)
                        }
                    }
            }
    }

    private fun modifyValue(value: Any, op: FuzzedOp): FuzzedValue? {
        if (!op.isComparisonOp()) return null
        val multiplier = if (op == FuzzedOp.LT || op == FuzzedOp.GE) -1 else 1
        return when(value) {
            is Boolean -> value.not()
            is Byte -> value + multiplier.toByte()
            is Char -> (value.toInt() + multiplier).toChar()
            is Short -> value + multiplier.toShort()
            is Int -> value + multiplier
            is Long -> value + multiplier.toLong()
            is Float -> value + multiplier.toDouble()
            is Double -> value + multiplier.toDouble()
            else -> null
        }?.let { JsPrimitiveModel(it).fuzzed { summary = "%var% ${
            (if (op == FuzzedOp.EQ || op == FuzzedOp.LE || op == FuzzedOp.GE) {
                op.reverseOrNull() ?: error("cannot find reverse operation for $op")
            } else op).sign
        } $value" } }
    }
}