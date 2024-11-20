package com.example.budgetapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DetailedActivity : AppCompatActivity() {
    private lateinit var transaction: Transaction
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detailed)
        val updateBtn = findViewById<Button>(R.id.update_button)
        val label = findViewById<EditText>(R.id.labelInputLayout)
        val amount = findViewById<EditText>(R.id.amountInputLayout)
        val description = findViewById<EditText>(R.id.descInputLayout)
        val date = findViewById<EditText>(R.id.dateInputLayout)
        val closeBtn = findViewById<Button>(R.id.close_button)

        transaction = intent.getSerializableExtra("transaction") as Transaction

        label.setText(transaction.label)
        amount.setText(transaction.amount.toString())
        description.setText(transaction.description)
        date.setText(transaction.date)

        updateBtn.setOnClickListener {
            val labelText = label.text.toString()
            val amountText = amount.text.toString().toDoubleOrNull()
            val descriptionText = description.text.toString()
            val dateText = date.text.toString()

            if (labelText.isEmpty()) {
                label.error = "Please enter a valid label"
            }

            else if (amountText == null) {
                amount.error = "Please enter a valid amount"
            }

            else if (dateText.isEmpty() || !dateText.matches(Regex("\\d{4}/\\d{2}/\\d{2}"))) {
                date.error = "Please enter a valid date"
            }

            else {
                val transaction = Transaction(transaction.id, labelText, amountText, descriptionText, dateText)
                update(transaction)

                label.text.clear()
                amount.text.clear()
                description.text.clear()
                date.text.clear()
                Toast.makeText(this, "Transaction updated", Toast.LENGTH_SHORT).show()
            }

        }

        closeBtn.setOnClickListener {
            finish()
        }
    }

    private fun update(transaction: Transaction) {
        val db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "transactions"
        ).build()

        GlobalScope.launch(Dispatchers.IO) {
            db.transactionDao().update(transaction)
            finish()
        }
    }
}