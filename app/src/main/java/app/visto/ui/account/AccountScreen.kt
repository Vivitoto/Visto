package app.visto.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.visto.ui.Strings

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
    onCancel: (() -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.APP_NAME) },
                navigationIcon = {
                    if (onCancel != null) {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
            )
        },
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = Strings.ACCOUNT_TITLE, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.displayName,
                onValueChange = { onStateChange(AccountFormReducer.updateDisplayName(state, it)) },
                label = { Text(Strings.ACCOUNT_DISPLAY_NAME) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = { onStateChange(AccountFormReducer.updateBaseUrl(state, it)) },
                label = { Text(Strings.ACCOUNT_SERVER_URL) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.username,
                onValueChange = { onStateChange(AccountFormReducer.updateUsername(state, it)) },
                label = { Text(Strings.ACCOUNT_USERNAME) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = { onStateChange(AccountFormReducer.updatePassword(state, it)) },
                label = { Text(Strings.ACCOUNT_PASSWORD) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.rootPath,
                onValueChange = { onStateChange(AccountFormReducer.updateRootPath(state, it)) },
                label = { Text(Strings.ACCOUNT_ROOT_PATH) },
                singleLine = true,
                isError = !state.isSafeRootPath,
                supportingText = if (!state.isSafeRootPath) {
                    { Text(Strings.ERR_INVALID_PATH) }
                } else null,
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
                    modifier = Modifier.weight(1f),
                ) {
                    if (state.isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(Strings.ACCOUNT_TEST_CONNECTION, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Button(
                    onClick = onSave,
                    enabled = state.canSave,
                    modifier = Modifier.weight(1f),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(Strings.ACCOUNT_SAVE_AND_USE, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
