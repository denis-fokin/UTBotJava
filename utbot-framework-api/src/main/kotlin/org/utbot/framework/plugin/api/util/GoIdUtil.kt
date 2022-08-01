@file:OptIn(ExperimentalUnsignedTypes::class)

package org.utbot.framework.plugin.api.util

import org.utbot.framework.plugin.api.GoClassId
import org.utbot.framework.plugin.api.GoSyntheticNoTypeId
import org.utbot.framework.plugin.api.GoTypeId

val goVoidTypeId = GoSyntheticNoTypeId()

// TODO: check if types below corresponds to the full useful built-in Go types

@Suppress("unused")
val goErrorTypeId = GoTypeId("error", isErrorType = true)
val goAnyTypeId = GoTypeId("any")

val goByteTypeId = GoTypeId("byte", UByte::class) // = uint8
val goBoolTypeId = GoTypeId("bool", Boolean::class)

val goComplex128TypeId = GoTypeId("complex128")
val goComplex64TypeId = GoTypeId("complex64")

val goFloat32TypeId = GoTypeId("float32", Float::class)
val goFloat64TypeId = GoTypeId("float64", Double::class)

val goIntTypeId = GoTypeId("int", Int::class)
val goInt16TypeId = GoTypeId("int16", Short::class)
val goInt32TypeId = GoTypeId("int32", Int::class)
val goInt64TypeId = GoTypeId("int64", Long::class)
val goInt8TypeId = GoTypeId("int8", Byte::class)

val goRuneTypeId = GoTypeId("rune", Int::class) // = int32
val goStringTypeId = GoTypeId("string", String::class)

val goUintTypeId = GoTypeId("uint", UInt::class)
val goUint16TypeId = GoTypeId("uint16", UShort::class)
val goUint32TypeId = GoTypeId("uint32", UInt::class)
val goUint64TypeId = GoTypeId("uint64", ULong::class)
val goUint8TypeId = GoTypeId("uint8", UByte::class)
val goUintPtrTypeId = GoTypeId("uintptr", ULong::class)


val goPrimitives = setOf(
    goByteTypeId,
    goBoolTypeId,
    goComplex128TypeId,
    goComplex64TypeId,
    goFloat32TypeId,
    goFloat64TypeId,
    goIntTypeId,
    goInt16TypeId,
    goInt32TypeId,
    goInt64TypeId,
    goInt8TypeId,
    goRuneTypeId,
    goStringTypeId,
    goUintTypeId,
    goUint16TypeId,
    goUint32TypeId,
    goUint64TypeId,
    goUint8TypeId,
    goUintPtrTypeId,
)

val GoClassId.isPrimitive: Boolean
    get() = this in goPrimitives

val goTypesNeverRequireExplicitCast = setOf(
    goBoolTypeId,
    goComplex128TypeId,
    goComplex64TypeId,
    goFloat64TypeId,
    goIntTypeId,
    goStringTypeId,
)

val GoTypeId.neverRequiresExplicitCast: Boolean
    get() = this in goTypesNeverRequireExplicitCast