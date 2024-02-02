package com.example.dorstu

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.util.Log
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import com.aallam.openai.api.audio.SpeechRequest
import com.aallam.openai.api.audio.Voice
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.time.Duration.Companion.seconds

class PageLecture : AppCompatActivity() {



    private val scrollSpeed = 3 // Vitesse de défilement (1 pixel par défilement)
    private val scrollDelay = 90 // Délai entre chaque défilement (en millisecondes)
    private var handler: Handler? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var totalAudioDuration: Int = 0
    private var scrollAmountPerMillisecond: Float = 0f
    private lateinit var scrollView: ScrollView
    private lateinit var textView: TextView
    private lateinit var spannableStory: SpannableString


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page_lecture)

        handler = Handler(Looper.getMainLooper())


        scrollView = findViewById<ScrollView>(R.id.scrollView)
        textView = findViewById<TextView>(R.id.textView7) // Remplacer "textView7" par l'ID de votre TextViewD

        // Récupérez les données de l'intent
        val intent = intent
        val generatedStory = intent.getStringExtra("generatedStory")
        val audioFilePath = intent.getStringExtra("AudioFilePath")

        // Set the text to the TextView
        textView.text = generatedStory ?: "No story available."


        // Utilisez la variable generatedStory comme bon vous semble
        if (generatedStory != null) {
//            spannableStory = SpannableString(generatedStory)
            textView
            // Utilisez un Handler pour mettre à jour la position de défilement
            handler = Handler(Looper.getMainLooper())
            val runnable = object : Runnable {
                override fun run() {
                    // Faites défiler le ScrollView vers le bas
                    scrollView.scrollBy(0, scrollSpeed.toInt())

                    // Vérifiez si le défilement est terminé (arrivé en bas)
                    if (!isScrolledToBottom(scrollView, textView)) {
                        // Répétez la mise à jour après le délai
                        handler?.postDelayed(this, scrollDelay.toLong())
                    }
                }
            }

            // Ajoutez un délai initial avant de commencer le défilement
            val initialDelay = 5000L // 10 secondes en millisecondes
            handler?.postDelayed(runnable, initialDelay)
        }

        val playPauseButton = findViewById<Button>(R.id.btnPlayPause)
        playPauseButton.setOnClickListener {
            if (isPlaying) {
                pauseAudio()
            } else {
                startAudio(audioFilePath)
                startScrolling()
            }
        }
    }


    private fun isScrolledToBottom(scrollView: ScrollView, textView: TextView): Boolean {
        // Vérifiez si le ScrollView est au bas de son contenu
        return !scrollView.canScrollVertically(1)
    }


    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        // Arrêtez le défilement automatique lorsque l'activité est détruite
        handler?.removeCallbacksAndMessages(null)
        // Nettoyer l'intent
        intent.removeExtra("generatedStory")
        intent.removeExtra("AudioFilePath")
    }

    private fun startAudio(audioFilePath: String?) {
        if (audioFilePath != null) {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(audioFilePath)
                    prepare()
                    totalAudioDuration = duration
                    scrollAmountPerMillisecond = calculateScrollSpeed(totalAudioDuration, textView)
                }
            }
            mediaPlayer?.start()
            isPlaying = true


        }
    }


    private fun startScrolling() {
        val runnable = object : Runnable {
            override fun run() {
                if (mediaPlayer?.isPlaying == true) {
                    val currentPosition = mediaPlayer?.currentPosition ?: 0
                    val scrollPosition = (currentPosition * scrollAmountPerMillisecond).toInt()
                    scrollView.scrollTo(0, scrollPosition)
                    handler?.postDelayed(this, scrollDelay.toLong())
                }
            }
        }
        handler?.postDelayed(runnable, scrollDelay.toLong())
    }


    private fun pauseAudio() {
        mediaPlayer?.pause()
        isPlaying = false
    }

    private fun calculateScrollSpeed(audioDuration: Int, textView: TextView): Float {
        // Calculez la hauteur totale du texte et divisez-la par la durée de l'audio
        val totalTextHeight = textView.height
        return totalTextHeight.toFloat() / audioDuration
    }

}
