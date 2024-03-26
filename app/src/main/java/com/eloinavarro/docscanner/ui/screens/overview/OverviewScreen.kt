package com.eloinavarro.docscanner.ui.screens.overview

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.eloinavarro.docscanner.R
import com.eloinavarro.docscanner.domain.ScannedDocument
import com.eloinavarro.docscanner.injection.Provider
import com.eloinavarro.docscanner.ui.common.FancyText
import com.eloinavarro.docscanner.ui.common.Screen
import com.eloinavarro.docscanner.ui.common.TextDivider
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream


@Composable
fun OverviewScreen(
    onItemClick: (ScannedDocument) -> Unit,
    overviewViewModel: OverviewViewModel = Provider.provideOverviewViewModel()
) {
    var scannedDocuments by remember {
        mutableStateOf<List<ScannedDocument>>(emptyList())
    }
    val context = LocalContext.current
    val options = GmsDocumentScannerOptions.Builder()
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .setGalleryImportAllowed(true)
        .setResultFormats(
            GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
            GmsDocumentScannerOptions.RESULT_FORMAT_PDF
        )
        .build()
    val scanner = GmsDocumentScanning.getClient(options)
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { activityResult ->
            if (activityResult.resultCode == RESULT_OK) {
                val result =
                    GmsDocumentScanningResult.fromActivityResultIntent(
                        activityResult.data
                    )
                overviewViewModel.setScanningResult(result)
                scannedDocuments = overviewViewModel.scannedDocuments

                result?.pdf?.let { pdf ->
                    copyFileToInternalStorage(context, pdf)
                }
            }
        }
    )
    Screen {
        Scaffold(
            topBar = { OverviewTopbar() },
            bottomBar = {
                OverviewBottombar {
                    scanner.getStartScanIntent(context as Activity)
                        .addOnSuccessListener {
                            scannerLauncher.launch(
                                IntentSenderRequest.Builder(it).build()
                            )
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                context,
                                "Failed to start scanner",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding),
                verticalArrangement = Arrangement.spacedBy(
                    space = dimensionResource(id = R.dimen.padding_medium),
                    alignment = Alignment.Top
                )
            ) {
                TextDivider(value = stringResource(R.string.txt_document_list_title))
                if (scannedDocuments.isEmpty()) {
                    FancyText(stringResource(R.string.txt_empty_list_hint))
                } else {
                    ScannedElementsGrid(
                        onItemClick = onItemClick,
                        scannedDocuments = scannedDocuments
                    )
                }
            }
        }
    }
}

fun copyFileToInternalStorage(context: Context, pdf: GmsDocumentScanningResult.Pdf): Uri {
    val destinationFile = File(context.filesDir, "scanned.pdf")
    val fileOutputStream = FileOutputStream(destinationFile)
    context.contentResolver.openInputStream(pdf.uri)?.use {
        it.copyTo(fileOutputStream)
    }
    return Uri.fromFile(destinationFile)
}