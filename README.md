# qrscanner
QR Scanner with On camera Preview.   
Go to QrCodeAnalyser.kt to customize onpreview
```
try {
            // Whenever reader fails to detect a QR code in image
            // it throws NotFoundException
            //create a file to write bitmap data
            var f = bitmapToFile(bmp, "Bfile")
            println(f)
            val result = reader.decode(binaryBitmap)
            System.out.println("FOUND")
            onQrCodesDetected(result)
        } catch (e: NotFoundException) {
            e.printStackTrace()
            System.out.println("NOT FOUND")
        }
  ```
