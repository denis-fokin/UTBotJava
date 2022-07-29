package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.*
import org.utbot.fuzzer.*
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue
import org.utbot.fuzzer.providers.ConstantsModelProvider.fuzzed
import org.utbot.jcdb.api.Accessible
import org.utbot.jcdb.api.ClassId
import org.utbot.jcdb.api.FieldId
import org.utbot.jcdb.api.MethodId
import org.utbot.jcdb.api.isPrimitive
import java.util.function.IntSupplier

/**
 * Creates [UtAssembleModel] for objects which have public constructors with primitives types and String as parameters.
 */
class ObjectModelProvider : ModelProvider {

    var modelProvider: ModelProvider
    var limitValuesCreatedByFieldAccessors: Int = 100
        set(value) {
            field = maxOf(0, value)
        }

    private val idGenerator: IntSupplier
    private val recursion: Int
    private val limit: Int

    private val nonRecursiveModelProvider: ModelProvider
        get() {
            val modelProviderWithoutRecursion = modelProvider.exceptIsInstance<ObjectModelProvider>()
            return if (recursion > 0) {
                ObjectModelProvider(idGenerator, limit = 1, recursion - 1).with(modelProviderWithoutRecursion)
            } else {
                modelProviderWithoutRecursion.withFallback(NullModelProvider)
            }
        }

    constructor(idGenerator: IntSupplier) : this(idGenerator, Int.MAX_VALUE)

    constructor(idGenerator: IntSupplier, limit: Int) : this(idGenerator, limit, 1)

    private constructor(idGenerator: IntSupplier, limit: Int, recursion: Int) {
        this.idGenerator = idGenerator
        this.recursion = recursion
        this.limit = limit
        this.modelProvider = objectModelProviders(idGenerator)
    }

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        val fuzzedValues = with(description) {
            parameters.asSequence()
                .filterNot { it == stringClassId || it.isPrimitiveWrapper }
                .flatMap { classId ->
                    collectConstructors(classId) { javaConstructor ->
                        isAccessible(javaConstructor, classId, description.packageName)
                    }.sortedWith(
                        primitiveParameterizedConstructorsFirstAndThenByParameterCount
                    ).take(limit)
                }
                .associateWith { constructorId ->
                    fuzzParameters(
                        constructorId,
                        nonRecursiveModelProvider
                    )
                }
                .flatMap { (constructorId, fuzzedParameters) ->
                    if (constructorId.parameters.isEmpty()) {
                        sequenceOf(assembleModel(idGenerator.asInt, constructorId, emptyList())) +
                                generateModelsWithFieldsInitialization(constructorId, description, concreteValues)
                    }
                    else {
                        fuzzedParameters.map { params ->
                            assembleModel(idGenerator.asInt, constructorId, params)
                        }
                    }
                }
        }

