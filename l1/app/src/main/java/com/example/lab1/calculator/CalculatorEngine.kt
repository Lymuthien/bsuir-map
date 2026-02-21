package com.example.lab1.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode


class CalculatorEngine {
    data class UiState(
        val expression: String,
        val display: String,
        val isError: Boolean,
    )

    private val expr = StringBuilder()

    private var display: String = "0"
    private var isError: Boolean = false

    private val mc = MathContext(18, RoundingMode.HALF_UP)

    fun state(): UiState = UiState(
        expression = expr.toString(),
        display = display,
        isError = isError,
    )

    fun clear() {
        expr.clear()
        display = "0"
        isError = false
    }

    fun deleteLast() {
        if (isError) {
            clear()
            return
        }
        if (expr.isNotEmpty()) {
            expr.deleteCharAt(expr.length - 1)
        }
        recomputePreview()
    }

    fun inputDigit(d: Char) {
        require(d in '0'..'9')
        if (isError) clear()
        expr.append(d)
        recomputePreview()
    }

    fun inputDecimalPoint() {
        if (isError) clear()
        // Add "0." if the dot is pressed immediately after the operator/at the beginning
        val last = expr.lastOrNull()
        if (last == null || last in "+-*/") {
            expr.append("0.")
            recomputePreview()
            return
        }
        // Do not allow two dots in one number
        val currentNumber = tailNumber()
        if (!currentNumber.contains('.')) {
            expr.append('.')
        }
        recomputePreview()
    }

    fun inputOperator(op: Char) {
        require(op in charArrayOf('+', '-', '*', '/'))
        if (isError) clear()
        if (expr.isEmpty()) {
            // Allow the expression to begin with a minus
            if (op == '-') {
                expr.append(op)
            }
            recomputePreview()
            return
        }
        val last = expr.last()
        if (last in "+-*/") {
            expr.setCharAt(expr.length - 1, op)
        } else {
            expr.append(op)
        }
        recomputePreview()
    }

    fun toggleSign() {
        if (isError) clear()
        if (expr.isEmpty()) {
            expr.append('-')
            recomputePreview()
            return
        }

        val (startIdx, number) = tailNumberWithStart()
        if (number.isEmpty()) {
            if (expr.last() in "+-*/") {
                expr.append('-')
            }
            recomputePreview()
            return
        }

        // If there is already a unary minus before the number, remove it, otherwise add it
        val minusIdx = startIdx - 1
        val canBeUnaryMinus = minusIdx >= 0 && (minusIdx == 0 || expr[minusIdx - 1] in "+-*/") && expr[minusIdx] == '-'
        if (canBeUnaryMinus) {
            expr.deleteCharAt(minusIdx)
        } else {
            expr.insert(startIdx, '-')
        }
        recomputePreview()
    }


    fun equals() {
        if (isError) {
            clear()
            return
        }
        val expression = expr.toString()
        val result = evaluateOrNull(expression)
        if (result == null) {
            setError()
            return
        }
        val normalized = normalize(result)
        expr.clear()
        expr.append(normalized)
        display = normalized
        isError = false
    }

    private fun setError() {
        display = "Error"
        isError = true
    }

    private fun recomputePreview() {
        if (expr.isEmpty()) {
            display = "0"
            isError = false
            return
        }

        // If the expression ends with an operator, we simply show the tail
        val s = expr.toString()
        if (s.last() in "+-*/") {
            display = s
            isError = false
            return
        }

        val result = evaluateOrNull(s)
        if (result == null) {
            display = s
            isError = false
            return
        }
        display = normalize(result)
        isError = false
    }

    private fun normalize(value: BigDecimal): String {
        val stripped = value.stripTrailingZeros()
        val normalized = if (stripped.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO else stripped
        return normalized.toPlainString()
    }

    private fun tailNumber(): String = tailNumberWithStart().second

    private fun tailNumberWithStart(): Pair<Int, String> {
        if (expr.isEmpty()) return 0 to ""
        var i = expr.length - 1
        while (i >= 0 && (expr[i].isDigit() || expr[i] == '.')) i--
        val start = i + 1
        return start to expr.substring(start)
    }

    private fun evaluateOrNull(expression: String): BigDecimal? {
        return try {
            val tokens = tokenize(expression) ?: return null
            val rpn = toRpn(tokens)
            evalRpn(rpn)
        } catch (_: ArithmeticException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private sealed interface Token {
        data class Num(val v: BigDecimal) : Token
        data class Op(val c: Char) : Token
    }

    private fun tokenize(s: String): List<Token>? {
        val out = mutableListOf<Token>()
        var i = 0
        fun prevIsOpOrStart(): Boolean {
            if (out.isEmpty()) return true
            return out.last() is Token.Op
        }

        while (i < s.length) {
            val ch = s[i]
            when {
                ch.isWhitespace() -> i++
                ch.isDigit() || ch == '.' || (ch == '-' && prevIsOpOrStart()) -> {
                    val start = i
                    i++
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                    val numStr = s.substring(start, i)
                    val normalized = when (numStr) {
                        "." -> "0."
                        "-." -> "-0."
                        else -> numStr
                    }
                    val v = normalized.toBigDecimalOrNull() ?: return null
                    out.add(Token.Num(v))
                }
                ch in "+*/" || ch == '-' -> {
                    out.add(Token.Op(ch))
                    i++
                }
                else -> return null
            }
        }

        if (out.lastOrNull() is Token.Op) return null
        return out
    }

    private fun precedence(op: Char): Int = when (op) {
        '*', '/' -> 2
        '+', '-' -> 1
        else -> 0
    }

    private fun toRpn(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val ops = ArrayDeque<Char>()
        for (t in tokens) {
            when (t) {
                is Token.Num -> output.add(t)
                is Token.Op -> {
                    val o1 = t.c
                    while (ops.isNotEmpty()) {
                        val o2 = ops.last()
                        if (precedence(o2) >= precedence(o1)) {
                            output.add(Token.Op(ops.removeLast()))
                        } else break
                    }
                    ops.addLast(o1)
                }
            }
        }
        while (ops.isNotEmpty()) {
            output.add(Token.Op(ops.removeLast()))
        }
        return output
    }

    private fun evalRpn(rpn: List<Token>): BigDecimal? {
        val stack = ArrayDeque<BigDecimal>()
        for (t in rpn) {
            when (t) {
                is Token.Num -> stack.addLast(t.v)
                is Token.Op -> {
                    if (stack.size < 2) return null
                    val b = stack.removeLast()
                    val a = stack.removeLast()
                    val res = when (t.c) {
                        '+' -> a.add(b, mc)
                        '-' -> a.subtract(b, mc)
                        '*' -> a.multiply(b, mc)
                        '/' -> {
                            if (b.compareTo(BigDecimal.ZERO) == 0) return null
                            a.divide(b, 12, RoundingMode.HALF_UP)
                        }
                        else -> return null
                    }
                    stack.addLast(res)
                }
            }
        }
        return if (stack.size == 1) stack.last() else null
    }
}

