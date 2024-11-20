package com.example.budgetapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TransactionDao {
    @Query("SELECT * from transactions")
    fun getAll(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE SUBSTR(date, 1, 4) = :year AND SUBSTR(date, 6, 2) = :month")
    fun getTransactionsForYearAndMonth(year: String, month: String): List<Transaction>

    @Insert
    fun insertAll(vararg transaction: Transaction)

    @Delete
    fun delete(transaction: Transaction)

    @Update
    fun update(vararg transaction: Transaction)
}