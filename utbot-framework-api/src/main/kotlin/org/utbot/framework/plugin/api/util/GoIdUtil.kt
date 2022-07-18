package org.utbot.framework.plugin.api.util

import org.utbot.framework.plugin.api.GoClassId

// TODO: check if types below corresponds to the full useful built-in Go types

val goByteClassId = GoClassId("byte") // = uint8
val goBoolClassId = GoClassId("bool")

val goComplex128ClassId = GoClassId("complex128")
val goComplex64ClassId = GoClassId("complex64")

val goFloat32ClassId = GoClassId("float32")
val goFloat64ClassId = GoClassId("float64")

val goIntClassId = GoClassId("int")
val goInt16ClassId = GoClassId("int16")
val goInt32ClassId = GoClassId("int32")
val goInt64ClassId = GoClassId("int64")
val goInt8ClassId = GoClassId("int8")

val goRuneClassId = GoClassId("rune") // = int32
val goStringClassId = GoClassId("string")

val goUintClassId = GoClassId("uint")
val goUint16ClassId = GoClassId("uint16")
val goUint32ClassId = GoClassId("uint32")
val goUint64ClassId = GoClassId("uint64")
val goUint8ClassId = GoClassId("uint8")
val goUintPtrClassId = GoClassId("uintptr")


val goPrimitives = setOf(
    goByteClassId,
    goBoolClassId,
    goComplex128ClassId,
    goComplex64ClassId,
    goFloat32ClassId,
    goFloat64ClassId,
    goIntClassId,
    goInt16ClassId,
    goInt32ClassId,
    goInt64ClassId,
    goInt8ClassId,
    goRuneClassId,
    goStringClassId,
    goUintClassId,
    goUint16ClassId,
    goUint32ClassId,
    goUint64ClassId,
    goUint8ClassId,
    goUintPtrClassId,
)

val GoClassId.isPrimitive: Boolean
    get() = this in goPrimitives