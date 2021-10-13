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
import java.io.ByteArrayOutputStream
import android.graphics.BitmapFactory

import android.graphics.Rect

import android.graphics.ImageFormat

import android.graphics.YuvImage

import android.graphics.Bitmap
import java.io.FileOutputStream

import java.io.File

import android.os.Environment.getExternalStorageDirectory
import androidx.core.app.ActivityCompat
import java.io.OutputStream
import kotlin.coroutines.coroutineContext
import dev.sasikanth.camerax.sample.ScanActivity as Sca


private fun ByteBuffer.toByteArray(): ByteArray {
    rewind()
    val data = ByteArray(remaining())
    get(data)
    return data
}

class QrCodeAnalyzer(

    private val onQrCodesDetected: (qrCode: Result) -> Unit

) : ImageAnalysis.Analyzer {

    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
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
//    fun saveMediaToStorage(bitmap: Bitmap) {
//        //Generating a file name
//        val filename = "${System.currentTimeMillis()}.jpg"
//
//        //Output stream
//        var fos: OutputStream? = null
//
//        //For devices running android >= Q
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            //getting the contentResolver
//
//            context.contentResolver?.also { resolver ->
//
//                //Content resolver will process the contentvalues
//                val contentValues = ContentValues().apply {
//
//                    //putting file information in content values
//                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
//                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
//                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
//                }
//
//                //Inserting the contentValues to contentResolver and getting the Uri
//                val imageUri: Uri? =
//                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
//
//                //Opening an outputstream with the Uri that we got
//                fos = imageUri?.let { resolver.openOutputStream(it) }
//            }
//        } else {
//            //These for devices running on android < Q
//            //So I don't think an explanation is needed here
//            val imagesDir =
//                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
//            val image = File(imagesDir, filename)
//            fos = FileOutputStream(image)
//        }
//
//        fos?.use {
//            //Finally writing the bitmap to the output stream that we opened
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
//            context?.toast("Saved to Photos")
//        }
//    }

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
        val bmp: Bitmap = bmp1.copy(bmp1.getConfig(), true)

        //saveImage(bmp,"see")


//        val imageView = findViewById<View>(R.id.prev) as ImageView

//        val data = image.planes[0].buffer.toByteArray()
//        val bmp = Bitmap.createBitmap(image.height, image.width, Bitmap.Config.ALPHA_8)
//        val buffer = ByteBuffer.wrap(data)
//
//        //buffer.rewind()
//        bmp.copyPixelsFromBuffer(buffer)
//        val image_: Image? = image.getImage()
//        val bmp = image_?.let { toBitmap(it) }
        // get image's width and height

        // convert to greyscale
        // get image's width and height
//        val width: Int = bmp.getWidth()
//        val height: Int = bmp.getHeight()
//        var flag = 1
//        var invertedPixel = 0
//
//        for (y in 0 until height) {
//            if (y % (height / 2) == 0) {
//                flag = if (flag == 0) {
//                    1
//                } else 0
//            }
//            for (x in 0 until width) {
//                // Here (x,y)denotes the coordinate of image
//                // for modifying the pixel value.
//                var p: Int = bmp.getPixel(x, y)
//                val a = p shr 24 and 0xff
//                val r = p shr 16 and 0xff
//                val g = p shr 8 and 0xff
//                val b = p and 0xff
//                // calculate average
//                val avg = (r + g + b) / 3
//
//                // replace RGB value with avg
//                p = a shl 24 or (avg shl 16) or (avg shl 8) or avg
//                invertedPixel = if (flag == 0) {
//                    0xFFFFFF - p or -0x1000000
//                } else {
//                    p
//                }
//                bmp.setPixel(x, y, invertedPixel)
//            }
//        }
//


        val intArray = IntArray(bmp.getWidth() * bmp.getHeight())
        //copy pixel data from the Bitmap into the 'intArray' array
        //copy pixel data from the Bitmap into the 'intArray' array
        bmp.getPixels(intArray, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight())

        val source: LuminanceSource =
            RGBLuminanceSource(bmp.getWidth(), bmp.getHeight(), intArray)

//        val stream = ByteArrayOutputStream()
//        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
//        val byteArray = stream.toByteArray()
//        bmp.recycle()


//        val source = PlanarYUVLuminanceSource(
//            byteArray,
//            image.width,
//            image.height,
//            0,
//            0,
//            image.width,
//            image.height,
//            false
//        )

        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        try {
            // Whenever reader fails to detect a QR code in image
            // it throws NotFoundException
            //create a file to write bitmap data
//            var f = bitmapToFile(bmp, "Bfile")
//            println(f)
            val result = reader.decode(binaryBitmap)
            System.out.println("FOUND")
            onQrCodesDetected(result)
        } catch (e: NotFoundException) {
            e.printStackTrace()
            System.out.println("NOT FOUND")
        }
//        catch (e: ArrayIndexOutOfBoundsException){
//            e.printStackTrace()
//        }
        image.close()
    }

    fun bitmapToFile(bitmap: Bitmap, fileNameToSave: String): File? { // File name like "image.png"
        //create a file to write bitmap data
        var file: File? = null
        return try {




            file = File(getExternalStorageDirectory().toString() + File.separator + fileNameToSave)
            file.createNewFile()

            //Convert bitmap to byte array
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos) // YOU can also save it in JPEG
            val bitmapdata = bos.toByteArray()

            //write the bytes in file
            val fos = FileOutputStream(file)
            fos.write(bitmapdata)
            fos.flush()
            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            file // it will return null
        }
    }



}
