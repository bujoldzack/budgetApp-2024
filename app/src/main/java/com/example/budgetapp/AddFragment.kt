package com.example.budgetapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AddFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add, container, false)
        val addBtn = view.findViewById<Button>(R.id.add_button)
        val label = view.findViewById<EditText>(R.id.labelInputLayout)
        val amount = view.findViewById<EditText>(R.id.amountInputLayout)
        val description = view.findViewById<EditText>(R.id.descInputLayout)
        val date = view.findViewById<EditText>(R.id.dateInputLayout)

        addBtn.setOnClickListener {
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
                val transaction = Transaction(0, labelText, amountText, descriptionText, dateText)
                insert(transaction)

                label.text.clear()
                amount.text.clear()
                description.text.clear()
                date.text.clear()
                Toast.makeText(activity, "Transaction added", Toast.LENGTH_SHORT).show()
            }

        }


        return view
    }

    private fun insert(transaction: Transaction) {
        val db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java,
            "transactions"
        ).build()

        GlobalScope.launch(Dispatchers.IO) {
            db.transactionDao().insertAll(transaction)
        }
    }
}