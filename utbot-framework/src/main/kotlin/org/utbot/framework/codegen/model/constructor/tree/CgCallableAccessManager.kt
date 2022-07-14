package org.utbot.framework.codegen.model.constructor.tree

import kotlinx.collections.immutable.PersistentList
import org.utbot.framework.codegen.Junit5
import org.utbot.framework.codegen.TestNg
import org.utbot.framework.codegen.model.constructor.builtin.TestClassUtilMethodProvider
import org.utbot.framework.codegen.model.constructor.builtin.any
import org.utbot.framework.codegen.model.constructor.builtin.anyOfClass
import org.utbot.framework.codegen.model.constructor.builtin.forName
import org.utbot.framework.codegen.model.constructor.builtin.getDeclaredConstructor
import org.utbot.framework.codegen.model.constructor.builtin.getDeclaredMethod
import org.utbot.framework.codegen.model.constructor.builtin.getTargetException
import org.utbot.framework.codegen.model.constructor.builtin.invoke
import org.utbot.framework.codegen.model.constructor.builtin.newInstance
import org.utbot.framework.codegen.model.constructor.builtin.setAccessible
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.context.CgContextOwner
import org.utbot.framework.codegen.model.constructor.util.CgComponents
import org.utbot.framework.codegen.model.constructor.util.classCgClassId
import org.utbot.framework.codegen.model.constructor.util.getAmbiguousOverloadsOf
import org.utbot.framework.codegen.model.constructor.util.importIfNeeded
import org.utbot.framework.codegen.model.constructor.util.isTestClassUtil
import org.utbot.framework.codegen.model.constructor.util.typeCast
import org.utbot.framework.codegen.model.tree.CgAllocateArray
import org.utbot.framework.codegen.model.tree.CgAssignment
import org.utbot.framework.codegen.model.tree.CgConstructorCall
import org.utbot.framework.codegen.model.tree.CgExecutableCall
import org.utbot.framework.codegen.model.tree.CgExpression
import org.utbot.framework.codegen.model.tree.CgFieldAccess
import org.utbot.framework.codegen.model.tree.CgGetJavaClass
import org.utbot.framework.codegen.model.tree.CgMethodCall
import org.utbot.framework.codegen.model.tree.CgSpread
import org.utbot.framework.codegen.model.tree.CgStatement
import org.utbot.framework.codegen.model.tree.CgStaticFieldAccess
import org.utbot.framework.codegen.model.tree.CgThisInstance
import org.utbot.framework.codegen.model.tree.CgValue
import org.utbot.framework.codegen.model.tree.CgVariable
import org.utbot.framework.codegen.model.util.at
import org.utbot.framework.codegen.model.util.isAccessibleFrom
import org.utbot.framework.codegen.model.util.nullLiteral
import org.utbot.framework.codegen.model.util.resolve
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.util.exceptions
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.framework.plugin.api.util.isSubtypeOf
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.method
import org.utbot.framework.plugin.api.util.objectArrayClassId
import org.utbot.framework.plugin.api.util.objectClassId

typealias Block = PersistentList<CgStatement>

class CgIncompleteMethodCall(val method: MethodId, val caller: CgExpression?)

/**
 * Provides DSL methods for method and field access elements creation
 *
 * Checks the accessibility of methods and fields and replaces
 * direct access with reflective access when needed
 */
interface CgCallableAccessManager {
    operator fun CgExpression?.get(methodId: MethodId): CgIncompleteMethodCall

    operator fun ClassId.get(staticMethodId: MethodId): CgIncompleteMethodCall

    operator fun ConstructorId.invoke(vararg args: Any?): CgExecutableCall

    operator fun CgIncompleteMethodCall.invoke(vararg args: Any?): CgMethodCall

    // non-static fields
    operator fun CgExpression.get(fieldId: FieldId): CgFieldAccess
//        CgFieldAccess(this, fieldId)

