package com.example.dorstu

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Spinner

import android.app.AlertDialog
import android.media.MediaPlayer
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.aallam.openai.api.audio.SpeechRequest
import com.aallam.openai.api.audio.Voice
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.RetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.HashMap;
import kotlin.time.Duration.Companion.seconds

class PageHistoire : AppCompatActivity() {

    private lateinit var ageInput: Spinner
    private lateinit var requestQueue: RequestQueue
    private lateinit var generatedStory: String
    private var customCharacter: String? = null
    private lateinit var characterSpinner: Spinner
    private lateinit var characterAdapter: ArrayAdapter<String>
    private val characterList: MutableList<String> = mutableListOf()
    private lateinit var locationSpinner: Spinner
    private lateinit var themeSpinner: Spinner
    private lateinit var locationAdapter: ArrayAdapter<String>
    private lateinit var themeAdapter: ArrayAdapter<String>
    private val locationList: MutableList<String> = mutableListOf()
    private val themeList: MutableList<String> = mutableListOf()
    private lateinit var progressBar: ProgressBar
    private var userId: String? = null
    private var profileName: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page_histoire)

        requestQueue = Volley.newRequestQueue(this)
        ageInput = findViewById(R.id.ageInput)



        profileName = intent.getStringExtra("profileName") ?: "DefaultProfile"
        userId = intent.getStringExtra("userId")

        val archiveButton = findViewById<Button>(R.id.archiveButton)
        archiveButton.setOnClickListener {
            val intent = Intent(this, PageArchives::class.java)
            intent.putExtra("ProfileName", profileName)
            startActivity(intent)
        }

        // Initialiser les listes avec les valeurs des ressources
        characterList.addAll(resources.getStringArray(R.array.character_options).toList())
        locationList.addAll(resources.getStringArray(R.array.location_options).toList())
        themeList.addAll(resources.getStringArray(R.array.theme_options).toList())


        val generateRandomStoryButton = findViewById<Button>(R.id.generateStoryButton)

        progressBar = findViewById(R.id.progressBar)


        // Associer les ressources aux Spinners
        val ageSpinner = findViewById<Spinner>(R.id.ageInput)
        characterSpinner = findViewById<Spinner>(R.id.spinner5)
        val locationSpinner = findViewById<Spinner>(R.id.spinner3)
        val themeSpinner = findViewById<Spinner>(R.id.spinner6)
        //val generatedStoryText = findViewById<TextView>(R.id.textView7)


