package org.utbot.intellij.plugin.js.fuzzer

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.*

val jsUndefinedClassId = JsClassId("undefined")
val jsIntClassId = JsClassId("int")
val jsBooleanClassId = JsClassId("boolean")
val jsDoubleClassId = JsClassId("double")
val jsStringClassId = JsClassId("string")
val jsLongClassId = JsClassId("long")


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
        stringClassId -> jsStringClassId
        longClassId -> jsLongClassId
        else -> jsUndefinedClassId
    }


val JsClassId.isPrimitive: Boolean
    get() = this in jsPrimitives

val JsClassId.isUndefined: Boolean
    get() = this == jsUndefinedClassId