package com.nk.expensetracker.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nk.expensetracker.core.time.TimeUtils
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    dateMillis: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    
    val dateText = remember(dateMillis) {
        val localDate = TimeUtils.millisToLocalDate(dateMillis)
        localDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    }

    OutlinedTextField(
        value = dateText,
        onValueChange = {},
        readOnly = true,
        label = { Text("Date") },
        trailingIcon = {
            Icon(Icons.Filled.DateRange, contentDescription = "Select Date")
        },
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = OutlinedTextFieldDefaults.colors().unfocusedTextColor,
            disabledBorderColor = OutlinedTextFieldDefaults.colors().unfocusedIndicatorColor,
            disabledLabelColor = OutlinedTextFieldDefaults.colors().unfocusedLabelColor,
            disabledTrailingIconColor = OutlinedTextFieldDefaults.colors().unfocusedTrailingIconColor
        ),
        enabled = false, // Disable typing, make it clickable only
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
    )

    if (showDialog) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                        showDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("CANCEL")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