// Créer des adaptateurs pour les spinners et ajouter des options
        val ageAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.age_options,
            android.R.layout.simple_spinner_item
        )
        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ageSpinner.adapter = ageAdapter

        // Créer des adaptateurs pour les Spinners
        characterAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, characterList)
        locationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locationList)
        themeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themeList)


        // Configurer les adaptateurs pour les Spinners
        characterSpinner.adapter = characterAdapter
        locationSpinner.adapter = locationAdapter
        themeSpinner.adapter = themeAdapter

        characterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                val selectedOption = parentView.getItemAtPosition(position).toString()
                if (selectedOption == "Personnalisé") {
                    showCustomInputDialog(characterList, characterAdapter, characterSpinner)
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                // Ne rien faire si rien n'est sélectionné
            }
        }

        locationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                val selectedOption = parentView.getItemAtPosition(position).toString()
                if (selectedOption == "Personnalisé") {
                    showCustomInputDialog(locationList, locationAdapter, locationSpinner)
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                // Ne rien faire si rien n'est sélectionné
            }
        }

        themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                val selectedOption = parentView.getItemAtPosition(position).toString()
                if (selectedOption == "Personnalisé") {
                    showCustomInputDialog(themeList, themeAdapter, themeSpinner)
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                // Ne rien faire si rien n'est sélectionné
            }
        }


        generateRandomStoryButton.setOnClickListener(View.OnClickListener {
            // Récupérez l'âge, le personnage, le thème et le lieu à partir de vos spinners ou d'autres champs
            val age = ageInput.selectedItem.toString().trim()
            val character = characterSpinner.selectedItem.toString()
            val theme = themeSpinner.selectedItem.toString()
            val location = locationSpinner.selectedItem.toString()

            // Appelez la fonction pour générer l'histoire personnalisée avec les données récupérées
            generateCustomStory(age, character, theme, location)
            Toast.makeText(this, "Veuillez patientez svp ,génération d'une histoire...", Toast.LENGTH_LONG).show()

        })

    }

    private fun generateCustomStory(
        age: String,
        character: String,
        theme: String,
        location: String
    ) {
        // Use the variables only if they are not 'Aléatoire'
        val characterPhrase = if (character != "Aléatoire") "avec un personnage principal comme $character" else ""
        val themePhrase = if (theme != "Aléatoire") "sur le thème de $theme" else ""
        val locationPhrase = if (location != "Aléatoire") "se déroulant à $location" else ""

        // Construct the message
        val message = buildString {
            append("Générer une histoire aléatoire pour un enfant de $age ans")
            if (characterPhrase.isNotEmpty()) append(", $characterPhrase")
            if (themePhrase.isNotEmpty()) append(", $themePhrase")
            if (locationPhrase.isNotEmpty()) append(", $locationPhrase")
            append(". Le titre de l'histoire sera indiqué au début, TOUJOURS suivi d'une virgule EXCLUSIVEMENT et SANS GUILLEMETS.")
        }

        generateStory(message)
    }

    private fun generateStory(message: String) {

        // Afficher la ProgressBar
        progressBar.visibility = View.VISIBLE

        // Créez le JSON de la requête avec le message personnalisé
        val jsonBody = JSONObject()
        try {
            jsonBody.put("model", "gpt-3.5-turbo")
            jsonBody.put(
                "messages", JSONArray()
                    .put(
                        JSONObject().put("role", "system")
                            .put("content", "You are a helpful assistant.")
                    )
                    .put(JSONObject().put("role", "user").put("content", message))
            )
            // jsonBody.put("max_tokens", 50)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val jsonObjectRequest: JsonObjectRequest = object : JsonObjectRequest(
            Request.Method.POST,
            "https://api.openai.com/v1/chat/completions",
            jsonBody,
            Response.Listener { response ->
                progressBar.visibility = View.GONE
                try {
                    val choicesArray = response.getJSONArray("choices")
                    if (choicesArray.length() > 0) {
                        val firstChoice = choicesArray.getJSONObject(0)
                        generatedStory = firstChoice.getJSONObject("message").getString("content")

                        // Extract the title from the generated story
                        val storyTitle = extractTitle(generatedStory)

                        generateTextToSpeech(generatedStory)

                        // Save the story with the extracted title to Firestore
                        val story = Story(
                            title = storyTitle, // Use the extracted title
                            content = generatedStory
                        )
                        saveStoryToFirestore(userId, profileName, story)

                        // Redirect to PageLecture with the generated story content
                        redirectToPageLecture()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error ->
                // Error handling code
            }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String>? {
                val header: MutableMap<String, String> = HashMap()
                header["Content-Type"] = "application/json"
                header["Authorization"] =
                    "Bearer API KEY" ///////////////////////////////////////////////////////// API KEY
                return header
            }

        }

        val intTimeoutPeriod = 60000 // 60 seconds timeout duration defined

        val retryPolicy: RetryPolicy = DefaultRetryPolicy(
            intTimeoutPeriod,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        jsonObjectRequest.setRetryPolicy(retryPolicy)

        requestQueue.add(jsonObjectRequest)

    }

    fun generateTextToSpeech(text: String) {
        lifecycleScope.launch {
            try {
                val openai = OpenAI(
                    token = "API KEY", ////////////////////////////////////////////////////////////////////////////////////////////API KEY
                    timeout = Timeout(socket = 60.seconds),
                )

                val rawAudio = openai.speech(
                    request = SpeechRequest(
                        model = ModelId("tts-1"),
                        input = text,
                        voice = Voice.Nova,
                    )
                )

                val tempMp3 = File.createTempFile("temp_audio", ".mp3", cacheDir)
                FileOutputStream(tempMp3).use { fos ->
                    fos.write(rawAudio)
                }
                val audioFilePath = tempMp3.absolutePath
                Log.d("PageHistoire", "Audio file path: ${tempMp3.absolutePath}")

                withContext(Dispatchers.Main) {
                    val intent = Intent(this@PageHistoire, PageLecture::class.java).apply {
                        putExtra("AudioFilePath", audioFilePath)
                        putExtra("generatedStory", text)
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {
                // Handle exceptions, perhaps show a user-friendly error message
                Log.e("TextToSpeechError", "Error generating speech: ${e.message}")
            }
        }
    }

    private fun extractTitle(story: String): String {
        return story.substringBefore(",")
    }



    data class Story(
        val title: String,
        val content: String,
        val savedDate: Long = System.currentTimeMillis()
    )

    private fun saveStoryToFirestore(userId: String?, profileName: String, story: Story) {
        val db = FirebaseFirestore.getInstance()
        userId?.let { uid ->
            db.collection("users").document(uid)
                .collection("archives").document(profileName)
                .collection("stories").add(story)
                .addOnSuccessListener {
                    Log.d("Firestore", "Story saved!")
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error saving the story", e)
                }
        }
    }

    private fun redirectToPageLecture() {
        val story = Story(
            title = "Generated Story",
            content = generatedStory
        )

        saveStoryToFirestore(userId, profileName, story)

        val intent = Intent(this, PageLecture::class.java)
        intent.putExtra("generatedStory", generatedStory)
        intent.putExtra("profileName", profileName)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }
    private fun showCustomInputDialog(
        list: MutableList<String>,
        adapter: ArrayAdapter<String>,
        spinner: Spinner
    ) {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Saisissez une valeur personnalisée")

        val inputEditText = EditText(this)
        alertDialog.setView(inputEditText)

        alertDialog.setPositiveButton("OK") { dialog, which ->
            val customValue = inputEditText.text.toString()

            // Ajouter la valeur personnalisée à la liste appropriée et notifier l'adaptateur correspondant
            list.add(customValue)
            adapter.notifyDataSetChanged()

            // Sélectionner la valeur personnalisée dans le Spinner approprié
            spinner.setSelection(adapter.count - 1)
        }

        alertDialog.setNegativeButton("Annuler") { dialog, which ->
            dialog.dismiss()
        }

        alertDialog.show()
    }

}
