package org.utbot.framework.plugin.api.util

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.JsClassId

val jsUndefinedClassId = JsClassId("undefined")
val jsNumberClassId = JsClassId("number")
val jsBooleanClassId = JsClassId("boolean")
val jsDoubleClassId = JsClassId("double")
val jsStringClassId = JsClassId("string")


val jsPrimitives = setOf(
    jsNumberClassId,
    jsBooleanClassId,
    jsDoubleClassId,
)

fun ClassId.toJsClassId() =
    when (this) {
        intClassId -> jsNumberClassId
        booleanClassId -> jsBooleanClassId
        doubleClassId -> jsDoubleClassId
        stringClassId -> jsStringClassId
        longClassId -> jsNumberClassId
        else -> jsUndefinedClassId
    }


val JsClassId.isJsPrimitive: Boolean
    get() = this in jsPrimitives

val JsClassId.isUndefined: Boolean
    get() = this == jsUndefinedClassId