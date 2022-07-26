package org.utbot.framework.plugin.api.util

import org.utbot.framework.plugin.api.GoClassId
import org.utbot.framework.plugin.api.GoCommonClassId

// TODO: check if types below corresponds to the full useful built-in Go types

@Suppress("unused")
val goErrorClassId = GoCommonClassId("error", isErrorType = true)
@Suppress("unused")
val goVoidClassId = GoCommonClassId("", Unit::class)

val goByteClassId = GoCommonClassId("byte", UByte::class) // = uint8
val goBoolClassId = GoCommonClassId("bool", Boolean::class)

val goComplex128ClassId = GoCommonClassId("complex128")
val goComplex64ClassId = GoCommonClassId("complex64")

val goFloat32ClassId = GoCommonClassId("float32", Float::class)
val goFloat64ClassId = GoCommonClassId("float64", Double::class)

val goIntClassId = GoCommonClassId("int", Int::class)
val goInt16ClassId = GoCommonClassId("int16", Short::class)
val goInt32ClassId = GoCommonClassId("int32", Int::class)
val goInt64ClassId = GoCommonClassId("int64", Long::class)
val goInt8ClassId = GoCommonClassId("int8", Byte::class)

val goRuneClassId = GoCommonClassId("rune", Int::class) // = int32
val goStringClassId = GoCommonClassId("string", String::class)

val goUintClassId = GoCommonClassId("uint", UInt::class)
val goUint16ClassId = GoCommonClassId("uint16", UShort::class)
val goUint32ClassId = GoCommonClassId("uint32", UInt::class)
val goUint64ClassId = GoCommonClassId("uint64", ULong::class)
val goUint8ClassId = GoCommonClassId("uint8", UByte::class)
val goUintPtrClassId = GoCommonClassId("uintptr", ULong::class)


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