package com.example.budgetapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardFragment : Fragment() {

    private lateinit var deleteTransaction: Transaction
    private lateinit var transactions: List<Transaction>
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    private lateinit var balanceTextView: TextView
    private lateinit var incomeTextView: TextView
    private lateinit var expenseTextView: TextView
    private lateinit var db: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // Initialize the views
        balanceTextView = view.findViewById(R.id.balance)
        incomeTextView = view.findViewById(R.id.income)
        expenseTextView = view.findViewById(R.id.expense)

        // Initialize transactions
        transactions = arrayListOf()

        transactionAdapter = TransactionAdapter(transactions)
        linearLayoutManager = LinearLayoutManager(activity)

        db = Room.databaseBuilder(
            requireActivity().applicationContext,
            AppDatabase::class.java,
            "transactions"
        ).build()

        val recyclerview = view.findViewById<RecyclerView>(R.id.transactions_list)

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

        fetchAll()

        return view
    }

    private fun fetchAll() {
        GlobalScope.launch(Dispatchers.IO) {
            transactions = db.transactionDao().getAll()

            withContext(Dispatchers.Main) {
                updateDashboard()
                transactionAdapter.setData(transactions)
            }
        }
    }

    private fun updateDashboard() {

        val totalAmount = transactions.map { it.amount }.sum()
        val incomeAmount = transactions.filter { it.amount > 0 }.map { it.amount }.sum()
        val expenseAmount = totalAmount - incomeAmount

        // Update the views
        balanceTextView.text = "$ %.2f".format(totalAmount)
        incomeTextView.text = "$ %.2f".format(incomeAmount)
        expenseTextView.text = "$ %.2f".format(expenseAmount)
    }

    private fun deleteTransaction(transaction: Transaction) {
        deleteTransaction = transaction

        GlobalScope.launch(Dispatchers.IO) {
            db.transactionDao().delete(transaction)
            transactions = transactions.filter { it.id != transaction.id }
            requireActivity().runOnUiThread {
                updateDashboard()
                transactionAdapter.setData(transactions)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fetchAll()
    }
}