package com.example.wtf2.util;

import android.content.Context;
import android.util.Log;

import com.example.wtf2.data.AppDatabase;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

public class GoogleDriveHelper {
    private static final String TAG = "GoogleDriveHelper";
    private Drive mDriveService;
    private Context mContext;
    private static final String DATABASE_FILE_NAME = "notes_database"; // Имя файла базы без расширения

    public GoogleDriveHelper(Context context) {
        mContext = context;
        initializeDriveService();
    }

    private void initializeDriveService() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mContext);
        if (account == null) {
            Log.e(TAG, "No signed-in Google account found");
            return;
        }

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                mContext, Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(account.getAccount());

        mDriveService = new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("WTF2")
                .build();
    }

    public String uploadFile(java.io.File file, String mimeType) throws IOException {
        if (mDriveService == null) {
            throw new IOException("Drive service not initialized");
        }

        String existingFileId = findFileByName(file.getName());
        File fileMetadata = new File();
        fileMetadata.setName(file.getName());

        FileContent mediaContent = new FileContent(mimeType, file);
        File uploadedFile;
        if (existingFileId != null) {
            uploadedFile = mDriveService.files().update(existingFileId, fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
            Log.d(TAG, "File updated: " + uploadedFile.getId());
        } else {
            uploadedFile = mDriveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
            Log.d(TAG, "File uploaded: " + uploadedFile.getId());
        }
        return uploadedFile.getId();
    }

    public void downloadFile(String fileId, java.io.File destinationFile) throws IOException {
        if (mDriveService == null) {
            throw new IOException("Drive service not initialized");
        }

        try (OutputStream outputStream = new FileOutputStream(destinationFile)) {
            mDriveService.files().get(fileId)
                    .executeMediaAndDownloadTo(outputStream);
            Log.d(TAG, "File downloaded: " + fileId);
        }
    }

    public String findFileByName(String fileName) throws IOException {
        if (mDriveService == null) {
            throw new IOException("Drive service not initialized");
        }

        FileList result = mDriveService.files().list()
                .setQ("name = '" + fileName + "' and trashed = false")
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        List<File> files = result.getFiles();
        if (files != null && !files.isEmpty()) {
            return files.get(0).getId();
        }
        return null;
    }

    public void syncDatabaseToDrive() throws IOException {
        java.io.File dbFile = mContext.getDatabasePath("notes_database");
        if (!dbFile.exists()) {
            throw new IOException("Local database file not found");
        }
        uploadFile(dbFile, "application/x-sqlite3");
    }

    public void downloadDatabaseFromDrive() throws IOException {
        String fileId = findFileByName(DATABASE_FILE_NAME);
        if (fileId == null) {
            throw new IOException("Database file not found on Google Drive");
        }
        java.io.File dbFile = mContext.getDatabasePath("notes_database");

        // Закрываем текущую базу данных и очищаем экземпляр
        AppDatabase currentDb = AppDatabase.getInstance(mContext);
        if (currentDb.isOpen()) {
            currentDb.close();
        }
        synchronized (AppDatabase.class) {
            AppDatabase.INSTANCE = null; // Сбрасываем singleton
        }

        // Загружаем новый файл
        downloadFile(fileId, dbFile);

        // Переинициализируем базу данных
        AppDatabase newDb = AppDatabase.getInstance(mContext);
        newDb.getOpenHelper().getWritableDatabase(); // Открываем базу
        while (!AppDatabase.isInitialized()) {
            try {
                Thread.sleep(100); // Ждем полной инициализации
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while waiting for DB initialization", e);
            }
        }
        Log.d(TAG, "Database downloaded and reinitialized");
    }
}