package com.dank.proto_logueo

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dank.proto_logueo.databinding.ActivityHomeBinding
import com.dank.proto_logueo.databinding.LayoutTabEncomiendasBinding
import com.dank.proto_logueo.databinding.LayoutTabPerfilBinding
import com.dank.proto_logueo.databinding.LayoutTabViajesBinding
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class HomeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID_TOKEN = "extra_id_token"
        const val EXTRA_UID = "extra_uid"
    }

    private lateinit var binding: ActivityHomeBinding

    private var idToken: String = ""
    private var uid: String = ""
    private var authHeader: String = ""

    private var currentTabId: Int = 0

    // Lista cargada de ciudades desde el backend
    private var availableCities: List<String> = listOf("Bogota", "Cali", "Medellin", "Miami", "Madrid", "Paris")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        idToken = intent.getStringExtra(EXTRA_ID_TOKEN).orEmpty()
        uid = intent.getStringExtra(EXTRA_UID).orEmpty()
        authHeader = "Bearer $idToken"

        if (idToken.isEmpty() || uid.isEmpty()) {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Cargar lista de ciudades desde la API en segundo plano
        fetchCitiesFromApi()

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_viajes -> {
                    if (currentTabId != R.id.nav_viajes) {
                        currentTabId = R.id.nav_viajes
                        showViajesTab()
                    }
                    true
                }
                R.id.nav_encomiendas -> {
                    if (currentTabId != R.id.nav_encomiendas) {
                        currentTabId = R.id.nav_encomiendas
                        showEncomiendasTab()
                    }
                    true
                }
                R.id.nav_perfil -> {
                    if (currentTabId != R.id.nav_perfil) {
                        currentTabId = R.id.nav_perfil
                        showPerfilTab()
                    }
                    true
                }
                else -> false
            }
        }

        // Pestaña por defecto
        binding.bottomNavigation.selectedItemId = R.id.nav_viajes
    }

    private fun fetchCitiesFromApi() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.lapticonApi.getCiudades()
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val extractedCities = response.body()!!.mapNotNull { it.ciudad }.filter { it.isNotBlank() }.distinct()
                    if (extractedCities.isNotEmpty()) {
                        availableCities = extractedCities.sorted()
                    }
                }
            } catch (_: Exception) {
                // Mantiene lista por defecto si falla la conexión
            }
        }
    }

    private fun setupCityDropdown(actv: AutoCompleteTextView) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, availableCities)
        actv.setAdapter(adapter)
        actv.setOnClickListener { actv.showDropDown() }
        actv.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) actv.showDropDown() }
    }

    // ==========================================
    // TAB 1: VIAJES (UBER RIDES)
    // ==========================================
    private fun showViajesTab() {
        binding.containerView.removeAllViews()
        val tabBinding = LayoutTabViajesBinding.inflate(layoutInflater, binding.containerView, true)

        setupCityDropdown(tabBinding.actvSearchCiudad)

        tabBinding.btnBuscarViajes.setOnClickListener {
            val ciudad = tabBinding.actvSearchCiudad.text?.toString()?.trim().orEmpty()
            if (ciudad.isEmpty()) {
                Toast.makeText(this, "Selecciona una ciudad de origen", Toast.LENGTH_SHORT).show()
            } else {
                searchViajes(ciudad, tabBinding)
            }
        }

        tabBinding.btnCrearViaje.setOnClickListener {
            showCrearViajeDialog {
                loadMisViajes(tabBinding)
                checkViajeDisponible(tabBinding)
            }
        }

        tabBinding.btnMisViajes.setOnClickListener {
            loadMisViajes(tabBinding)
        }

        checkViajeDisponible(tabBinding)
        loadMisViajes(tabBinding)
    }

    private fun checkViajeDisponible(tabBinding: LayoutTabViajesBinding) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.lapticonApi.getViajeDisponible(authHeader)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val viaje = response.body()!!
                        tabBinding.cardActiveTrip.visibility = View.VISIBLE
                        tabBinding.tvActiveTripDetail.text =
                            "Vuelo ${viaje.numeroVuelo ?: "N/A"}: ${viaje.ciudadOrigen} ➔ ${viaje.ciudadDestino} (${viaje.fechaViaje})"
                    } else {
                        tabBinding.cardActiveTrip.visibility = View.GONE
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    tabBinding.cardActiveTrip.visibility = View.GONE
                }
            }
        }
    }

    private fun searchViajes(ciudad: String, tabBinding: LayoutTabViajesBinding) {
        tabBinding.pbViajes.visibility = View.VISIBLE
        tabBinding.containerViajesList.removeAllViews()
        tabBinding.tvViajesSectionTitle.text = "Resultados para $ciudad"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.lapticonApi.buscarViajes(authHeader, ciudad)
                withContext(Dispatchers.Main) {
                    tabBinding.pbViajes.visibility = View.GONE
                    if (response.isSuccessful) {
                        val viajes = response.body().orEmpty()
                        if (viajes.isEmpty()) {
                            addEmptyMessageToContainer(tabBinding.containerViajesList, "No se encontraron viajes disponibles desde $ciudad.")
                        } else {
                            viajes.forEach { viaje ->
                                addViajeCardToContainer(tabBinding.containerViajesList, viaje)
                            }
                        }
                    } else {
                        addEmptyMessageToContainer(tabBinding.containerViajesList, "Error al buscar viajes (${response.code()})")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tabBinding.pbViajes.visibility = View.GONE
                    addEmptyMessageToContainer(tabBinding.containerViajesList, "Error de conexión: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun loadMisViajes(tabBinding: LayoutTabViajesBinding) {
        tabBinding.pbViajes.visibility = View.VISIBLE
        tabBinding.containerViajesList.removeAllViews()
        tabBinding.tvViajesSectionTitle.text = "Mis Viajes Creados"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.lapticonApi.getMisViajes(authHeader)
                withContext(Dispatchers.Main) {
                    tabBinding.pbViajes.visibility = View.GONE
                    if (response.isSuccessful) {
                        val viajes = response.body().orEmpty()
                        if (viajes.isEmpty()) {
                            addEmptyMessageToContainer(tabBinding.containerViajesList, "Aún no has registrado ningún viaje.")
                        } else {
                            viajes.forEach { viaje ->
                                addViajeCardToContainer(tabBinding.containerViajesList, viaje)
                            }
                        }
                    } else {
                        addEmptyMessageToContainer(tabBinding.containerViajesList, "Error al cargar mis viajes (${response.code()})")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tabBinding.pbViajes.visibility = View.GONE
                    addEmptyMessageToContainer(tabBinding.containerViajesList, "Error de red: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun showCrearViajeDialog(onSuccess: () -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_crear_viaje, null)
        val etVuelo = dialogView.findViewById<EditText>(R.id.etNumeroVuelo)
        val actvOrigen = dialogView.findViewById<AutoCompleteTextView>(R.id.actvCiudadOrigen)
        val actvDestino = dialogView.findViewById<AutoCompleteTextView>(R.id.actvCiudadDestino)
        val etFecha = dialogView.findViewById<EditText>(R.id.etFechaViaje)

        // 1. Número de vuelo generado automáticamente
        val autoFlightNum = "FL-" + (1000..9999).random()
        etVuelo.setText(autoFlightNum)

        // 2. Dropdowns de ciudades
        setupCityDropdown(actvOrigen)
        setupCityDropdown(actvDestino)

        // 3. DatePicker con Calendario
        etFecha.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val monthFormatted = String.format("%02d", month + 1)
                    val dayFormatted = String.format("%02d", dayOfMonth)
                    etFecha.setText("$year-$monthFormatted-$dayFormatted")
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        AlertDialog.Builder(this)
            .setTitle("🚗 Crear Nuevo Viaje")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val vuelo = etVuelo.text.toString().trim()
                val origen = actvOrigen.text.toString().trim()
                val destino = actvDestino.text.toString().trim()
                val fecha = etFecha.text.toString().trim()

                if (origen.isEmpty() || destino.isEmpty() || fecha.isEmpty()) {
                    Toast.makeText(this, "Selecciona ciudad de origen, destino y fecha", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val request = CreateViajeRequest(vuelo, origen, destino, fecha)
                        val response = RetrofitClient.lapticonApi.createViaje(authHeader, request)
                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                Toast.makeText(this@HomeActivity, "¡Viaje creado exitosamente!", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            } else {
                                val errorMsg = response.errorBody()?.string() ?: "Error (${response.code()})"
                                Toast.makeText(this@HomeActivity, "Error al crear viaje: $errorMsg", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@HomeActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addViajeCardToContainer(container: LinearLayout, viaje: Viaje) {
        val card = MaterialCardView(this).apply {
            radius = 12f
            cardElevation = 2f
            setCardBackgroundColor(Color.WHITE)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 12)
            layoutParams = params
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val tvTitle = TextView(this).apply {
            text = "✈️ Vuelo: ${viaje.numeroVuelo ?: "N/A"}"
            textSize = 15f
            setTextColor(Color.BLACK)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val tvRoute = TextView(this).apply {
            text = "📍 RUTA: ${viaje.ciudadOrigen ?: "Origen"} ➔ ${viaje.ciudadDestino ?: "Destino"}"
            textSize = 14f
            setTextColor(Color.parseColor("#1D1B20"))
            setPadding(0, 4, 0, 4)
        }

        val tvFecha = TextView(this).apply {
            text = "📅 Fecha: ${viaje.fechaViaje ?: "Sin fecha"}  |  Estado: ${viaje.estado ?: "disponible"}"
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
        }

        layout.addView(tvTitle)
        layout.addView(tvRoute)
        layout.addView(tvFecha)
        card.addView(layout)
        container.addView(card)
    }

    // ==========================================
    // TAB 2: ENCOMIENDAS (UBER EXPRESS / CONNECT)
    // ==========================================
    private fun showEncomiendasTab() {
        binding.containerView.removeAllViews()
        val tabBinding = LayoutTabEncomiendasBinding.inflate(layoutInflater, binding.containerView, true)

        setupCityDropdown(tabBinding.actvSearchOrigen)
        setupCityDropdown(tabBinding.actvSearchDestino)

        tabBinding.btnBuscarEncomiendas.setOnClickListener {
            val origen = tabBinding.actvSearchOrigen.text?.toString()?.trim().orEmpty()
            val destino = tabBinding.actvSearchDestino.text?.toString()?.trim().orEmpty()

            if (origen.isEmpty() || destino.isEmpty()) {
                Toast.makeText(this, "Selecciona origen y destino para buscar", Toast.LENGTH_SHORT).show()
            } else {
                searchEncomiendas(origen, destino, tabBinding)
            }
        }

        tabBinding.btnCrearEncomienda.setOnClickListener {
            showCrearEncomiendaDialog {
                loadMisEncomiendas(tabBinding)
            }
        }

        tabBinding.btnMisEncomiendas.setOnClickListener {
            loadMisEncomiendas(tabBinding)
        }

        loadMisEncomiendas(tabBinding)
    }

    private fun searchEncomiendas(origen: String, destino: String, tabBinding: LayoutTabEncomiendasBinding) {
        tabBinding.pbEncomiendas.visibility = View.VISIBLE
        tabBinding.containerEncomiendasList.removeAllViews()
        tabBinding.tvEncomiendasSectionTitle.text = "Encomiendas disponibles: $origen ➔ $destino"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.lapticonApi.getEncomiendasDisponibles(authHeader, origen, destino)
                withContext(Dispatchers.Main) {
                    tabBinding.pbEncomiendas.visibility = View.GONE
                    if (response.isSuccessful) {
                        val encomiendas = response.body().orEmpty()
                        if (encomiendas.isEmpty()) {
                            addEmptyMessageToContainer(tabBinding.containerEncomiendasList, "No hay encomiendas disponibles de $origen a $destino.")
                        } else {
                            encomiendas.forEach { encomienda ->
                                addEncomiendaCardToContainer(tabBinding.containerEncomiendasList, encomienda, isMine = false, tabBinding)
                            }
                        }
                    } else {
                        addEmptyMessageToContainer(tabBinding.containerEncomiendasList, "Error al buscar encomiendas (${response.code()})")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tabBinding.pbEncomiendas.visibility = View.GONE
                    addEmptyMessageToContainer(tabBinding.containerEncomiendasList, "Error de red: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun loadMisEncomiendas(tabBinding: LayoutTabEncomiendasBinding) {
        tabBinding.pbEncomiendas.visibility = View.VISIBLE
        tabBinding.containerEncomiendasList.removeAllViews()
        tabBinding.tvEncomiendasSectionTitle.text = "Mis Encomiendas Registradas"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.lapticonApi.getMisEncomiendas(authHeader)
                withContext(Dispatchers.Main) {
                    tabBinding.pbEncomiendas.visibility = View.GONE
                    if (response.isSuccessful) {
                        val encomiendas = response.body().orEmpty()
                        if (encomiendas.isEmpty()) {
                            addEmptyMessageToContainer(tabBinding.containerEncomiendasList, "No has registrado encomiendas aún.")
                        } else {
                            encomiendas.forEach { encomienda ->
                                addEncomiendaCardToContainer(tabBinding.containerEncomiendasList, encomienda, isMine = true, tabBinding)
                            }
                        }
                    } else {
                        addEmptyMessageToContainer(tabBinding.containerEncomiendasList, "Error al cargar mis encomiendas (${response.code()})")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tabBinding.pbEncomiendas.visibility = View.GONE
                    addEmptyMessageToContainer(tabBinding.containerEncomiendasList, "Error de red: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun showCrearEncomiendaDialog(onSuccess: () -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_crear_encomienda, null)
        val actvOrigen = dialogView.findViewById<AutoCompleteTextView>(R.id.actvCiudadOrigenEnc)
        val actvDestino = dialogView.findViewById<AutoCompleteTextView>(R.id.actvCiudadDestinoEnc)
        val etObs = dialogView.findViewById<EditText>(R.id.etObservacionesEnc)
        val etValor = dialogView.findViewById<EditText>(R.id.etValorEnc)

        setupCityDropdown(actvOrigen)
        setupCityDropdown(actvDestino)

        AlertDialog.Builder(this)
            .setTitle("📦 Crear Nueva Encomienda")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val origen = actvOrigen.text.toString().trim()
                val destino = actvDestino.text.toString().trim()
                val obs = etObs.text.toString().trim()
                val valorStr = etValor.text.toString().trim()
                val valor = valorStr.toDoubleOrNull() ?: 10.0

                if (origen.isEmpty() || destino.isEmpty() || obs.isEmpty()) {
                    Toast.makeText(this, "Selecciona origen, destino y observaciones", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val request = CreateEncomiendaRequest(origen, destino, obs, "USD", valor)
                        val response = RetrofitClient.lapticonApi.createEncomienda(authHeader, request)
                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                Toast.makeText(this@HomeActivity, "¡Encomienda creada exitosamente!", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            } else {
                                val errorMsg = response.errorBody()?.string() ?: "Error (${response.code()})"
                                Toast.makeText(this@HomeActivity, "Error al crear encomienda: $errorMsg", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@HomeActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addEncomiendaCardToContainer(
        container: LinearLayout,
        encomienda: Encomienda,
        isMine: Boolean,
        tabBinding: LayoutTabEncomiendasBinding
    ) {
        val card = MaterialCardView(this).apply {
            radius = 12f
            cardElevation = 2f
            setCardBackgroundColor(Color.WHITE)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 12)
            layoutParams = params
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val tvTitle = TextView(this).apply {
            text = "📦 Encomienda: ${encomienda.ciudadOrigen ?: "N/A"} ➔ ${encomienda.ciudadDestino ?: "N/A"}"
            textSize = 15f
            setTextColor(Color.BLACK)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val tvDetails = TextView(this).apply {
            text = "📝 Obs: ${encomienda.observaciones ?: "Sin notas"} | 💵 Valor: ${encomienda.valor ?: 0.0} ${encomienda.moneda ?: "USD"}"
            textSize = 13f
            setTextColor(Color.parseColor("#1D1B20"))
            setPadding(0, 4, 0, 4)
        }

        val tvEstado = TextView(this).apply {
            text = "📌 Estado: ${encomienda.estado ?: "disponible"}"
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
        }

        layout.addView(tvTitle)
        layout.addView(tvDetails)
        layout.addView(tvEstado)

        if (isMine && !encomienda.id.isNullOrEmpty()) {
            val btnCambiarEstado = com.google.android.material.button.MaterialButton(this).apply {
                text = "⚙️ Cambiar Estado"
                textSize = 11f
                setTextColor(Color.parseColor("#1565C0"))
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    showActualizarEstadoEncomiendaDialog(encomienda.id, tabBinding)
                }
            }
            layout.addView(btnCambiarEstado)
        }

        card.addView(layout)
        container.addView(card)
    }

    private fun showActualizarEstadoEncomiendaDialog(encomiendaId: String, tabBinding: LayoutTabEncomiendasBinding) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_actualizar_estado_encomienda, null)
        val actvEstado = dialogView.findViewById<AutoCompleteTextView>(R.id.actvEstadoEncomienda)

        val estados = listOf("disponible", "tomada", "vencida", "recibida", "cancelada")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, estados)
        actvEstado.setAdapter(adapter)
        actvEstado.setOnClickListener { actvEstado.showDropDown() }

        AlertDialog.Builder(this)
            .setTitle("Actualizar Estado de Encomienda")
            .setView(dialogView)
            .setPositiveButton("Actualizar") { _, _ ->
                val nuevoEstado = actvEstado.text.toString().trim()
                if (nuevoEstado.isEmpty()) {
                    Toast.makeText(this, "Selecciona un estado", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val request = UpdateEstadoEncomiendaRequest(nuevoEstado)
                        val response = RetrofitClient.lapticonApi.updateEstadoEncomienda(encomiendaId, authHeader, request)
                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                Toast.makeText(this@HomeActivity, "Estado actualizado a '$nuevoEstado'", Toast.LENGTH_SHORT).show()
                                loadMisEncomiendas(tabBinding)
                            } else {
                                Toast.makeText(this@HomeActivity, "Error al cambiar estado (${response.code()})", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@HomeActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ==========================================
    // TAB 3: PERFIL (USER PROFILE & RATINGS)
    // ==========================================
    private fun showPerfilTab() {
        binding.containerView.removeAllViews()
        val tabBinding = LayoutTabPerfilBinding.inflate(layoutInflater, binding.containerView, true)

        tabBinding.btnEditarPerfil.setOnClickListener {
            showEditarPerfilDialog(tabBinding)
        }

        tabBinding.btnVerRatings.setOnClickListener {
            showBuscarRatingsDialog()
        }

        tabBinding.btnCalificarUsuario.setOnClickListener {
            showCalificarUsuarioDialog()
        }

        tabBinding.btnLogout.setOnClickListener {
            logout()
        }

        loadPerfil(tabBinding)
    }

    private fun loadPerfil(tabBinding: LayoutTabPerfilBinding) {
        tabBinding.pbPerfil.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.lapticonApi.getUserProfile(uid, authHeader)
                withContext(Dispatchers.Main) {
                    tabBinding.pbPerfil.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.user != null) {
                        val user = response.body()!!.user!!
                        val initial = user.nombre?.firstOrNull()?.uppercaseChar()?.toString()
                            ?: user.email?.firstOrNull()?.uppercaseChar()?.toString()
                            ?: "U"

                        tabBinding.tvAvatarInitial.text = initial
                        tabBinding.tvHeaderName.text = user.fullName
                        tabBinding.tvHeaderEmail.text = user.displayEmail

                        tabBinding.tvNombreCompleto.text = user.fullName
                        tabBinding.tvCorreo.text = user.displayEmail.ifEmpty { "N/A" }
                        tabBinding.tvTelefono.text = user.telefono ?: "N/A"
                        tabBinding.tvNacionalidad.text = user.nacionalidad ?: "N/A"

                        tabBinding.tvUid.text = user.uid ?: uid
                        tabBinding.tvDbId.text = user.id ?: "N/A"
                    } else {
                        Toast.makeText(this@HomeActivity, "Error al cargar perfil (${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tabBinding.pbPerfil.visibility = View.GONE
                    Toast.makeText(this@HomeActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showEditarPerfilDialog(tabBinding: LayoutTabPerfilBinding) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_editar_perfil, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.etEditNombre)
        val etApellido = dialogView.findViewById<EditText>(R.id.etEditApellido)
        val etTelefono = dialogView.findViewById<EditText>(R.id.etEditTelefono)
        val etNacionalidad = dialogView.findViewById<EditText>(R.id.etEditNacionalidad)

        AlertDialog.Builder(this)
            .setTitle("✏️ Editar Mi Perfil")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = etNombre.text.toString().trim()
                val apellido = etApellido.text.toString().trim()
                val telefono = etTelefono.text.toString().trim()
                val nacionalidad = etNacionalidad.text.toString().trim()

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val request = UpdateUserRequest(
                            nombre = nombre.ifEmpty { null },
                            apellido = apellido.ifEmpty { null },
                            telefono = telefono.ifEmpty { null },
                            nacionalidad = nacionalidad.ifEmpty { null }
                        )
                        val response = RetrofitClient.lapticonApi.updateUserProfile(uid, authHeader, request)
                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                Toast.makeText(this@HomeActivity, "¡Perfil actualizado!", Toast.LENGTH_SHORT).show()
                                loadPerfil(tabBinding)
                            } else {
                                Toast.makeText(this@HomeActivity, "Error al actualizar perfil (${response.code()})", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@HomeActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showBuscarRatingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_buscar_ratings, null)
        val etTargetUid = dialogView.findViewById<EditText>(R.id.etTargetUidSearch)
        etTargetUid.setText(uid) // Precargado con el propio UID por defecto

        AlertDialog.Builder(this)
            .setTitle("⭐ Consultar Reseñas")
            .setView(dialogView)
            .setPositiveButton("Consultar") { _, _ ->
                val searchUid = etTargetUid.text.toString().trim()
                if (searchUid.isEmpty()) {
                    Toast.makeText(this, "Ingresa un UID válido", Toast.LENGTH_SHORT).show()
                } else {
                    loadRatings(searchUid)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun loadRatings(targetUid: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.lapticonApi.getRatingsUsuario(targetUid)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val ratingData = response.body()!!
                        val total = ratingData.total ?: 0
                        val promedio = ratingData.promedio
                        val calificaciones = ratingData.calificaciones.orEmpty()

                        val sb = StringBuilder()
                        sb.append("📊 Total Reseñas: $total\n")
                        sb.append("⭐ Promedio: ${promedio?.let { String.format("%.1f / 5.0", it) } ?: "Sin promedio"}\n\n")

                        if (total == 0 || calificaciones.isEmpty()) {
                            sb.append("El usuario no tiene reseñas registradas actualmente.")
                        } else {
                            sb.append("--- Lista de Calificaciones ---\n\n")
                            calificaciones.forEachIndexed { index, r ->
                                sb.append("${index + 1}. ⭐ ${r.calificacion}/5  (${r.rolDestino ?: "viajero"})\n")
                                sb.append("💬 \"${r.comentario ?: "Sin comentario"}\"\n\n")
                            }
                        }

                        AlertDialog.Builder(this@HomeActivity)
                            .setTitle("⭐ Calificaciones ($targetUid)")
                            .setMessage(sb.toString())
                            .setPositiveButton("Cerrar", null)
                            .show()
                    } else {
                        Toast.makeText(this@HomeActivity, "Error al consultar reseñas (${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HomeActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showCalificarUsuarioDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_crear_rating, null)
        val etDestinoUid = dialogView.findViewById<EditText>(R.id.etTargetUid)
        val etScore = dialogView.findViewById<EditText>(R.id.etScore)
        val etComment = dialogView.findViewById<EditText>(R.id.etComment)

        AlertDialog.Builder(this)
            .setTitle("💬 Calificar a un Usuario")
            .setView(dialogView)
            .setPositiveButton("Enviar") { _, _ ->
                val targetUid = etDestinoUid.text.toString().trim()
                val score = etScore.text.toString().toIntOrNull() ?: 5
                val comment = etComment.text.toString().trim()

                if (targetUid.isEmpty() || comment.isEmpty()) {
                    Toast.makeText(this, "Completa el UID destino y tu comentario", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val request = CreateRatingRequest(targetUid, "viajero", score, comment)
                        val response = RetrofitClient.lapticonApi.createRating(authHeader, request)
                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                Toast.makeText(this@HomeActivity, "¡Calificación enviada!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@HomeActivity, "Error al enviar calificación (${response.code()})", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@HomeActivity, "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addEmptyMessageToContainer(container: LinearLayout, message: String) {
        val tv = TextView(this).apply {
            text = message
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            setPadding(12, 12, 12, 12)
        }
        container.addView(tv)
    }

    private fun logout() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
