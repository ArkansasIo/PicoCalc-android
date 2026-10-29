package io.arkansasio.picocalc;

/** Recursive-descent expression evaluator for basic arithmetic and exponentiation. */
public final class ExpressionEvaluator {
    private final String input;
    private int pos;
    private ExpressionEvaluator(String input) { this.input = input.replace(" ", ""); }
    public static double evaluate(String expression) {
        ExpressionEvaluator p = new ExpressionEvaluator(expression);
        double value = p.expression();
        if (p.pos != p.input.length()) throw new IllegalArgumentException("Unexpected input at " + p.pos);
        if (!Double.isFinite(value)) throw new ArithmeticException("Result is not finite");
        return value;
    }
    private boolean eat(char c) { if (pos < input.length() && input.charAt(pos) == c) { pos++; return true; } return false; }
    private double expression() {
        double x = term();
        while (true) { if (eat('+')) x += term(); else if (eat('-')) x -= term(); else return x; }
    }
    private double term() {
        double x = power();
        while (true) { if (eat('*')) x *= power(); else if (eat('/')) { double d = power(); if (d == 0) throw new ArithmeticException("Division by zero"); x /= d; } else return x; }
    }
    private double power() { double x = unary(); if (eat('^')) x = Math.pow(x, power()); return x; }
    private double unary() { if (eat('+')) return unary(); if (eat('-')) return -unary(); return atom(); }
    private double atom() {
        if (eat('(')) { double x = expression(); if (!eat(')')) throw new IllegalArgumentException("Missing )"); return x; }
        int start = pos;
        while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos)=='.')) pos++;
        if (start == pos) throw new IllegalArgumentException("Expected number at " + pos);
        try { return Double.parseDouble(input.substring(start, pos)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid number", e); }
    }
}
