package org.utbot.intellij.plugin.js.fuzzer

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.intClassId

val jsUndefinedClassId = JsClassId("undefined")
val jsIntClassId = JsClassId("int")
val jsBooleanClassId = JsClassId("boolean")
val jsDoubleClassId = JsClassId("double")


val jsPrimitives = setOf(
    jsIntClassId,
    jsBooleanClassId,
    jsDoubleClassId,
)

fun ClassId.toJsClassId() =
    when(this) {
        intClassId -> jsIntClassId
        booleanClassId -> jsBooleanClassId
        doubleClassId -> jsDoubleClassId
        else -> jsUndefinedClassId
    }


val JsClassId.isPrimitive: Boolean
    get() = this in jsPrimitives

val JsClassId.isUndefined: Boolean
    get() = this == jsUndefinedClassId