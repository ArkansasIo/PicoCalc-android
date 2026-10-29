package io.arkansasio.picocalc;
import org.junit.Test;
import static org.junit.Assert.*;
public class ExpressionEvaluatorTest {
    @Test public void arithmeticPrecedence() { assertEquals(14.0, ExpressionEvaluator.evaluate("2+3*4"), 1e-9); }
    @Test public void parenthesesAndPower() { assertEquals(256.0, ExpressionEvaluator.evaluate("(2+2)^4"), 1e-9); }
    @Test public void unaryMinus() { assertEquals(-5.0, ExpressionEvaluator.evaluate("-2-3"), 1e-9); }
    @Test(expected=ArithmeticException.class) public void divisionByZeroRejected() { ExpressionEvaluator.evaluate("1/0"); }
    @Test(expected=IllegalArgumentException.class) public void trailingInputRejected() { ExpressionEvaluator.evaluate("2+2abc"); }
}
