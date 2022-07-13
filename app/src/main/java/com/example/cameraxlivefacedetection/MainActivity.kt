package com.example.cameraxlivefacedetection

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    //calling the preview view instance
    private lateinit var viewFinder: PreviewView

    //calling the overlay instance
    private lateinit var overlay: Overlay

    //this method creates an executor that executes a single task at a time
    private var cameraExecutor = Executors.newSingleThreadExecutor()

    //start the onCreate function
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //calling the Preview View
        viewFinder = findViewById(R.id.viewFinder)
        //Now we call the overlay context, context - you call it to get information regarding another part of your program
        overlay = Overlay(this)
        //LayoutParams are used by views to tell their parents how they want to be laid out
        val layoutOverlay = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        //now we add the content view
        this.addContentView(overlay, layoutOverlay) //addContentView(view,params)
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    //start and build CameraX
    private fun startCamera() {
        //A singleton which can be used to bind the lifecycle of cameras to any LifecycleOwner within an application's process
        //LifecycleOwner - A class that has an Android lifecycle. These events can be used by custom components to handle lifecycle changes without implementing any code inside the Activity or the Fragment.
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        //Registers a listener to be run  on the given executor.
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // Select back camera as a default. A set of requirements and priorities used to select a camera or return a filtered set of cameras.
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            // Preview UseCase Builder
            val previewUseCase = Preview.Builder()
                .build()
                .also {
                    //A interface implemented by the application to provide a Surface for Preview.
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }
            // Build Image Analysis
            val analysisUseCase = ImageAnalysis.Builder()
                .build()
                .also {
                    // We set up the interface to analyze
                    //lifecycle = returns the lifecycle of the provider
                    it.setAnalyzer(cameraExecutor, FaceAnalyzer(lifecycle, overlay)) //.setAnalyzer(Executor,Analyzer)
                }
            
            try {
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to cameraX. Binds the collection of UseCase to a LifecycleOwner.
                cameraProvider.bindToLifecycle(
                    this, //lifecycleOwner
                    cameraSelector,
                    previewUseCase,
                    analysisUseCase
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this)) //Return an Executor that will run enqueued tasks on the main thread associated with this context.
    }

    //helps grant permissions
    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }


    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
        )
    }
}