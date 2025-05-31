package com.example.linklive.presentation.auth

import android.content.Intent
import android.util.Patterns
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.linklive.R
import com.example.linklive.presentation.auth.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen() {
    val context = LocalContext.current
    val owner = LocalViewModelStoreOwner.current

    val viewModel: AuthViewModel = remember {
        ViewModelProvider(owner!!)[AuthViewModel::class.java]
    }

    val googleSignInClient = viewModel.provideGoogleSignInClient(context)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isValidEmail by remember { mutableStateOf(true) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                viewModel.signInWithGoogle(idToken)
            }
        } catch (e: ApiException) {
            // Handle error
        }
    }

    OutlinedTextField(
        value = email,
        onValueChange = {
            email = it
            isValidEmail = Patterns.EMAIL_ADDRESS.matcher(it).matches()
        },
        label = { Text("Email Address") },
        isError = !isValidEmail,
        leadingIcon = {
            Icon(
                Icons.Default.Email,
                tint = Color.DarkGray,
                contentDescription = null
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.DarkGray,
            focusedTextColor = Color.DarkGray,
            unfocusedTextColor = Color.DarkGray,
            cursorColor = Color.DarkGray,
            focusedLabelColor = Color.DarkGray,
            unfocusedLabelColor = Color.Gray,
            errorBorderColor = Color.Red,
            errorLabelColor = Color.Red,
            errorTextColor = Color.Gray
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(8.dp)
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        leadingIcon = {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = Color.DarkGray
            )
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.DarkGray,
            focusedTextColor = Color.DarkGray,
            unfocusedTextColor = Color.DarkGray,
            cursorColor = Color.DarkGray,
            focusedLabelColor = Color.DarkGray,
            unfocusedLabelColor = Color.Gray,
            errorBorderColor = Color.Red,
            errorLabelColor = Color.Red,
            errorTextColor = Color.Gray
        ),
        trailingIcon = {
            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = image,
                    tint = Color.DarkGray,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(8.dp)
    )

    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = rememberMe,
            onCheckedChange = { rememberMe = it },
            colors = CheckboxDefaults.colors(
                uncheckedColor = Color.Gray,
                checkedColor = Color.Gray
            ),
            modifier = Modifier
                .size(12.dp)
                .padding(start = 10.dp)
        )
        Spacer(Modifier.width(18.dp))
        Text(
            "Remember me",
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "Forgot Password?",
            modifier = Modifier.clickable { /* handle */ },
            fontSize = 14.sp,
            color = colorResource(R.color.dark_green),
            fontWeight = FontWeight.Medium
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = { viewModel.loginEmail(email, password) },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.dark_green))
    ) {
        Text(
            "Login",
            fontSize = 16.sp,
            color = colorResource(R.color.white)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color.LightGray
        )
        Text("  Or login with  ", color = Color.Gray)
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color.LightGray
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SocialLoginButton(
            icon = painterResource(id = R.drawable.ic_google),
            label = "Google",
            modifier = Modifier.weight(1f),
            googleSignInClient = googleSignInClient,
            launcher = launcher
        )
        SocialLoginButton(
            icon = painterResource(id = R.drawable.ic_microsoft),
            label = "Microsoft",
            modifier = Modifier.weight(1f),
            launcher = launcher
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
fun SocialLoginButton(
    icon: Painter,
    label: String,
    modifier: Modifier = Modifier,
    googleSignInClient: GoogleSignInClient? = null,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>? = null
) {
    OutlinedButton(
        onClick = {
            when (label) {
                "Google" -> {
                    googleSignInClient?.let { client ->
                        val signInIntent = client.signInIntent
                        launcher?.launch(signInIntent)
                    }
                }
                "Microsoft" -> {
                    // Handle Microsoft login
                }
            }
        },
        modifier = modifier
            .height(50.dp)
            .padding(horizontal = 4.dp),
        border = BorderStroke(1.dp, Color.Gray)
    ) {
        Icon(painter = icon, contentDescription = label, tint = Color.Unspecified)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = colorResource(R.color.black),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Preview
@Composable
private fun PreviewLoginScreen() {
    LoginScreen()
}