package com.dank.proto_logueo

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dank.proto_logueo.databinding.ActivityRegisterBinding
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etFechaNacimiento.setOnClickListener { showDatePicker() }
        binding.btnRegister.setOnClickListener { performRegister() }
        binding.btnBackToLogin.setOnClickListener { finish() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance().apply { add(Calendar.YEAR, -25) }
        val dialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                binding.etFechaNacimiento.setText(
                    String.format("%04d-%02d-%02d", year, month + 1, day)
                )
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.maxDate = System.currentTimeMillis()
        dialog.show()
    }

    private fun performRegister() {
        val nombre = binding.etNombre.text?.toString()?.trim().orEmpty()
        val apellido = binding.etApellido.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val telefono = binding.etTelefono.text?.toString()?.trim().orEmpty()
        val nacionalidad = binding.etNacionalidad.text?.toString()?.trim().orEmpty()
        val fechaNacimiento = binding.etFechaNacimiento.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirm = binding.etConfirmPassword.text?.toString().orEmpty()

        listOf(
            binding.tilNombre, binding.tilApellido, binding.tilEmail,
            binding.tilPassword, binding.tilConfirmPassword
        ).forEach { it.error = null }

        var hasError = false
        if (nombre.isEmpty()) { binding.tilNombre.error = "Ingresa tu nombre"; hasError = true }
        if (apellido.isEmpty()) { binding.tilApellido.error = "Ingresa tu apellido"; hasError = true }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Ingresa un correo válido"; hasError = true
        }
        if (password.length < 6) {
            binding.tilPassword.error = "Mínimo 6 caracteres"; hasError = true
        }
        if (confirm != password) {
            binding.tilConfirmPassword.error = "Las contraseñas no coinciden"; hasError = true
        }
        if (hasError) return

        val apiKey = Constants.FIREBASE_API_KEY.trim()
        setLoading(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1) Crear la cuenta en Firebase
                val signUp = RetrofitClient.authApi.signUp(apiKey, AuthRequest(email, password))
                if (!signUp.isSuccessful) {
                    val msg = parseFirebaseError(signUp.errorBody()?.string())
                    withContext(Dispatchers.Main) {
                        setLoading(false)
                        Toast.makeText(this@RegisterActivity, msg, Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val idToken = signUp.body()?.idToken
                val uid = signUp.body()?.localId
                if (idToken.isNullOrEmpty() || uid.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        setLoading(false)
                        Toast.makeText(
                            this@RegisterActivity,
                            "Cuenta creada, pero la respuesta fue incompleta. Inicia sesión.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    return@launch
                }

                // 2) Crear el perfil en el backend de Lapticon
                var profileWarning: String? = null
                try {
                    val profile = RetrofitClient.lapticonApi.createUserProfile(
                        "Bearer $idToken",
                        CreateUserRequest(
                            uid = uid,
                            email = email,
                            correo = email,
                            nombre = nombre,
                            apellido = apellido,
                            telefono = telefono.ifEmpty { null },
                            nacionalidad = nacionalidad.ifEmpty { null },
                            fechaNacimiento = fechaNacimiento.ifEmpty { null }
                        )
                    )
                    if (!profile.isSuccessful) {
                        profileWarning = "No se pudo guardar el perfil (${profile.code()}). " +
                            "Puedes completarlo desde Perfil > Editar."
                    }
                } catch (e: Exception) {
                    profileWarning = "No se pudo guardar el perfil: ${e.localizedMessage}"
                }

                withContext(Dispatchers.Main) {
                    setLoading(false)
                    Toast.makeText(
                        this@RegisterActivity,
                        profileWarning ?: "¡Cuenta creada exitosamente!",
                        Toast.LENGTH_LONG
                    ).show()

                    startActivity(Intent(this@RegisterActivity, HomeActivity::class.java).apply {
                        putExtra(HomeActivity.EXTRA_ID_TOKEN, idToken)
                        putExtra(HomeActivity.EXTRA_UID, uid)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    Toast.makeText(
                        this@RegisterActivity,
                        "Error de red: ${e.localizedMessage ?: "sin conexión"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun parseFirebaseError(errorJson: String?): String {
        val code = try {
            gson.fromJson(errorJson, AuthErrorResponse::class.java).error?.message
        } catch (_: Exception) {
            null
        }.orEmpty()

        return when {
            code.startsWith("EMAIL_EXISTS") -> "Ese correo ya está registrado. Inicia sesión."
            code.startsWith("WEAK_PASSWORD") -> "La contraseña es muy débil (mínimo 6 caracteres)."
            code.startsWith("INVALID_EMAIL") -> "El correo no es válido."
            code.startsWith("OPERATION_NOT_ALLOWED") ->
                "El registro con correo y contraseña no está habilitado en Firebase."
            code.startsWith("TOO_MANY_ATTEMPTS") -> "Demasiados intentos. Prueba más tarde."
            code.isNotEmpty() -> "No se pudo registrar: $code"
            else -> "No se pudo registrar. Intenta de nuevo."
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnRegister.isEnabled = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
