package com.example.lab_week_08

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker
import com.example.lab_week_08.worker.ThirdWorker

class MainActivity : AppCompatActivity() {

    private val workManager by lazy {
        WorkManager.getInstance(this.applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        // --- PERUBAHAN UTAMA DIMULAI DARI SINI ---

        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val id = "001"

        // 1. Definisikan semua request seperti sebelumnya
        val firstRequest = OneTimeWorkRequest.Builder(FirstWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(FirstWorker.INPUT_DATA_ID, id))
            .build()

        val secondRequest = OneTimeWorkRequest.Builder(SecondWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(SecondWorker.INPUT_DATA_ID, id))
            .build()

        val thirdRequest = OneTimeWorkRequest.Builder(ThirdWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(ThirdWorker.INPUT_DATA_ID, id)) // Bug diperbaiki
            .build()

        // 2. Mulai rantai kerja PERTAMA (hanya worker 1 dan 2)
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .enqueue()

        // 3. Atur Observer secara berantai
        workManager.getWorkInfoByIdLiveData(firstRequest.id)
            .observe(this, Observer { info ->
                if (info != null && info.state.isFinished) {
                    showResult("First process is done")
                }
            })

        workManager.getWorkInfoByIdLiveData(secondRequest.id)
            .observe(this, Observer { info ->
                if (info != null && info.state.isFinished) {
                    showResult("Second process is done")
                    // Setelah worker 2 selesai, jalankan Service 1 DAN worker 3
                    showResult("Starting Notification Service...")
                    launchNotificationService()

                    showResult("Starting Third Process...")
                    workManager.enqueue(thirdRequest)

                    // Hapus observer ini agar tidak terpanggil lagi
                    workManager.getWorkInfoByIdLiveData(secondRequest.id).removeObservers(this)
                }
            })

        workManager.getWorkInfoByIdLiveData(thirdRequest.id)
            .observe(this, Observer { info ->
                if (info != null && info.state.isFinished) {
                    showResult("Third process is done")
                    // Setelah worker 3 selesai, jalankan Service 2
                    showResult("Starting Second Notification Service...")
                    launchSecondNotificationService()

                    // Hapus observer ini agar tidak terpanggil lagi
                    workManager.getWorkInfoByIdLiveData(thirdRequest.id).removeObservers(this)
                }
            })
    }

    private fun getIdInputData(idKey: String, idValue: String) =
        Data.Builder()
            .putString(idKey, idValue)
            .build()

    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun launchNotificationService() {
        NotificationService.trackingCompletion.observe(this) { idValue ->
            showResult("Process for Notification Channel ID $idValue is done!")
        }
        val serviceIntent = Intent(this, NotificationService::class.java).apply {
            putExtra(EXTRA_ID, "001")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun launchSecondNotificationService() {
        SecondNotificationService.trackingCompletion.observe(this) { idValue ->
            showResult("Process for Notification Channel ID $idValue is done!")
        }
        val serviceIntent = Intent(this, SecondNotificationService::class.java).apply {
            putExtra(EXTRA_ID, "002") // Sebaiknya gunakan ID berbeda untuk service kedua
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    companion object {
        const val EXTRA_ID = "Id"
    }
}