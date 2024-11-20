package com.example.budgetapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnalyticsFragment : Fragment() {

    private lateinit var transactions: List<Transaction>
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var deleteTransaction: Transaction

    private lateinit var totalIncomeText: TextView
    private lateinit var totalExpenseText: TextView
    private lateinit var emailButton: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var balanceText: TextView
    private lateinit var yearSpinner: Spinner
    private lateinit var monthSpinner: Spinner
    private lateinit var db: AppDatabase
    private lateinit var recyclerview: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_analytics, container, false)

        totalIncomeText = view.findViewById(R.id.income)
        totalExpenseText = view.findViewById(R.id.expense)
        emailButton = view.findViewById(R.id.email_button)
        balanceText = view.findViewById(R.id.balance)
        yearSpinner = view.findViewById(R.id.yearSpinner)
        monthSpinner = view.findViewById(R.id.monthSpinner)
        recyclerview = view.findViewById(R.id.transactions_list)

        db = Room.databaseBuilder(
            requireActivity().applicationContext,
            AppDatabase::class.java,
            "transactions"
        ).build()

        transactions = arrayListOf()
        transactionAdapter = TransactionAdapter(transactions)
        linearLayoutManager = LinearLayoutManager(activity)

        recyclerview.apply {
            adapter = transactionAdapter
            layoutManager = linearLayoutManager
        }

        val itemTouchHelper = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                deleteTransaction(transactions[viewHolder.adapterPosition])
                Toast.makeText(activity, "Transaction deleted", Toast.LENGTH_SHORT).show()
            }
        }

        val swipeHelper = ItemTouchHelper(itemTouchHelper)
        swipeHelper.attachToRecyclerView(recyclerview)

        val years = listOf("2024")
        val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )

        yearSpinner.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)
        monthSpinner.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)

        val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                updateAnalytics()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        yearSpinner.onItemSelectedListener = spinnerListener
        monthSpinner.onItemSelectedListener = spinnerListener

        emailButton.setOnClickListener {
            val selectedYear = yearSpinner.selectedItem.toString()
            val selectedMonthIndex = monthSpinner.selectedItemPosition + 1
            val formattedMonth = if (selectedMonthIndex < 10) "0$selectedMonthIndex" else "$selectedMonthIndex"

            lifecycleScope.launch(Dispatchers.IO) {
                val filteredTransactions =
                    db.transactionDao().getTransactionsForYearAndMonth(selectedYear, formattedMonth)

                val emailBody = buildEmailBody(filteredTransactions)

                withContext(Dispatchers.Main) {
                    sendEmail(emailBody)
                }
            }
        }

        return view
    }

    private fun updateAnalytics() {
        val selectedYear = yearSpinner.selectedItem.toString()
        val selectedMonthIndex = monthSpinner.selectedItemPosition + 1 // Convert to 1-based index
        val formattedMonth = if (selectedMonthIndex < 10) "0$selectedMonthIndex" else "$selectedMonthIndex" // Format to "01", "02", etc.

        lifecycleScope.launch(Dispatchers.IO) {
            val filteredTransactions =
                db.transactionDao().getTransactionsForYearAndMonth(selectedYear, formattedMonth)

            val totalAmount = filteredTransactions.sumOf { it.amount }
            val incomeAmount = filteredTransactions.filter { it.amount > 0 }.sumOf { it.amount }
            val expenseAmount = totalAmount - incomeAmount

            withContext(Dispatchers.Main) {
                balanceText.text = "$%.2f".format(totalAmount)
                totalIncomeText.text = "$%.2f".format(incomeAmount)
                totalExpenseText.text = "$%.2f".format(-expenseAmount)

                // Update RecyclerView
                transactions = filteredTransactions
                transactionAdapter.setData(transactions)
            }
        }
    }

    private fun deleteTransaction(transaction: Transaction) {
        deleteTransaction = transaction

        lifecycleScope.launch(Dispatchers.IO) {
            db.transactionDao().delete(transaction)
            transactions = transactions.filter { it.id != transaction.id }

            withContext(Dispatchers.Main) {
                updateAnalytics()
            }
        }
    }

    private fun buildEmailBody(transactions: List<Transaction>): String {
        val sb = StringBuilder()
        sb.append("Transaction Report for ${monthSpinner.selectedItem} ${yearSpinner.selectedItem}\n\n")

        transactions.forEach { transaction ->
            sb.append("Title: ${transaction.label}\n")
            sb.append("Date: ${transaction.date}\n")
            sb.append("Amount: ${transaction.amount}\n")
            sb.append("Description: ${transaction.description}\n")
            sb.append("\n")
        }

        sb.append("\nTotal Transactions: ${transactions.size}")
        return sb.toString()
    }

    private fun sendEmail(emailBody: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "message/rfc822"
        intent.putExtra(Intent.EXTRA_SUBJECT, "Transaction Report")
        intent.putExtra(Intent.EXTRA_TEXT, emailBody)

        try {
            startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(requireContext(), "No email client installed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateAnalytics()
    }
}