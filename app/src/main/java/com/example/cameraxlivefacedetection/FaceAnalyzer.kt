package com.example.cameraxlivefacedetection

import android.annotation.SuppressLint
import android.media.Image
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.Lifecycle
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceAnalyzer(lifecycle: Lifecycle, private val overlay: Overlay) : ImageAnalysis.Analyzer {
    //ML kit face detector builder class
    private val options = FaceDetectorOptions.Builder()
        //Defines options to control accuracy / speed trade-offs in performing face detection
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        //Defines options to enable face landmarks or not.
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        //Sets whether to detect no contours or all contours as defined in
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        //Indicates whether to run additional classifiers for characterizing attributes such as "smiling" and "eyes open"
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        //Sets the smallest desired face size, expressed as a proportion of the width of the head to the image width
        .setMinFaceSize(0.15f)
        //.enableTracking() //disable when contour is enable
        .build()

    //Gets an instance of FaceDetector that detects faces in a supplied image with a default FaceDetectorOptions.
    //Here we pass our Builder Class
    private val detector = FaceDetection.getClient(options)

    //the init block is run at every time the class is instantiated
    init {
        //add the detector in lifecycle observer to properly close it when it's no longer needed.
        lifecycle.addObserver(detector)
    }

    // ImageProxy = An image proxy which has a similar interface as Image.
    override fun analyze(imageProxy: ImageProxy) {
        //Sets the dimensions for preview pictures
        overlay.setPreviewSize(Size(imageProxy.width, imageProxy.height))
        detectFaces(imageProxy)
    }

    // Face = Represents a face detected by FaceDetector.
    //Listener called when a Task completes successfully.
    private val successListener = OnSuccessListener<List<Face>> { faces ->
        Log.d(TAG, "Number of face detected: " + faces.size)
        overlay.setFaces(faces)
    }

    //Listener called when a Task completes unsuccessfully.
    private val failureListener = OnFailureListener { e ->
        Log.e(TAG, "Face analysis failure.", e)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun detectFaces(imageProxy: ImageProxy) {
        //Creates an InputImage from an Image object, e.g., what you obtained from your hardware camera
        // and we have to supplement this fromMediaImage(Image image, int rotationDegrees)
        val image = InputImage.fromMediaImage(
            //Returns the android Image.
            imageProxy.image as Image,
            //here the imageInfo is the Metadata for an image.
            //and rotationDegrees returns the rotation needed to transform the image to the correct orientation.
            imageProxy.imageInfo.rotationDegrees
        )
        //helps process our face detection
        detector.process(image)
            .addOnSuccessListener(successListener)//here we set the boxes of the detected faces
            .addOnFailureListener(failureListener)
            //when the task is completed
            .addOnCompleteListener {
                //Closes the underlying Image.
                imageProxy.close()
            }
    }

    companion object {
        private const val TAG = "FaceAnalyzer"
    }
}