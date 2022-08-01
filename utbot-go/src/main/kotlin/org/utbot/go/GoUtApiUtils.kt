package org.utbot.go

import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.goFloat32TypeId

fun getExplicitCastModeForFloatModel(
    typeId: GoTypeId,
    explicitCastRequired: Boolean,
    defaultFloat32Mode: ExplicitCastMode
): ExplicitCastMode {
    if (explicitCastRequired) {
        return ExplicitCastMode.REQUIRED
    }
    return if (typeId == goFloat32TypeId) {
        defaultFloat32Mode
    } else { // goFloat64TypeId case
        ExplicitCastMode.NEVER
    }
}

fun GoUtModel.isNaNOrInf(): Boolean = this is GoUtFloatNaNModel || this is GoUtFloatInfModel

fun GoUtModel.doesNotContainNaNOrInf(): Boolean {
    if (this.isNaNOrInf()) return false
    val asComplexModel = (this as? GoUtComplexModel) ?: return true
    return !(asComplexModel.realValue.isNaNOrInf() || asComplexModel.imagValue.isNaNOrInf())
}

fun GoUtModel.containsNaNOrInf(): Boolean = !this.doesNotContainNaNOrInf()