        fuzzedValues.forEach { fuzzedValue ->
            description.parametersMap[fuzzedValue.model.classId]?.forEach { index ->
                yieldValue(index, fuzzedValue)
            }
        }
    }

    private fun generateModelsWithFieldsInitialization(constructorId: ConstructorExecutableId, description: FuzzedMethodDescription, concreteValues: Collection<FuzzedConcreteValue>): Sequence<FuzzedValue> {
        if (limitValuesCreatedByFieldAccessors == 0) return emptySequence()
        val fields = findSuitableFields(constructorId.classId, description)
        val syntheticClassFieldsSetterMethodDescription = FuzzedMethodDescription(
            "${constructorId.classId.simpleName}<syntheticClassFieldSetter>",
            voidClassId,
            fields.map { it.classId },
            concreteValues
        ).apply {
            packageName = description.packageName
        }

        return fuzz(syntheticClassFieldsSetterMethodDescription, nonRecursiveModelProvider)
            .take(limitValuesCreatedByFieldAccessors) // limit the number of fuzzed values in this particular case
            .map { fieldValues ->
                val fuzzedModel = assembleModel(idGenerator.asInt, constructorId, emptyList())
                val assembleModel = fuzzedModel.model as? UtAssembleModel ?: error("Expected UtAssembleModel but ${fuzzedModel.model::class.java} found")
                val modificationChain = assembleModel.modificationsChain as? MutableList ?: error("Modification chain must be mutable")
                fieldValues.asSequence().mapIndexedNotNull { index, value ->
                    val field = fields[index]
                    when {
                        field.canBeSetDirectly -> UtDirectSetFieldModel(
                            fuzzedModel.model,
                            constructorId.classId.findField(field.name),
                            value.model
                        )
                        field.setter != null -> UtExecutableCallModel(
                            fuzzedModel.model,
                            methodId(constructorId.classId, field.setter.name, field.setter.returnType, field.classId).asExecutable(),
                            listOf(value.model)
                        )
                        else -> null
                    }
                }.forEach(modificationChain::add)
                fuzzedModel
            }
    }

    companion object {
        private fun collectConstructors(classId: ClassId, predicate: (ConstructorExecutableId) -> Boolean): Sequence<ConstructorExecutableId> {
            return classId.allConstructors
                .asSequence()
                .filter(predicate)
        }

        private fun isAccessible(member: Accessible, declaringClass: ClassId, packageName: String?): Boolean {
            return member.isPublic ||
                    (packageName != null && isPackagePrivate(member) && declaringClass.packageName == packageName)
        }

        private fun isPackagePrivate(member: Accessible): Boolean {
            val hasAnyAccessModifier = member.isPrivate
                    || member.isProtected
                    || member.isPublic
            return !hasAnyAccessModifier
        }

        private fun FuzzedMethodDescription.fuzzParameters(constructorId: ConstructorExecutableId, vararg modelProviders: ModelProvider): Sequence<List<FuzzedValue>> {
            val fuzzedMethod = FuzzedMethodDescription(
                executableId = constructorId,
                concreteValues = this.concreteValues
            ).apply {
                this.packageName = this@fuzzParameters.packageName
            }
            return fuzz(fuzzedMethod, *modelProviders)
        }

        private fun assembleModel(id: Int, constructorId: ConstructorExecutableId, params: List<FuzzedValue>): FuzzedValue {
            val instantiationChain = mutableListOf<UtStatementModel>()
            return UtAssembleModel(
                id,
                constructorId.classId,
                "${constructorId.classId.name}${constructorId.parameters}#" + id.toString(16),
                instantiationChain = instantiationChain,
                modificationsChain = mutableListOf()
            ).apply {
                instantiationChain += UtExecutableCallModel(null, constructorId, params.map { it.model }, this)
            }.fuzzed {
                summary = "%var% = ${constructorId.classId.simpleName}(${constructorId.parameters.joinToString { it.simpleName }})"
            }
        }

        private fun findSuitableFields(classId: ClassId, description: FuzzedMethodDescription): List<FieldDescription>  {
            return classId.fields.map { field ->
                FieldDescription(
                    field.name,
                    field.type,
                    isAccessible(field, classId, description.packageName) && field.isFinal && field.isStatic,
                    classId.findPublicSetterIfHasPublicGetter(field, description)
                )
            }
        }

        private fun ClassId.findPublicSetterIfHasPublicGetter(field: FieldId, description: FuzzedMethodDescription): MethodId? {
            val postfixName = field.name.capitalize()
            val setterName = "set$postfixName"
            val getterName = "get$postfixName"
            val getter = methods.firstOrNull { it.name == getterName } ?: return null
            return if (isAccessible(getter, this, description.packageName) && getter.returnType == field.type) {
                methods.find {
                    isAccessible(it, this, description.packageName) &&
                            it.name == setterName &&
                            it.parameters.size == 1 &&
                            it.parameters[0] == field.type
                }
            } else {
                null
            }
        }

        private val primitiveParameterizedConstructorsFirstAndThenByParameterCount =
            compareByDescending<ConstructorExecutableId> { constructorId ->
                constructorId.parameters.all { classId ->
                    classId.isPrimitive || classId == stringClassId
                }
            }.thenComparingInt { constructorId ->
                constructorId.parameters.size
            }

        private class FieldDescription(
            val name: String,
            val classId: ClassId,
            val canBeSetDirectly: Boolean,
            val setter: MethodId?,
        )
    }
}
