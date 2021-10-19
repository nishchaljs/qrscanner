package dev.sasikanth.camerax.sample

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent.getActivity
import android.content.pm.PackageManager
import android.graphics.ImageFormat.*
import android.os.Build
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import android.graphics.BitmapFactory

import android.graphics.Rect

import android.graphics.ImageFormat

import android.graphics.YuvImage

import android.graphics.Bitmap

import android.os.Environment.getExternalStorageDirectory
import android.util.Base64
import androidx.core.app.ActivityCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONObject
import kotlin.coroutines.coroutineContext
import dev.sasikanth.camerax.sample.ScanActivity as Sca
import android.R
import android.content.Context

import android.widget.TextView

import android.view.View

import android.view.LayoutInflater
import android.provider.MediaStore

import android.net.Uri

import android.R.attr.bitmap
import android.app.ProgressDialog
import android.widget.Toast
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*


private fun ByteBuffer.toByteArray(): ByteArray {
    rewind()
    val data = ByteArray(remaining())
    get(data)
    return data
}

class QrCodeAnalyzer(

    private val onQrCodesDetected: (qrCode: String) -> Unit
// variable to hold context

//save the context recievied via constructor in a local variable

) : ImageAnalysis.Analyzer {

    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // variable to hold context
    private var context: Context? = null

//save the context recievied via constructor in a local variable

    //save the context recievied via constructor in a local variable
    fun YourNonActivityClass(context: Context) {
        this.context = context
    }
    fun verifyStoragePermissions(activity: Activity) {
        // Check if we have write permission
        val permission = ActivityCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }


    private val yuvFormats = mutableListOf(YUV_420_888)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            yuvFormats.addAll(listOf(YUV_422_888, YUV_444_888))
        }

    }

    private val reader = MultiFormatReader().apply {
        val map = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to arrayListOf(BarcodeFormat.QR_CODE)
        )
        setHints(map)
    }
    private fun ImageProxy.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer // Y
        val uBuffer = planes[1].buffer // U
        val vBuffer = planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
        outputStream().use { out ->
            bitmap.compress(format, quality, out)
            out.flush()
        }
    }


    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(image: ImageProxy) {
        // We are using YUV format because, ImageProxy internally uses ImageReader to get the image
        // by default ImageReader uses YUV format unless changed.
        if (image.format !in yuvFormats) {
            Log.e("QRCodeAnalyzer", "Expected YUV, now = ${image.format}")
            return
        }
        val bmp1: Bitmap = image.toBitmap()
        var bmp: Bitmap = bmp1.copy(bmp1.getConfig(), true)

        val intArray = IntArray(bmp.getWidth() * bmp.getHeight())
        //copy pixel data from the Bitmap into the 'intArray' array
        //copy pixel data from the Bitmap into the 'intArray' array
        bmp.getPixels(intArray, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight())
        val stream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 90, stream)
        val barray = stream.toByteArray()

        val source: LuminanceSource =
            RGBLuminanceSource(bmp.getWidth(), bmp.getHeight(), intArray)


        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        try {

            val py = Python.getInstance()
            val module = py.getModule("main")
            val results = module.callAttr("process_image",barray)
            if (results.toString() == "QR detection fail"){

                val result = reader.decode(binaryBitmap)
                println("FOUND without detection")
               // onQrCodesDetected(results)
            }
            else{
                println("HAHA WORKS ${(results.toString())}")
                onQrCodesDetected(results.toString())


            }}



         catch (e: NotFoundException) {
            e.printStackTrace()
            System.out.println("NOT FOUND")

        }
        catch (e: Exception){
            e.printStackTrace()
        }
        image.close()
    }


}
