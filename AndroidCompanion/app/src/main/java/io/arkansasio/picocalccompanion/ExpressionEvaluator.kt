package io.arkansasio.picocalccompanion

/** Safe arithmetic parser: no dynamic evaluation, reflection, or code execution. */
object ExpressionEvaluator {
 fun evaluate(input: String): Double {
  val p = Parser(input)
  val result = p.expression()
  p.skip()
  require(p.end()) { "Unexpected input at position ${p.pos}" }
  require(result.isFinite()) { "Result is not finite" }
  return result
 }
 private class Parser(private val s: String) {
  var pos = 0
  fun end() = pos >= s.length
  fun skip() { while (!end() && s[pos].isWhitespace()) pos++ }
  private fun take(c: Char): Boolean { if (!end() && s[pos] == c) { pos++; return true }; return false }
  fun expression(): Double {
   var n = term()
   while (true) { skip(); n = when { take('+') -> n + term(); take('-') -> n - term(); else -> return n } }
  }
  private fun term(): Double {
   var n = factor()
   while (true) {
    skip()
    n = when {
     take('*') -> n * factor()
     take('/') -> { val d = factor(); require(d != 0.0) { "Division by zero" }; n / d }
     take('%') -> { val d = factor(); require(d != 0.0) { "Modulo by zero" }; n % d }
     else -> return n
    }
   }
  }
  private fun factor(): Double {
   skip()
   if (take('+')) return factor()
   if (take('-')) return -factor()
   if (take('(')) { val n = expression(); skip(); require(take(')')) { "Missing closing parenthesis" }; return n }
   val start = pos
   var digit = false
   while (!end() && s[pos].isDigit()) { pos++; digit = true }
   if (!end() && s[pos] == '.') { pos++; while (!end() && s[pos].isDigit()) { pos++; digit = true } }
   require(digit) { "Expected a number at position $pos" }
   if (!end() && (s[pos] == 'e' || s[pos] == 'E')) {
    pos++; if (!end() && (s[pos] == '+' || s[pos] == '-')) pos++
    val e = pos; while (!end() && s[pos].isDigit()) pos++
    require(pos > e) { "Invalid exponent" }
   }
   return s.substring(start, pos).toDouble()
  }
 }
}
