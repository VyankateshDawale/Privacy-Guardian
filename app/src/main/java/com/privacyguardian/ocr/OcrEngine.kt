package com.privacyguardian.ocr

import android.graphics.Bitmap
import android.net.Uri

interface OcrEngine {
    suspend fun recognize(bitmap: Bitmap): Result<OcrResult>
    suspend fun recognize(uri: Uri, loadBitmap: suspend (Uri) -> Bitmap?): Result<OcrResult>
}
