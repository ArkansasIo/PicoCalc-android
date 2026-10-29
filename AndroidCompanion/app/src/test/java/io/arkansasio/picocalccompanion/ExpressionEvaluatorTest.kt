package io.arkansasio.picocalccompanion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ExpressionEvaluatorTest {
 @Test fun precedence() { assertEquals(14.0, ExpressionEvaluator.evaluate("2 + 3 * 4"), 0.000001) }
 @Test fun parenthesesAndUnary() { assertEquals(-5.0, ExpressionEvaluator.evaluate("-(2 + 3)"), 0.000001) }
 @Test fun decimalAndExponent() { assertEquals(125.5, ExpressionEvaluator.evaluate("1.25e2 + .5"), 0.000001) }
 @Test fun divisionByZeroRejected() { assertThrows(IllegalArgumentException::class.java) { ExpressionEvaluator.evaluate("9 / 0") } }
 @Test fun trailingInputRejected() { assertThrows(IllegalArgumentException::class.java) { ExpressionEvaluator.evaluate("2 + 2 xyz") } }
}
