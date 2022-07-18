package org.utbot.go.providers

import org.utbot.framework.plugin.api.GoUtPrimitiveModel
import org.utbot.framework.plugin.api.util.*
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue

// This class is highly based on PrimitiveDefaultsModelProvider.
@Suppress("DuplicatedCode")
object GoPrimitivesModelProvider : ModelProvider {

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.parametersMap.forEach { (classId, parameterIndices) ->
            val primitives: List<FuzzedValue> = when (classId) {
                goByteClassId, goUint8ClassId -> listOf(
                    GoUtPrimitiveModel(Char.MIN_VALUE).fuzzed { summary = "%var% = Char.MIN_VALUE" },
                    GoUtPrimitiveModel(Char.MAX_VALUE).fuzzed { summary = "%var% = Char.MAX_VALUE" },
                )
                goBoolClassId -> listOf(
                    GoUtPrimitiveModel(false).fuzzed { summary = "%var% = false" },
                    GoUtPrimitiveModel(true).fuzzed { summary = "%var% = true" }
                )
                goComplex128ClassId, goComplex64ClassId -> TODO()
                byteClassId -> listOf(
                    GoUtPrimitiveModel(0.toByte()).fuzzed { summary = "%var% = 0" },
                    GoUtPrimitiveModel(1.toByte()).fuzzed { summary = "%var% > 0" },
                    GoUtPrimitiveModel((-1).toByte()).fuzzed { summary = "%var% < 0" },
                    GoUtPrimitiveModel(Byte.MIN_VALUE).fuzzed { summary = "%var% = Byte.MIN_VALUE" },
                    GoUtPrimitiveModel(Byte.MAX_VALUE).fuzzed { summary = "%var% = Byte.MAX_VALUE" },
                )
                goFloat32ClassId -> listOf(
                    GoUtPrimitiveModel(0.0f).fuzzed { summary = "%var% = 0f" },
                    GoUtPrimitiveModel(1.1f).fuzzed { summary = "%var% > 0f" },
                    GoUtPrimitiveModel(-1.1f).fuzzed { summary = "%var% < 0f" },
                    GoUtPrimitiveModel(Float.MIN_VALUE).fuzzed { summary = "%var% = Float.MIN_VALUE" },
                    GoUtPrimitiveModel(Float.MAX_VALUE).fuzzed { summary = "%var% = Float.MAX_VALUE" },
                    GoUtPrimitiveModel(Float.NEGATIVE_INFINITY).fuzzed { summary = "%var% = Float.NEGATIVE_INFINITY" },
                    GoUtPrimitiveModel(Float.POSITIVE_INFINITY).fuzzed { summary = "%var% = Float.POSITIVE_INFINITY" },
                    GoUtPrimitiveModel(Float.NaN).fuzzed { summary = "%var% = Float.NaN" },
                )
                goFloat64ClassId -> listOf(
                    GoUtPrimitiveModel(0.0).fuzzed { summary = "%var% = 0.0" },
                    GoUtPrimitiveModel(1.1).fuzzed { summary = "%var% > 0.0" },
                    GoUtPrimitiveModel(-1.1).fuzzed { summary = "%var% < 0.0" },
                    GoUtPrimitiveModel(Double.MIN_VALUE).fuzzed { summary = "%var% = Double.MIN_VALUE" },
                    GoUtPrimitiveModel(Double.MAX_VALUE).fuzzed { summary = "%var% = Double.MAX_VALUE" },
                    GoUtPrimitiveModel(Double.NEGATIVE_INFINITY).fuzzed { summary = "%var% = Double.NEGATIVE_INFINITY" },
                    GoUtPrimitiveModel(Double.POSITIVE_INFINITY).fuzzed { summary = "%var% = Double.POSITIVE_INFINITY" },
                    GoUtPrimitiveModel(Double.NaN).fuzzed { summary = "%var% = Double.NaN" },
                )
                goInt16ClassId -> listOf(
                    GoUtPrimitiveModel(0.toShort()).fuzzed { summary = "%var% = 0" },
                    GoUtPrimitiveModel(1.toShort()).fuzzed { summary = "%var% > 0" },
                    GoUtPrimitiveModel((-1).toShort()).fuzzed { summary = "%var% < 0" },
                    GoUtPrimitiveModel(Short.MIN_VALUE).fuzzed { summary = "%var% = Short.MIN_VALUE" },
                    GoUtPrimitiveModel(Short.MAX_VALUE).fuzzed { summary = "%var% = Short.MAX_VALUE" },
                )
                goIntClassId, goInt32ClassId, goRuneClassId -> listOf(
                    GoUtPrimitiveModel(0).fuzzed { summary = "%var% = 0" },
                    GoUtPrimitiveModel(1).fuzzed { summary = "%var% > 0" },
                    GoUtPrimitiveModel((-1)).fuzzed { summary = "%var% < 0" },
                    GoUtPrimitiveModel(Int.MIN_VALUE).fuzzed { summary = "%var% = Int.MIN_VALUE" },
                    GoUtPrimitiveModel(Int.MAX_VALUE).fuzzed { summary = "%var% = Int.MAX_VALUE" },
                )
                goInt64ClassId -> listOf(
                    GoUtPrimitiveModel(0L).fuzzed { summary = "%var% = 0L" },
                    GoUtPrimitiveModel(1L).fuzzed { summary = "%var% > 0L" },
                    GoUtPrimitiveModel(-1L).fuzzed { summary = "%var% < 0L" },
                    GoUtPrimitiveModel(Long.MIN_VALUE).fuzzed { summary = "%var% = Long.MIN_VALUE" },
                    GoUtPrimitiveModel(Long.MAX_VALUE).fuzzed { summary = "%var% = Long.MAX_VALUE" },
                )
                goInt8ClassId -> TODO()
                goStringClassId -> listOf(
                    GoUtPrimitiveModel("").fuzzed { summary = "%var% = empty string" },
                    GoUtPrimitiveModel("   ").fuzzed { summary = "%var% = blank string" },
                    GoUtPrimitiveModel("string").fuzzed { summary = "%var% != empty string" },
                    GoUtPrimitiveModel("\\n\\t\\r").fuzzed { summary = "%var% has special characters" },
                    // TODO: get rid of double slash
                )
                goUintClassId, goUint16ClassId, goUint32ClassId, goUint64ClassId -> TODO()
                else -> listOf()
            }

            primitives.forEach { model ->
                parameterIndices.forEach { index ->
                    yieldValue(index, model)
                }
            }
        }
    }
}