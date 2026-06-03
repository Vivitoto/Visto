package app.visto.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * First-launch / settings screen for the WebDAV account.
 *
 * Pure UI: it relies on the caller to supply a [state] holder and the four
 * callbacks. This keeps the screen straightforward to drive from
 * [MainActivity] or from a future ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    state: AccountFormState,
    onStateChange: (AccountFormState) -> Unit,
    onTestConnection: () -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Visto") })
        },
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Connect a WebDAV account")
            OutlinedTextField(
                value = state.displayName,
                onValueChange = { onStateChange(AccountFormReducer.updateDisplayName(state, it)) },
                label = { Text("Display name") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = { onStateChange(AccountFormReducer.updateBaseUrl(state, it)) },
                label = { Text("Server URL (https://...)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.username,
                onValueChange = { onStateChange(AccountFormReducer.updateUsername(state, it)) },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = { onStateChange(AccountFormReducer.updatePassword(state, it)) },
                label = { Text("Password / app token") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.rootPath,
                onValueChange = { onStateChange(AccountFormReducer.updateRootPath(state, it)) },
                label = { Text("Root path") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            state.errorMessage?.let { Text(text = it) }
            state.message?.let { Text(text = it) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onTestConnection,
                    enabled = state.canTestConnection,
                ) {
                    if (state.isTesting) {
                        CircularProgressIndicator()
                    } else {
                        Text("Test connection")
                    }
                }
                Button(
                    onClick = onSave,
                    enabled = state.canSave,
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator()
                    } else {
                        Text("Save & open")
                    }
                }
            }
        }
    }
}
