package com.sciencehackfest.trive_sci.view

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.NumberFormat
import java.util.Locale

class CurrencyTextWatcher(private val editText: EditText) : TextWatcher {
    private var isEditing = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        if (isEditing) return

        isEditing = true

        val rawString = s.toString().replace(".", "")
        val amount: Long = if (rawString.isEmpty()) 0 else rawString.toLong()

        val numberFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))
        val formattedString = numberFormat.format(amount)

        editText.setText(formattedString)
        editText.setSelection(formattedString.length)

        isEditing = false
    }
}
