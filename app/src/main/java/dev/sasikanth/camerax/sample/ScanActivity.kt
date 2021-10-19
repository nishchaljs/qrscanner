package dev.sasikanth.camerax.sample

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import okhttp3.RequestBody.Companion.toRequestBody
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_scan.*
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Part
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import android.provider.MediaStore

import android.graphics.Bitmap
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.io.ByteArrayOutputStream


// This is an arbitrary number we are using to keep track of the permission
// request. Where an app has multiple context for requesting permission,
// this can help differentiate the different contexts.
private const val REQUEST_CODE_PERMISSIONS = 10

// This is an array of all the permission specified in the manifest.
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class ScanActivity : AppCompatActivity() {



    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        if (! Python.isStarted()) {
            Python.start(AndroidPlatform(this@ScanActivity))
        }

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // Request camera permissions
        if (allPermissionsGranted()) {
            cameraProvider()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun cameraProvider() {
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            startCamera(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startCamera(cameraProvider: ProcessCameraProvider) {
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val preview = Preview.Builder().apply {
            setTargetResolution(Size(previewView.width, previewView.height))
        }.build()



        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(previewView.width, previewView.height))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, QrCodeAnalyzer { qrResult ->
                    previewView.post {
                        Log.d("QRCodeAnalyzer", "Barcode scanned: $qrResult")


                        val bitmap =
                            MediaStore.Images.Media.getBitmap(this.contentResolver, File(qrResult).toUri() )

                        image.setImageBitmap(null)
                        image.setImageBitmap(bitmap)
                        val intArray = IntArray(bitmap.getWidth() * bitmap.getHeight())
//                //copy pixel data from the Bitmap into the 'intArray' array
//                //copy pixel data from the Bitmap into the 'intArray' array
                bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight())
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)

                val source: LuminanceSource =
                    RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                        try {
                                            val result = reader.decode(binaryBitmap)
                             println("FOUND in QR$result")
                             Toast.makeText(this, result.text, Toast.LENGTH_LONG).show()
                            finish()
                        }
                        catch (e: NotFoundException) {
                            e.printStackTrace()
                            println("NOT FOUND")
                        }
                    }
                })
            }

        cameraProvider.unbindAll()

        val camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
        preview.setSurfaceProvider(previewView.createSurfaceProvider(camera.cameraInfo))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                cameraProvider()
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

    private val reader = MultiFormatReader().apply {
        val map = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to arrayListOf(BarcodeFormat.QR_CODE)
        )
        setHints(map)
    }

    private fun uploadImage(imageUri: Uri, progressDialog: ProgressDialog) {
        val service: FileUploadService =
            RetrofitClient.getClient().create(FileUploadService::class.java)
        val imageFile = File(imageUri.path);
        val imageToBeSent = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", imageFile.name, imageToBeSent)
//        val body: Part = createFormData.createFormData(
//            "image",
//            imageFile.name,
//            imageToBeSent
//        )
//        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
//        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            OnGPS()
//        } else {
//            getLocation()
//        }
//        Log.i("Location", "uploadImage: $latitude $longitude")
//        //String latitude = "12.956976457086279";
//        val lat: RequestBody = RequestBody.create(
//            latitude,
//            MultipartBody.FORM
//        )
//
//        //String longitude = "77.59479215742788";
//        val lon: RequestBody = RequestBody.create(
//            longitude,
//            MultipartBody.FORM
//        )

        //  Call<ResponseBody> call = service.upload(body,lat,lon);
        val call = service.upload(body)
        call!!.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Log.i(
                    "upload harish code :" + response.headers(),
                    "success"
                ) //response.body().toString()
                progressDialog.dismiss()

//                resCode.setText(response.code());
//                Toast.makeText(QRActivity.this, response.toString(),Toast.LENGTH_LONG).show();
                var jsonData: String? = null
                try {
                    jsonData = response.body()!!.string()
                    val Jobject = JSONObject(jsonData)
                    val info = Jobject.getJSONObject("info")
                    println(info)
                    val name = info["name"] as String
                    val uid = info["uid"] as String
                    println("Responseee$name")
                    //                    if(decode.matches("None")){
//                        Toast.makeText(QRActivity.this, "Try Again",Toast.LENGTH_LONG).show();
//                        showres(false);
//                    }
//                    else{
//
                    //  Toast.makeText(QRActivity.this, decode,Toast.LENGTH_LONG).show();
                    //next api call
                    //showres(true, name, info["mrp"].toString(), uid)
                    //
//                    }
                    finish()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@ScanActivity,
                        "Failed to decode, Try Again!",
                        Toast.LENGTH_LONG
                    ).show()
                   // showres(false, "", "", "")
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@ScanActivity,
                        "Failed to decode, Try Again!",
                        Toast.LENGTH_LONG
                    ).show()
                  //  showres(false, "", "", "")
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@ScanActivity,
                        "Failed to decode, Try Again!",
                        Toast.LENGTH_LONG
                    ).show()
                  //  showres(false, "", "", "")
                }
                println(call)
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                progressDialog.dismiss()
                Log.i("U error harish code:", "Hi hello " + t.message)
                //                resCode.setText("failure");
                Toast.makeText(this@ScanActivity, "failure to connect server", Toast.LENGTH_LONG)
                    .show()
            }
        })
    }
}


