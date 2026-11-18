package com.dank.proto_logueo

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dank.proto_logueo.databinding.ActivityProfileBinding
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID_TOKEN = "extra_id_token"
        const val EXTRA_UID = "extra_uid"
    }

    private lateinit var binding: ActivityProfileBinding
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private var idToken: String = ""
    private var uid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idToken = intent.getStringExtra(EXTRA_ID_TOKEN).orEmpty()
        uid = intent.getStringExtra(EXTRA_UID).orEmpty()

        binding.btnRetry.setOnClickListener {
            loadUserProfile()
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }

        if (idToken.isEmpty() || uid.isEmpty()) {
            showErrorState("No se recibió el token de sesión o el UID correctamente.")
        } else {
            loadUserProfile()
        }
    }

    private fun loadUserProfile() {
        val authHeader = "Bearer $idToken"

        showLoadingState()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.lapticonApi.getUserProfile(uid, authHeader)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val profileResponse = response.body()
                        val userData = profileResponse?.user

                        if (profileResponse != null && userData != null) {
                            displayUserProfile(profileResponse, userData)
                        } else {
                            showErrorState("El servidor respondió sin datos de usuario (Código 200).")
                        }
                    } else {
                        val errorBody = response.errorBody()?.string().orEmpty()
                        showErrorState("Error del servidor (${response.code()}): $errorBody")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMsg = e.localizedMessage ?: "Error desconocido al conectar con el servidor."
                    showErrorState("Error de conexión:\n$errorMsg")
                }
            }
        }
    }

    private fun displayUserProfile(profileResponse: UserProfileResponse, user: UserData) {
        binding.layoutLoading.visibility = View.GONE
        binding.cardError.visibility = View.GONE
        binding.layoutContent.visibility = View.VISIBLE

        // Header Avatar and Name
        val initial = user.nombre?.firstOrNull()?.uppercaseChar()?.toString()
            ?: user.email?.firstOrNull()?.uppercaseChar()?.toString()
            ?: "U"
        binding.tvAvatarInitial.text = initial
        binding.tvHeaderName.text = user.fullName
        binding.tvHeaderEmail.text = user.displayEmail

        // Personal Information
        binding.tvNombre.text = user.nombre ?: "N/A"
        binding.tvApellido.text = user.apellido ?: "N/A"
        binding.tvCorreo.text = user.displayEmail.ifEmpty { "N/A" }
        binding.tvTelefono.text = user.telefono ?: "N/A"
        binding.tvNacionalidad.text = user.nacionalidad ?: "N/A"
        binding.tvFechaNacimiento.text = user.fechaNacimiento ?: "N/A"

        // System / Database Information
        binding.tvUid.text = user.uid ?: uid
        binding.tvDbId.text = user.id ?: "N/A"
        binding.tvCreatedAt.text = user.createdAt ?: "N/A"
        binding.tvUpdatedAt.text = user.updatedAt ?: "N/A"

        // Raw JSON
        binding.tvProfileRawJson.text = gson.toJson(profileResponse)
    }

    private fun showLoadingState() {
        binding.layoutLoading.visibility = View.VISIBLE
        binding.cardError.visibility = View.GONE
        binding.layoutContent.visibility = View.GONE
    }

    private fun showErrorState(message: String) {
        binding.layoutLoading.visibility = View.GONE
        binding.cardError.visibility = View.VISIBLE
        binding.layoutContent.visibility = View.GONE
        binding.tvErrorMessage.text = message
    }

    private fun logout() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
