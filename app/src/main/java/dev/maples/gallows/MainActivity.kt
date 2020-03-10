package dev.maples.gallows

import android.os.Bundle
import android.text.InputType
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import dev.maples.gallows.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.InputStreamReader

private const val DICTIONARY_NAME = "dictionary"
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var words: MutableList<String> = mutableListOf()
    private var numGuesses = 0
    private var wordLength = 0
    private var usedGuesses = emptyList<Char>().toMutableList()

    private val inputHandler = TextView.OnEditorActionListener { view, _, _ ->
        if (numGuesses == 0 && wordLength == 0) getNumGuesses(view.text.toString())
        else if (wordLength == 0) getWordLength(view.text.toString())
        else handleGuess(view.text.first())
        view.text = null
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.userInput.setOnEditorActionListener(inputHandler)

        // Setup the game
        binding.output = "How many guesses would you like?"
        setupGame()
    }

    private fun setupGame() {
        numGuesses = 0
        wordLength = 0
        binding.pattern = ""
        val reader = BufferedReader(InputStreamReader(assets.open(DICTIONARY_NAME)))
        reader.forEachLine { words.add(it) }
    }

    private fun getNumGuesses(input: String) {
        if (input.toInt() > 0) {
            numGuesses = input.toInt()
            binding.output= "How long would you like the word to be?"
        } else binding.output = "Invalid number of guess. Please enter a number 1-9"
    }

    private fun getWordLength(input: String) {
        if (input.toInt() > 1) {
            wordLength = input.toInt()
            words.removeIf { it.length != wordLength }

            // Check if they picked a valid length
            if (words.size == 0) {
                binding.output = "I don't know any words of that length. Could you try again?"
                wordLength = 0
            }
            binding.output = "Guess a letter! Guesses Remaining: $numGuesses"
            binding.userInput.inputType = InputType.TYPE_CLASS_TEXT
            for (i in 0 until wordLength) {
                binding.pattern += "_"
            }
        } else binding.output = "Invalid word length. Please enter a number greater than 1"
    }

    private fun handleGuess(input: Char) {
        // Validate guess
        if (!input.isLetter()) {
            binding.output = "That's not a letter! Guesses Remaining: $numGuesses"
            return
        }
        if (input.isLetter() && usedGuesses.contains(input)) {
            binding.output = "You already guessed that! Guesses Remaining: $numGuesses"
            return
        }

        // Calculate possible words
        val families = getWordFamily(input)
        val largestFamily = families.keys.maxBy { families[it]!!.size }!!
        words = families[largestFamily]!!

        // Check their guess
        val newPattern = updatePattern(largestFamily)
        if (newPattern == binding.pattern) {
            binding.output = "Wrong, try again! Guesses Remaining: ${--numGuesses}"
        } else if (newPattern.contains('_')) {
            binding.output = "Correct! Guesses Remaining: $numGuesses"
        } else {
            binding.output = "You win! Enter a new number of guesses to play again"
            setupGame()
        }

        // Lose if no guesses remaining
        if (numGuesses == 0 && newPattern.contains('_')) {
            binding.output = "You lose! The word was \"${words.first()}\" Enter a new number of guesses to play again"
            setupGame()
        }

        // Update necessary values
        usedGuesses.add(input)
        binding.pattern = newPattern
    }

    private fun getPattern(word: String, guessedLetter: Char): String {
        val regExp = "[^$guessedLetter]"
        return word.replace(regExp.toRegex(), "_")
    }

    private fun getWordFamily(guessedLetter: Char): MutableMap<String, MutableList<String>> {
        val family: MutableMap<String, MutableList<String>> = mutableMapOf()
        var currentSet: MutableList<String>
        for (word in words) {
            val pattern = getPattern(word, guessedLetter)
            currentSet = if (family.containsKey(pattern)) family[pattern] ?: mutableListOf()
                else mutableListOf()
            currentSet.add(word)
            family[pattern] = currentSet
        }
        return family
    }

    private fun updatePattern(key: String): String {
        var index = 0
        var newPattern = ""
        key.forEach {
            newPattern += if (it != '_' ) it else (binding.pattern!!.elementAt(index))
            index++
        }
        return newPattern
    }
}
