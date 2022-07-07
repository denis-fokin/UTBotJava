package examples.parser

import alibaba.fastjson.parser.ParserConfigExample
import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.utbot.examples.DoNotCalculate
import org.utbot.framework.plugin.api.MockStrategyApi

class SummaryParserConfigTest : SummaryTestCaseGeneratorTest(
    ParserConfigExample::class
) {
    @Test
    fun testIsPrimitive2() {
        val summary1 = "Test invokes:    Class::isPrimitive once\n" +
                "throws NullPointerException in: return clazz.isPrimitive() || clazz == Boolean.class ||" +
                " clazz == Character.class || clazz == Byte.class || clazz == Short.class || clazz == Integer.class ||" +
                " clazz == Long.class || clazz == Float.class || clazz == Double.class || clazz == BigInteger.class ||" +
                " clazz == BigDecimal.class || clazz == String.class || clazz == java.util.Date.class ||" +
                " clazz == java.sql.Date.class || clazz == java.sql.Time.class || clazz == java.sql.Timestamp.class ||" +
                " clazz.isEnum();"
        val summary2 = "Test invokes:    Class::isPrimitive once\n" +
                "returns from: return clazz.isPrimitive() || clazz == Boolean.class || clazz == Character.class ||" +
                " clazz == Byte.class || clazz == Short.class || clazz == Integer.class || clazz == Long.class ||" +
                " clazz == Float.class || clazz == Double.class || clazz == BigInteger.class ||" +
                " clazz == BigDecimal.class || clazz == String.class || clazz == java.util.Date.class ||" +
                " clazz == java.sql.Date.class || clazz == java.sql.Time.class || clazz == java.sql.Timestamp.class ||" +
                " clazz.isEnum();"

        val methodName1 = "testIsPrimitive2_ClassIsPrimitive"
        val methodName2 =
            "testIsPrimitive2_ClazzIsPrimitiveOrClazzNotEqualsBooleanClassOrClazzNotEqualsCharacterClassOr" +
                    "ClazzNotEqualsByteClassOrClazzNotEqualsShortClassOrClazzNotEqualsIntegerClassOr" +
                    "ClazzNotEqualsLongClassOrClazzNotEqualsFloatClassOrClazzNotEqualsDoubleClassOr" +
                    "ClazzNotEqualsBigIntegerClassOrClazzNotEqualsBigDecimalClassOrClazzNotEqualsStringClassOr" +
                    "ClazzNotEqualsJavautilDateClassOrClazzNotEqualsJavasqlDateClassOrClazzNotEqualsJavasqlTimeClassOr" +
                    "ClazzNotEqualsJavasqlTimestampClassOrClazzIsEnum"

        val displayName1 =
            "return clazz.isPrimitive() || clazz == Boolean.class || clazz == Character.class || clazz == Byte.class ||" +
                    " clazz == Short.class || clazz == Integer.class || clazz == Long.class || clazz == Float.class ||" +
                    " clazz == Double.class || clazz == BigInteger.class || clazz == BigDecimal.class ||" +
                    " clazz == String.class || clazz == java.util.Date.class || clazz == java.sql.Date.class ||" +
                    " clazz == java.sql.Time.class || clazz == java.sql.Timestamp.class ||" +
                    " clazz.isEnum() : True -> ThrowNullPointerException"
        val displayName2 =
            "ClassIsPrimitive -> return clazz.isPrimitive() || clazz == Boolean.class || clazz == Character.class ||" +
                    " clazz == Byte.class || clazz == Short.class || clazz == Integer.class || clazz == Long.class ||" +
                    " clazz == Float.class || clazz == Double.class || clazz == BigInteger.class ||" +
                    " clazz == BigDecimal.class || clazz == String.class || clazz == java.util.Date.class ||" +
                    " clazz == java.sql.Date.class || clazz == java.sql.Time.class ||" +
                    " clazz == java.sql.Timestamp.class || clazz.isEnum()"

        val summaryKeys = listOf(
            summary1,
            summary2
        )

        val displayNames = listOf(
            displayName1,
            displayName2
        )

        val methodNames = listOf(
            methodName1,
            methodName2
        )

        val method = ParserConfigExample::isPrimitive2
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        check(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

}