package com.example.mlkitscanner

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Patterns
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.io.IOException

class MainActivity : AppCompatActivity() {
    lateinit var btn:AppCompatButton
    lateinit var textId:TextView
    private val CAMERA_CODE=1
    private val GALLERY_CODE=2
    lateinit var bitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn=findViewById(R.id.btn)
        textId=findViewById(R.id.text)
        btn.setOnClickListener {
            val builder=AlertDialog.Builder(this)
            builder.setTitle("Choose Image Source")
            builder.setItems(arrayOf<CharSequence>("Camera","Gallery"),DialogInterface.OnClickListener { dialogInterface, i ->
                when(i){
                    0 -> takePictureFromCamera()
                    1-> PickPictureFromGallery()
                }
            })
            builder.show()
        }
        textId.setOnClickListener {
            if(Patterns.WEB_URL.matcher(textId.text.toString()).matches()){
                val intent2=Intent(Intent.ACTION_VIEW,Uri.parse(textId.text.toString()))
                startActivity(intent2)
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==CAMERA_CODE && resultCode==RESULT_OK){
             bitmap=data?.extras?.get("data") as Bitmap
            ChooseScannerType()

        }
        if(requestCode==GALLERY_CODE && resultCode==RESULT_OK){
            val bitmapData=data?.data?.let {uri->
                try{
                    contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }catch (e:IOException){
                    e.printStackTrace()
                }
            }
            ChooseScannerType()
             bitmap=bitmapData as Bitmap

        }
    }

    private fun ChooseScannerType() {
        val builder2=AlertDialog.Builder(this)
        builder2.setTitle("Choose Scanner Type")
        builder2.setItems(arrayOf<CharSequence>("QR Code","Object Detection","Face Detection","Image Labeling"),DialogInterface.OnClickListener { dialogInterface, i ->
            when(i){
                0 -> BarcodeScan(bitmap)
                1->ObjectScan(bitmap)
                2->FaceScan(bitmap)
                3->ImgLabeling(bitmap)

            }
        })
        builder2.show()

    }




    private fun PickPictureFromGallery() {
        val intent3=Intent(MediaStore.ACTION_PICK_IMAGES,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent3.type="image/*"
        startActivityForResult(intent3,GALLERY_CODE)
    }
    private fun takePictureFromCamera() {
        val intent=Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if(intent.resolveActivity(packageManager)!=null){
            startActivityForResult(intent,CAMERA_CODE)
        }else{
            Toast.makeText(this,"NO camera found",Toast.LENGTH_SHORT).show()

        }
    }

    private fun BarcodeScan(bitmap: Bitmap){
        val img=InputImage.fromBitmap(bitmap,0)
        val scanner = BarcodeScanning.getClient()
        scanner.process(img).addOnSuccessListener { barcodes ->
            var str:String=""
            for (i in barcodes){
                 var rawValue=i.rawValue
                 str+=rawValue
             }
            textId.text=""
            textId.text = str
            if(str.isEmpty()) {
                textId.text="Not Found"
            }
        }.addOnFailureListener {
                Toast.makeText(this,"Something wrong",Toast.LENGTH_SHORT).show() }
    }
    private fun ObjectScan(bitmap: Bitmap) {
        val img=InputImage.fromBitmap(bitmap,0)
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .build()
        val objectDetector = ObjectDetection.getClient(options)
        objectDetector.process(img).addOnSuccessListener {detectedObjects->
            //val labelList= mutableListOf<String>()
            /*for (detectedObject in detectedObjects){
                val labels=detectedObject.labels
                for(label in labels){
                    labelList.add(label.text)
                }
            }
            textId.text="" 
            val result=if(labelList.isEmpty()){"Not Found"
                }
            else{
                labelList.joinToString(", ")
            }
            textId.text =result*/
            var result=""
            var i=1
            var ib=detectedObjects.size
            for (detectedObject in detectedObjects){
                val category=detectedObject.labels
                val confidence = detectedObject.trackingId
                val boundingBox = detectedObject.boundingBox

                result += "Object $i: " +
                        /*"\nCategory: $category" +
                        "\nConfidence: ${confidence?.times(100)}%" +*/
                        //"\nBoundingBox: $boundingBox\n\n"
                i++
            }
            //textId.text = result.trim()
            textId.text = ib.toString()
            if (detectedObjects.isEmpty()) {
                textId.text ="Not found"
            }

        }.addOnFailureListener {
            Toast.makeText(this,"Something wrong",Toast.LENGTH_SHORT).show() }
        }
    private fun FaceScan(bitmap: Bitmap) {
        val options=FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        val detector=FaceDetection.getClient(options)
        val image=InputImage.fromBitmap(bitmap,0)

        detector.process(image).addOnSuccessListener {faces->
            var result=""
            var i=1
            for(face in faces){

                result= "Face Number : $i" +
                        "\nSmile : ${face.smilingProbability?.times(100)}%" +
                        "\nLeft Eye Open : ${face.leftEyeOpenProbability?.times(100)}%" +
                        "\nRight Eye Open : ${face.rightEyeOpenProbability?.times(100)}%"
                i++
            }
            textId.text =result
            if(faces.isEmpty()){
                Toast.makeText(this, "NO FACE DETECTED", Toast.LENGTH_SHORT).show()
                textId.text ="Not found"

            }
        }.addOnFailureListener{
            Toast.makeText(this, "Something wrong", Toast.LENGTH_SHORT).show()
        }
    }

    private fun ImgLabeling(bitmap: Bitmap) {

        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.7f)
            .build()

        val labeler = ImageLabeling.getClient(options)

        val image = InputImage.fromBitmap(bitmap, 0)

        labeler.process(image).addOnSuccessListener {labels ->
                var result = ""
                for (label in labels) {
                    val labelText = label.text
                    val confidence = label.confidence
                    result += "Label: $labelText\n"
                }
                textId.text = result.trim()
                if (labels.isEmpty()) {
                    textId.text ="Not found"
                }
            }
            .addOnFailureListener { e ->
              e.printStackTrace()
            }
    }

}
