package com.dank.proto_logueo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dank.proto_logueo.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.btnRegisterPlaceholder.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun performLogin() {
        val apiKey = Constants.FIREBASE_API_KEY.trim()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString()?.trim().orEmpty()

        // Reseteo de errores en UI
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        var hasError = false

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_empty_email)
            hasError = true
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_empty_password)
            hasError = true
        }

        if (hasError) return

        if (apiKey.isEmpty() || apiKey == "TU_FIREBASE_API_KEY_AQUI") {
            Toast.makeText(
                this,
                "Por favor configura tu Firebase API Key en Constants.kt",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        showLoading(isLoading = true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val authRequest = AuthRequest(
                    email = email,
                    password = password,
                    returnSecureToken = true,
                )

                val response = RetrofitClient.authApi.signInWithPassword(apiKey, authRequest)

                withContext(Dispatchers.Main) {
                    showLoading(isLoading = false)

                    if (response.isSuccessful) {
                        val authResponse = response.body()
                        val idToken = authResponse?.idToken
                        val uid = authResponse?.localId

                        if (!idToken.isNullOrEmpty() && !uid.isNullOrEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "Autenticación exitosa",
                                Toast.LENGTH_SHORT
                            ).show()

                            val intent = Intent(this@MainActivity, HomeActivity::class.java).apply {
                                putExtra(HomeActivity.EXTRA_ID_TOKEN, idToken)
                                putExtra(HomeActivity.EXTRA_UID, uid)
                            }
                            startActivity(intent)
                            finish()
                        } else {
                            showError("Respuesta de login incompleta (idToken o UID faltantes)")
                        }
                    } else {
                        val errorJson = response.errorBody()?.string()
                        val parsedError = parseError(errorJson)
                        showError(parsedError ?: "Error al autenticar (${response.code()})")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(isLoading = false)
                    val errorMsg = e.localizedMessage ?: "Error desconocido de conexión"
                    showError("Error de red: $errorMsg")
                }
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun parseError(errorJson: String?): String? {
        if (errorJson.isNullOrEmpty()) return null
        return try {
            val errorResponse = gson.fromJson(errorJson, AuthErrorResponse::class.java)

            when (val firebaseMessage = errorResponse.error?.message) {
                "EMAIL_NOT_FOUND" -> "El correo electrónico no se encuentra registrado."
                "INVALID_PASSWORD" -> "La contraseña ingresada es incorrecta."
                "USER_DISABLED" -> "Esta cuenta de usuario ha sido deshabilitada."
                "INVALID_KEY" -> "La Firebase API Key no es válida o no tiene permisos."
                "API_KEY_INVALID" -> "La Firebase API Key proporcionada es inválida."
                else -> firebaseMessage ?: "Error de autenticación ($errorJson)"
            }
        } catch (_: Exception) {
            errorJson
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.btnLogin.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