    // static fields
    operator fun ClassId.get(fieldId: FieldId): CgStaticFieldAccess
//        CgStaticFieldAccess(fieldId)

}

internal class CgCallableAccessManagerImpl(val context: CgContext) : CgCallableAccessManager,
    CgContextOwner by context {

    private val statementConstructor by lazy { CgComponents.getStatementConstructorBy(context) }

    private val variableConstructor by lazy { CgComponents.getVariableConstructorBy(context) }

    override operator fun CgExpression?.get(methodId: MethodId): CgIncompleteMethodCall =
        CgIncompleteMethodCall(methodId, this)

    override operator fun ClassId.get(staticMethodId: MethodId): CgIncompleteMethodCall =
        CgIncompleteMethodCall(staticMethodId, null)

    override operator fun ConstructorId.invoke(vararg args: Any?): CgExecutableCall {
        val resolvedArgs = args.resolve()
        val constructorCall = if (this canBeCalledWith resolvedArgs) {
            CgConstructorCall(this, resolvedArgs.guardedForDirectCallOf(this))
        } else {
            callWithReflection(resolvedArgs)
        }
        newConstructorCall(this)
        return constructorCall
    }

    override operator fun CgIncompleteMethodCall.invoke(vararg args: Any?): CgMethodCall {
        val resolvedArgs = args.resolve()
        val (receiverSuitability, argumentsSuitability) = method.callApplicability(caller, resolvedArgs)
        val methodCall = when {
            !argumentsSuitability -> {
                // arguments are not suitable, so we use reflection to call a method
                method.callWithReflection(caller, resolvedArgs)
            }
            receiverSuitability is ReceiverSuitability.ReflectionOnly -> {
                // receiver is not suitable, so we use reflection to call a method
                method.callWithReflection(caller, resolvedArgs)
            }
            receiverSuitability is ReceiverSuitability.RequiresTypeCast -> {
                // receiver is suitable, but requires a type cast to call a method
                CgMethodCall(
                    caller = typeCast(receiverSuitability.targetType, caller!!),
                    method,
                    resolvedArgs.guardedForDirectCallOf(method)
                )
            }
            receiverSuitability is ReceiverSuitability.Suitable -> {
                // receiver is suitable, so we can call a method directly
                CgMethodCall(caller, method, resolvedArgs.guardedForDirectCallOf(method))
            }
            else -> error("Impossible case: all method call suitability levels have been considered")
        }
//        val methodCall = if (method.callApplicability(caller, resolvedArgs)) {
//            CgMethodCall(caller, method, resolvedArgs.guardedForDirectCallOf(method))
//        } else {
//            method.callWithReflection(caller, resolvedArgs)
//        }
        newMethodCall(method)
        return methodCall
    }

    override fun CgExpression.get(fieldId: FieldId): CgFieldAccess {
        return CgFieldAccess(this, fieldId)
    }

    override fun ClassId.get(fieldId: FieldId): CgStaticFieldAccess {
        return CgStaticFieldAccess(fieldId)
    }

    private fun newMethodCall(methodId: MethodId) {
        if (isTestClassUtil(methodId)) requiredUtilMethods += methodId
        importIfNeeded(methodId)

        //Builtin methods does not have jClass, so [methodId.method] will crash on it,
        //so we need to collect required exceptions manually from source codes
        if (isTestClassUtil(methodId)) {
            (currentUtilMethodProvider as TestClassUtilMethodProvider)
                .findExceptionTypesOf(methodId)
                .forEach { addExceptionIfNeeded(it) }
            return
        }

        if (methodId == getTargetException) {
            addExceptionIfNeeded(Throwable::class.id)
        }

        val methodIsUnderTestAndThrowsExplicitly = methodId == currentExecutable
                && currentExecution?.result is UtExplicitlyThrownException
        val frameworkSupportsAssertThrows = testFramework == Junit5 || testFramework == TestNg

        //If explicit exception is wrapped with assertThrows,
        // no "throws" in test method signature is required.
        if (methodIsUnderTestAndThrowsExplicitly && frameworkSupportsAssertThrows) {
            return
        }

        methodId.method.exceptionTypes.forEach { addExceptionIfNeeded(it.id) }
    }

    private fun newConstructorCall(constructorId: ConstructorId) {
        importIfNeeded(constructorId.classId)
        for (exception in constructorId.exceptions) {
            addExceptionIfNeeded(exception)
        }
    }

    private infix fun CgExpression.canBeReceiverOf(executable: MethodId): ReceiverSuitability {
        return when {
            // method of the current test class can be called on its 'this' instance
            currentTestClass == executable.classId && this isThisInstanceOf currentTestClass -> ReceiverSuitability.Suitable
            // method of a class can be called on an object of this class or any of its subtypes
            this.type isSubtypeOf executable.classId -> ReceiverSuitability.Suitable
            // if receiver type does not suit the method, then we may try to use type cast, if the method's class can be accessed in the code
            executable.classId isAccessibleFrom testClassPackageName -> ReceiverSuitability.RequiresTypeCast(executable.classId)
            // if no other options worked, we have to use reflection
            else -> ReceiverSuitability.ReflectionOnly
        }
    }

    private infix fun CgExpression.canBeArgOf(type: ClassId): Boolean {
        // TODO: SAT-1210 support generics so that we wouldn't need to check specific cases such as this one
        if (this is CgExecutableCall && (executableId == any || executableId == anyOfClass)) {
            return true
        }
        return this == nullLiteral() && type.isAccessibleFrom(testClassPackageName)
                || this.type isSubtypeOf type
    }

    private infix fun CgExpression?.isThisInstanceOf(classId: ClassId): Boolean =
        this is CgThisInstance && this.type == classId

    /**
     * Check whether @receiver (list of expressions) is a valid list of arguments for [executableId]
     *
     * First, we check all arguments except for the last one.
     * It is done to consider the last argument separately since it can be a vararg,
     * which requires some additional checks.
     *
     * For the last argument there can be several cases:
     * - Last argument is not of array type - then we simply check this argument as all the others
     * - Last argument is of array type:
     *     - Given arguments and parameters have the same size
     *         - Last argument is an array and it matches last parameter array type
     *         - Last argument is a single element of a vararg parameter - then we check
     *           if argument's type matches the vararg element's type
     *     - Given arguments and parameters have different size (last parameter is vararg) - then we
     *       check if all of the given arguments match the vararg element's type
     *
     */
    private infix fun List<CgExpression>.canBeArgsOf(executableId: ExecutableId): Boolean {
        val paramTypes = executableId.parameters

        // no arguments case
        if (paramTypes.isEmpty()) {
            return this.isEmpty()
        }

        val paramTypesExceptLast = paramTypes.dropLast(1)
        val lastParamType = paramTypes.last()

        // considering all arguments except the last one
        for ((arg, paramType) in (this zip paramTypesExceptLast)) {
            if (!(arg canBeArgOf paramType)) return false
        }

        // when the last parameter is not of array type
        if (!lastParamType.isArray) {
            val lastArg = this.last()
            return lastArg canBeArgOf lastParamType
        }

        // when arguments and parameters have equal size
        if (size == paramTypes.size) {
            val lastArg = this.last()
            return when {
                // last argument matches last param type
                lastArg canBeArgOf lastParamType -> true
                // last argument is a single element of a vararg parameter
                lastArg canBeArgOf lastParamType.elementClassId!! -> true
                else -> false
            }
        }

        // when arguments size is greater than the parameters size
        // meaning that the last parameter is vararg
        return subList(paramTypes.size - 1, size).all {
            it canBeArgOf lastParamType.elementClassId!!
        }
    }

    // TODO: create some class for arguments suitability like it is done for receiver suitability (?)
    private data class MethodCallApplicability(
        val receiverSuitability: ReceiverSuitability,
        val argumentsSuitability: Boolean
    )

    /**
     * @return true if a method can be called with the given arguments without reflection
     */
//    private fun MethodId.canBeCalledWith(caller: CgExpression?, args: List<CgExpression>): Boolean {
    private fun MethodId.callApplicability(caller: CgExpression?, args: List<CgExpression>): MethodCallApplicability {
        val isMethodStatic = this.isStatic
        val isMethodOfCurrentClass = this.classId == currentTestClass

        // If method is from the current class, or it is static, then it may not have a caller.
        // Otherwise, it must have a non-null caller.
        return when {
            isMethodStatic -> {
                // caller must be null
                require(caller == null) { "Caller expression of a static method call must be null" }
                MethodCallApplicability(
                    receiverSuitability = ReceiverSuitability.Suitable,
                    argumentsSuitability = this isAccessibleFrom testClassPackageName && args canBeArgsOf this
                )
            }
            isMethodOfCurrentClass -> {
                // caller may be null, if we are calling a method of the current class from within this class
                MethodCallApplicability(
                    receiverSuitability = caller?.canBeReceiverOf(this) ?: ReceiverSuitability.Suitable,
                    argumentsSuitability = this isAccessibleFrom testClassPackageName && args canBeArgsOf this
                )
            }
            else -> {
                requireNotNull(caller) { "Method must have a caller, unless it is the method of the current test class or a static method" }
                MethodCallApplicability(
                    receiverSuitability = caller canBeReceiverOf this,
                    argumentsSuitability = this isAccessibleFrom testClassPackageName && args canBeArgsOf this
                )
            }
        }
    }

    private fun FieldId.fieldAccessApplicability(accessor: CgExpression?): ReceiverSuitability {
        val isFieldStatic = this.isStatic
        val isFieldOfCurrentClass = this.declaringClass == currentTestClass

        when {
            isFieldStatic -> {
                require(accessor == null) { "Accessor expression of a static field access must be null" }
                return ReceiverSuitability.Suitable
            }
            isFieldOfCurrentClass -> {
                return when {
                    accessor == null -> ReceiverSuitability.Suitable
                    accessor isThisInstanceOf currentTestClass -> ReceiverSuitability.Suitable
                    accessor.type isSubtypeOf currentTestClass -> ReceiverSuitability.Suitable
                    else -> ReceiverSuitability.ReflectionOnly
                }
            }
        }

        requireNotNull(accessor) {
            "Field access must have a non-null accessor, unless it is the field of the current test class or a static field"
        }

        if (this.declaringClass == accessor.type) {
            return ReceiverSuitability.Suitable
        }

        val fieldDeclaringClassType = this.declaringClass
        val accessorType = accessor.type

        val fieldName = this.name

        if (accessorType isSubtypeOf fieldDeclaringClassType) {
            val subtype = accessorType.jClass
            val supertype = fieldDeclaringClassType.jClass

            val subtypeField = subtype.declaredFields.find { it.name == fieldName }
            val supertypeField = supertype.declaredFields.find { it.name == fieldName }

            // if field name shadowing found
            if (subtypeField != null && supertypeField != null) {
                return if (fieldDeclaringClassType isAccessibleFrom testClassPackageName) {
                    ReceiverSuitability.RequiresTypeCast(fieldDeclaringClassType)
                } else {
                    ReceiverSuitability.ReflectionOnly
                }
            }

            // no name shadowing found
            return ReceiverSuitability.Suitable
        }

        // TODO: reduce code duplication (duplicated code is above)
        if (fieldDeclaringClassType isSubtypeOf accessorType) {
            val subtype = fieldDeclaringClassType.jClass
            val supertype = accessorType.jClass

            val subtypeField = subtype.declaredFields.find { it.name == fieldName }
            val supertypeField = supertype.declaredFields.find { it.name == fieldName }

            // if field name shadowing found
            if (subtypeField != null && supertypeField != null) {
                return if (fieldDeclaringClassType isAccessibleFrom testClassPackageName) {
                    ReceiverSuitability.RequiresTypeCast(fieldDeclaringClassType)
                } else {
                    ReceiverSuitability.ReflectionOnly
                }
            }

            // no name shadowing found
            return ReceiverSuitability.Suitable
        }

        // Accessor type is not subtype or supertype of the field's declaring class.
        // So the only remaining option to access the field is to use reflection.
        return ReceiverSuitability.ReflectionOnly

//        if (this.declaringClass)
//        when {
//            accessor.type == this.declaringClass -> ReceiverSuitability.Suitable
//            accessor.type isSubtypeOf this.declaringClass -> {
//                val subtype = accessor.type.jClass
//                val supertype = accessor.type.jClass
//                val fieldName = this.name
//
//                subtype.getDeclaredField()
//            }
//        }
    }

    /**
     * @return true if a constructor can be called with the given arguments without reflection
     */
    private infix fun ConstructorId.canBeCalledWith(args: List<CgExpression>): Boolean =
        isAccessibleFrom(testClassPackageName) && !classId.isAbstract && args canBeArgsOf this

    private fun List<CgExpression>.guardedForDirectCallOf(executable: ExecutableId): List<CgExpression> {
        val ambiguousOverloads = executable.classId
            .getAmbiguousOverloadsOf(executable)
            .filterNot { it == executable }
            .toList()

        val isEmptyAmbiguousOverloads = ambiguousOverloads.isEmpty()

        return if (isEmptyAmbiguousOverloads) this else castAmbiguousArguments(executable, this, ambiguousOverloads)
    }

    private fun castAmbiguousArguments(
        executable: ExecutableId,
        args: List<CgExpression>,
        ambiguousOverloads: List<ExecutableId>
    ): List<CgExpression> =
        args.withIndex().map { (i ,arg) ->
            val targetType = executable.parameters[i]

            // always cast nulls
            if (arg == nullLiteral()) return@map typeCast(targetType, arg)

            // in case arg type exactly equals target type, do nothing
            if (arg.type == targetType) return@map arg

            // arg type is subtype of target type
            // check other overloads for ambiguous types
            val typesInOverloadings = ambiguousOverloads.map { it.parameters[i] }
            val ancestors = typesInOverloadings.filter { arg.type.isSubtypeOf(it) }

            if (ancestors.isNotEmpty()) typeCast(targetType, arg) else arg
        }

    private fun ExecutableId.toExecutableVariable(args: List<CgExpression>): CgVariable {
        val declaringClass = statementConstructor.newVar(Class::class.id) { classId[forName](classId.name) }
        val argTypes = (args zip parameters).map { (arg, paramType) ->
            val baseName = when (arg) {
                is CgVariable -> "${arg.name}Type"
                else -> "${paramType.prettifiedName.decapitalize()}Type"
            }
            statementConstructor.newVar(classCgClassId, baseName) {
                if (paramType.isPrimitive) {
                    CgGetJavaClass(paramType)
                } else {
                    Class::class.id[forName](paramType.name)
                }
            }
        }

        return when (this) {
            is MethodId -> {
                val name = this.name + "Method"
                statementConstructor.newVar(java.lang.reflect.Method::class.id, name) {
                    declaringClass[getDeclaredMethod](this.name, *argTypes.toTypedArray())
                }
            }
            is ConstructorId -> {
                val name = this.classId.prettifiedName.decapitalize() + "Constructor"
                statementConstructor.newVar(java.lang.reflect.Constructor::class.id, name) {
                    declaringClass[getDeclaredConstructor](*argTypes.toTypedArray())
                }
            }
        }
    }

    /**
     * Receives a list of [CgExpression].
     * Transforms it into a list of [CgExpression] where:
     * - array and literal values are cast to [java.lang.Object]
     * - other values remain as they were
     *
     * @return a list of [CgExpression] where each expression can be
     * used as an argument of reflective call to a method or constructor
     */
    private fun List<CgExpression>.guardedForReflectiveCall(): List<CgExpression> =
        map {
            when {
                it is CgValue && it.type.isArray -> typeCast(objectClassId, it)
                it == nullLiteral() -> typeCast(objectClassId, it)
                else -> it
            }
        }

    private fun MethodId.callWithReflection(caller: CgExpression?, args: List<CgExpression>): CgMethodCall {
        val method = declaredExecutableRefs[this]
            ?: toExecutableVariable(args).also {
                declaredExecutableRefs = declaredExecutableRefs.put(this, it)
                +it[setAccessible](true)
            }

        val arguments = args.guardedForReflectiveCall().toTypedArray()
        val argumentsArrayVariable = convertVarargToArray(method, arguments)

        return method[invoke](caller, CgSpread(argumentsArrayVariable.type, argumentsArrayVariable))
    }

    private fun ConstructorId.callWithReflection(args: List<CgExpression>): CgExecutableCall {
        val constructor = declaredExecutableRefs[this]
            ?: this.toExecutableVariable(args).also {
                declaredExecutableRefs = declaredExecutableRefs.put(this, it)
                +it[setAccessible](true)
            }

        val arguments = args.guardedForReflectiveCall().toTypedArray()
        val argumentsArrayVariable = convertVarargToArray(constructor, arguments)

        return constructor[newInstance](argumentsArrayVariable)
    }

    private fun convertVarargToArray(reflectionCallVariable: CgVariable, arguments: Array<CgExpression>): CgVariable {
        val argumentsArrayVariable = variableConstructor.newVar(
            baseType = objectArrayClassId,
            baseName = "${reflectionCallVariable.name}Arguments"
        ) {
            CgAllocateArray(
                type = objectArrayClassId,
                elementType = objectClassId,
                size = arguments.size
            )
        }

        for ((i, argument) in arguments.withIndex()) {
            +CgAssignment(argumentsArrayVariable.at(i), argument)
        }

        return argumentsArrayVariable
    }

    /**
     * This sealed class describes different extents of suitability (or matching)
     * between an expression (in the role of a receiver) and a method or field.
     *
     * In other words, this class and its inheritors describe if a given object (receiver)
     * can be used to call a method or access a field, and if so,
     * then are there any additional actions required (like type cast).
     */
    // TODO: rename from argument to receiver (and in comments too)
    // TODO: perhaps Comparable interface is not needed here
    private sealed class ReceiverSuitability(private val priority: Int) : Comparable<ReceiverSuitability> {
        /**
         * Method can be called with a given receiver directly
         */
        object Suitable : ReceiverSuitability(SUITABLE_PRIORITY)

        /**
         * Method can be called with a given receiver, but this receiver needs to be type cast to the [targetType]
         */
        class RequiresTypeCast(val targetType: ClassId) : ReceiverSuitability(REQUIRES_TYPE_CAST_PRIORITY)

        /**
         * Method can only be called with a given receiver via reflection.
         * For example, if the receiver's type is inaccessible from the current package,
         * so we cannot declare a variable of this type or perform a type cast.
         * But there may be other cases. For example, we also cannot use
         * anonymous classes' names in the code, so the reflection may be required.
         */
        object ReflectionOnly : ReceiverSuitability(REFLECTION_PRIORITY)

        /**
         * The greater priority, the more preferable the result.
         *
         * For example, [Suitable] is more preferable than any other option,
         * because it does not require any additional actions (like type casts) and is straightforward.
         */
        override fun compareTo(other: ReceiverSuitability): Int {
            return priority.compareTo(other.priority)
        }

        companion object {
            private const val SUITABLE_PRIORITY = 3
            private const val REQUIRES_TYPE_CAST_PRIORITY = 2
            private const val REFLECTION_PRIORITY = 1
        }
    }
}