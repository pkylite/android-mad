package com.example.calculator

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.example.calculator.databinding.ActivityMainBinding
import org.mariuszgromada.math.mxparser.Expression
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        setupCalculatorButtons()
    }

    private fun setupCalculatorButtons() {
        // Number buttons
        listOf(
            binding.button0, binding.button1, binding.button2, binding.button3,
            binding.button4, binding.button5, binding.button6, binding.button7,
            binding.button8, binding.button9
        ).forEachIndexed { index, button ->
            button.setOnClickListener {
                binding.input.text = addToInputText(index.toString())
                showResult()
            }
        }

        // Operators and other buttons
        binding.buttonCroxx.setOnClickListener {
            binding.input.text = ""
            binding.output.text = ""
        }

        binding.buttonBracket.setOnClickListener {
            binding.input.text = addToInputText("(")
            showResult()
        }
        binding.buttonBracketR.setOnClickListener {
            binding.input.text = addToInputText(")")
            showResult()
        }
        binding.buttonClear.setOnClickListener {
            val currentText = binding.input.text.toString()
            if (currentText.isNotEmpty()) {
                binding.input.text = currentText.dropLast(1)
            }
            showResult()
        }
        binding.buttonDot.setOnClickListener {
            binding.input.text = addToInputText(".")
            showResult()
        }
        binding.buttonDivision.setOnClickListener {
            binding.input.text = addToInputText("÷")
            showResult()
        }
        binding.buttonMultiply.setOnClickListener {
            binding.input.text = addToInputText("×")
            showResult()
        }
        binding.buttonSubtraction.setOnClickListener {
            binding.input.text = addToInputText("-")
            showResult()
        }
        binding.buttonAddition.setOnClickListener {
            binding.input.text = addToInputText("+")
            showResult()
        }
        binding.buttonEquals.setOnClickListener {
            finalizeResult()
        }
    }

    private fun finalizeResult() {
        try {
            val expression = getInputExpression()
            val result = Expression(expression).calculate()
            if (result.isNaN()) {
                binding.output.text = getString(R.string.invalid_input)
                binding.output.setTextColor(ContextCompat.getColor(this, R.color.red))
            } else {
                binding.input.text = DecimalFormat("0.######").format(result).toString()
                binding.output.text = ""
            }
        } catch (e: Exception) {
            binding.output.text = getString(R.string.error_msg, e.message)
            binding.output.setTextColor(ContextCompat.getColor(this, R.color.red))
        }
    }

    private fun addToInputText(buttonValue: String): String {
        val currentText = binding.input.text.toString()
        if (buttonValue == "(" && currentText.isNotEmpty() && currentText.last().isDigit()) {
            // If last character is a number and the new input is an opening parenthesis
            return "$currentText×("
        } else if (buttonValue.first()
                .isDigit() && currentText.isNotEmpty() && currentText.last() == ')'
        ) {
            // If the new input starts with a digit and the last character is a closing parenthesis
            return "$currentText×$buttonValue"
        }
        return currentText + buttonValue
    }


    private fun getInputExpression(): String =
        binding.input.text.toString()
            .replace("÷", "/")
            .replace("×", "*")

    private fun showResult() {
        try {
            val expression = getInputExpression()
            if (expression.isEmpty()) {
                binding.output.text = ""
                return
            }
            val result = Expression(expression).calculate()
            if (!result.isNaN()) {
                binding.output.text = DecimalFormat("0.######").format(result).toString()
                binding.output.setTextColor(ContextCompat.getColor(this, R.color.green))
            } else {
                binding.output.text = ""
            }
        } catch (_: Exception) {
            binding.output.text = ""
        }
    }

    fun openWebsite(view: View? = null) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"))
        startActivity(intent)
    }
}
