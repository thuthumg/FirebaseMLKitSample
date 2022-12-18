package com.padcmyanmar.ttm.firebasemlkitsample

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.padcmyanmar.ttm.firebasemlkitsample.utils.loadBitMapFromUri
import com.padcmyanmar.ttm.firebasemlkitsample.utils.scaleToRatio
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException


class MainActivity : AppCompatActivity() {
    val PICK_IMAGE_REQUEST = 1111

    var mChosenImageBitmap: Bitmap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        callClickListener()
    }

    private fun callClickListener() {
        btnTakePhoto.setOnClickListener {
            openGallery()
        }
       btnFindText.setOnClickListener {
           detectTextAndUpdateUI()
       }

        btnFindFace.setOnClickListener {
            detectFaceAndDrawRectangle()
        }
    }

    private fun detectFaceAndDrawRectangle() {
        mChosenImageBitmap?.let {

            //1
            val inputImage = InputImage.fromBitmap(it,0)

            //2
            val options  = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build()

            //3
            val detector =  FaceDetection.getClient(options)

            detector.process(inputImage)
                .addOnSuccessListener { faces->
                    drawRectangleOnFace(it,faces)
                    ivPhoto.setImageBitmap(mChosenImageBitmap)
                }
                .addOnFailureListener { e ->
                    Snackbar.make(window.decorView,
                        e.localizedMessage ?: "Cannot detect face",Snackbar.LENGTH_SHORT).show()
                }
        }
    }

    private fun drawRectangleOnFace(it: Bitmap, faces: List<Face>) {

        val imageCanvas = Canvas(it)
        val paint = Paint()
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 9.0f

        faces.firstOrNull()?.boundingBox?.let {
            boundingBox -> imageCanvas.drawRect(boundingBox, paint)
        }
    }

    private fun detectTextAndUpdateUI() {
       mChosenImageBitmap?.let {
           val inputImage = InputImage.fromBitmap(it,0)
           val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)


           recognizer.process(inputImage)
               .addOnSuccessListener { visionText ->

                   val detectedTextString = java.lang.StringBuilder("")
                   visionText.textBlocks.forEach { textBlock ->
                       detectedTextString.append("${textBlock.text}\n")
                   }
                   tvDetectedTexts.text = ""
                   tvDetectedTexts.text = detectedTextString.toString()

                   //Draw bounding boxes
                   val paint = Paint()
                   paint.color = Color.GREEN
                   paint.style = Paint.Style.STROKE
                   paint.strokeWidth = 2.0f

                   visionText.textBlocks.forEach { block : Text.TextBlock->
                       val imageCanvas = Canvas(it)
                       block.boundingBox?.let { boundingBox : Rect ->
                           imageCanvas.drawRect(boundingBox,paint)
                       }
                   }
               }.addOnFailureListener { e: Exception->

                   Snackbar.make(window.decorView,   e.localizedMessage ?: "Cannot detect text", Snackbar.LENGTH_LONG)
                   .show()
               }

       }
    }

    fun openGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "Select Picture"),
            PICK_IMAGE_REQUEST
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {


            /*val filePath = data?.data
            try {

                filePath?.let {
                    if (Build.VERSION.SDK_INT >= 29) {
                        val source: ImageDecoder.Source =
                            ImageDecoder.createSource(this.contentResolver, filePath)

                        mChosenImageBitmap = ImageDecoder.decodeBitmap(source)
                        ivPhoto.setImageBitmap(mChosenImageBitmap)



                    } else {
                        mChosenImageBitmap = MediaStore.Images.Media.getBitmap(
                            applicationContext.contentResolver, filePath
                        )
                        ivPhoto.setImageBitmap(mChosenImageBitmap)

                    }
                }

            } catch (e: IOException) {
                e.printStackTrace()
            }*/


            val imageUri = data?.data
            imageUri?.let { image->
                Observable.just(image)
                    .map { it.loadBitMapFromUri(applicationContext) }
                    .map { it.scaleToRatio(0.35) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        mChosenImageBitmap = it
                        ivPhoto.setImageBitmap(mChosenImageBitmap)
                    }

            }

        }
    }
}