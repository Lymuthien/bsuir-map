package com.example.lab1

import com.example.lab1.calculator.CalculatorEngine
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun calculator_add_mul_precedence() {
        val e = CalculatorEngine()
        "2+3*4".forEach {
            when (it) {
                '+', '-', '*', '/' -> e.inputOperator(it)
                else -> e.inputDigit(it)
            }
        }
        e.equals()
        assertEquals("14", e.state().display)
    }

    @Test
    fun calculator_decimal_and_division() {
        val e = CalculatorEngine()
        e.inputDigit('1')
        e.inputDecimalPoint()
        e.inputDigit('5')
        e.inputOperator('/')
        e.inputDigit('2')
        e.equals()
        assertEquals("0.75", e.state().display)
    }

    @Test
    fun calculator_divide_by_zero_is_error() {
        val e = CalculatorEngine()
        e.inputDigit('7')
        e.inputOperator('/')
        e.inputDigit('0')
        e.equals()
        assertEquals("Error", e.state().display)
        assertTrue(e.state().isError)
    }
}