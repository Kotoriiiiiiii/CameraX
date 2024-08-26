package app.komatsuzaki.kotori.camerasampleapp

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Insets.add
import android.media.Image
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import app.komatsuzaki.kotori.camerasampleapp.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-mm-ss-SSS"
        private const val  REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                android.Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    // バインディングクラスの設定
    private lateinit var binding: ActivityMainBinding
    private  var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater).apply{
            setContentView(this.root)
        }


        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        binding.imageCaptureButton.setOnClickListener {
            takePhoto()
        }
    }
    private  fun startCamera() {
      val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

      cameraProviderFuture.addListener({
          val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

          val preview = Preview.Builder().build().also {
              it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
          }

          imageCapture = ImageCapture.Builder().build()

          val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

          try {
              cameraProvider.unbindAll()
              cameraProvider.bindToLifecycle(
                  this,cameraSelector, preview, imageCapture
              )

          }catch (exc: Exception) {
              Log.e("CameraX-sample", "Cameraの起動に失敗しました", exc)
          }
      }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        //imageCaptureにデータが入っていないときはメソッドを終了
        val imageCapture = imageCapture ?:return
        //保存するための変数nameを用意
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            //異なるAndroid.verでも画像が保存されるように書いている
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                //画像の保存に失敗した時
                override fun onError(exc: ImageCaptureException) {

                }
                //画像の保存に成功した時
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Toast.makeText(baseContext, "写真を保存しました", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == REQUEST_CODE_PERMISSIONS) {
                if (allPermissionsGranted()) {
                    startCamera()
                } else {
                    Toast.makeText(this, "パーミッションがないよ！", Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }

