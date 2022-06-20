package org.utbot.framework.synthesis

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.util.primitives

sealed class SynthesisUnit {
    abstract val classId: ClassId
}

data class ObjectUnit(
    override val classId: ClassId
) : SynthesisUnit() {
    fun isPrimitive() = classId in primitives
}


data class MethodUnit(
    override val classId: ClassId,
    val method: ExecutableId,
    val params: List<SynthesisUnit>
) : SynthesisUnit()

fun SynthesisUnit.isFullyDefined(): Boolean = when (this) {
    is ObjectUnit -> isPrimitive()
    is MethodUnit -> params.all { it.isFullyDefined() }
}