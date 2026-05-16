package com.kktaro.simplifyweightrecorder.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.kktaro.simplifyweightrecorder.R
import com.kktaro.simplifyweightrecorder.domain.model.WeightInputResult

@Composable
fun WeightInputContent(
    weightInput: String,
    validation: WeightInputResult,
    isSubmitting: Boolean,
    onWeightChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasError = validation !is WeightInputResult.Valid && validation !is WeightInputResult.Empty
    val errorMessageRes = when (validation) {
        WeightInputResult.NotANumber -> R.string.error_not_a_number
        WeightInputResult.OutOfRange -> R.string.error_out_of_range
        WeightInputResult.TooManyDecimals -> R.string.error_too_many_decimals
        else -> null
    }
    val isSubmitEnabled = validation is WeightInputResult.Valid && !isSubmitting

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.weight_screen_title),
            style = MaterialTheme.typography.titleLarge
        )
        OutlinedTextField(
            value = weightInput,
            onValueChange = onWeightChange,
            label = { Text(stringResource(id = R.string.weight_input_label)) },
            singleLine = true,
            enabled = !isSubmitting,
            isError = hasError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            supportingText = errorMessageRes?.let {
                { Text(text = stringResource(id = it)) }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = onSubmit,
            enabled = isSubmitEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(text = stringResource(id = R.string.submit_button_label))
            }
        }
    }
}
