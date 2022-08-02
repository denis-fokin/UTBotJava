package org.utbot.framework.codegen.model.constructor

import org.utbot.framework.plugin.api.util.jClass
import kotlin.reflect.KClass

data class UtTestClass(
    val classUnderTest: KClass<*>,
    val methodTestSets: List<CgMethodTestSet>,
    val innerClasses: List<UtTestClass> = listOf()
) {
    companion object {
        fun fromTestSets(classUnderTest: KClass<*>, testSets: List<CgMethodTestSet>): UtTestClass {
            val class2methodTestSets = testSets.groupBy { it.executableId.classId.jClass.kotlin }
            val class2innerClasses = testSets
                .map { it.executableId.classId.jClass.kotlin }
                .flatMap { generateSequence(it) { it.java.enclosingClass.kotlin }.takeWhile { it != classUnderTest } }
                .groupBy { it.java.enclosingClass.kotlin }
                .mapValues { (_, v) -> v.distinct() }
            return buildFromMaps(classUnderTest, class2methodTestSets, class2innerClasses)
        }

        private fun buildFromMaps(clazz: KClass<*>, class2methodTestSets: Map<KClass<*>, List<CgMethodTestSet>>, class2innerClasses: Map<KClass<*>, List<KClass<*>>>): UtTestClass {
            val innerClasses = class2innerClasses.getOrDefault(clazz, listOf())
            val methodTestSets = class2methodTestSets.getOrDefault(clazz, listOf())
            return UtTestClass(
                clazz,
                methodTestSets,
                innerClasses.map { buildFromMaps(it, class2methodTestSets, class2innerClasses) })
        }
    }

    val size: Int
        get() = methodTestSets.size + innerClasses.sumOf { it.size }
}