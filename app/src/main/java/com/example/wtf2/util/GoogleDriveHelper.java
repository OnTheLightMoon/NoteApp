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

/**
 * Утилитный класс для работы с Google Drive: загрузка и скачивание базы данных.
 */
public class GoogleDriveHelper {
    private static final String TAG = "GoogleDriveHelper";
    private static final String DATABASE_FILE_NAME = "notes_database";
    private static final String MIME_TYPE_SQLITE = "application/x-sqlite3";

    private Drive mDriveService;
    private final Context mContext;

    public GoogleDriveHelper(Context context) {
        mContext = context;
        initializeDriveService();
    }

    /**
     * Инициализирует сервис Google Drive для работы с файлами.
     * Проверяет наличие авторизованного аккаунта и логирует ошибки.
     */
    private void initializeDriveService() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(mContext);
        if (account == null) {
            Log.e(TAG, "No signed-in Google account found. Please sign in.");
            return;
        }

        Log.d(TAG, "Found signed-in account: " + account.getEmail());
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                mContext, Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(account.getAccount());

        try {
            mDriveService = new Drive.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName("WTF2")
                    .build();
            Log.d(TAG, "Drive service initialized successfully for account: " + account.getEmail());
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Drive service: " + e.getMessage(), e);
            mDriveService = null;
        }
    }

    /**
     * Загружает файл на Google Drive, обновляя существующий или создавая новый.
     * @param file Локальный файл для загрузки
     * @param mimeType MIME-тип файла
     * @return Идентификатор загруженного файла
     * @throws IOException Если сервис не инициализирован или произошла ошибка
     */
    public String uploadFile(java.io.File file, String mimeType) throws IOException {
        if (mDriveService == null) {
            Log.w(TAG, "Drive service not initialized, attempting reinitialization");
            reinitializeDriveService();
            if (mDriveService == null) {
                throw new IOException("Drive service not initialized after retry. Please sign in.");
            }
        }

        String existingFileId = findFileByName(file.getName());
        File fileMetadata = new File().setName(file.getName());
        FileContent mediaContent = new FileContent(mimeType, file);

        File uploadedFile;
        if (existingFileId != null) {
            uploadedFile = mDriveService.files().update(existingFileId, fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
            Log.d(TAG, "Updated existing file on Drive: " + uploadedFile.getId());
        } else {
            uploadedFile = mDriveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
            Log.d(TAG, "Created new file on Drive: " + uploadedFile.getId());
        }
        return uploadedFile.getId();
    }

    /**
     * Скачивает файл с Google Drive в указанное место.
     * @param fileId Идентификатор файла на Drive
     * @param destinationFile Локальный файл для сохранения
     * @throws IOException Если сервис не инициализирован или произошла ошибка
     */
    public void downloadFile(String fileId, java.io.File destinationFile) throws IOException {
        if (mDriveService == null) {
            Log.w(TAG, "Drive service not initialized, attempting reinitialization");
            reinitializeDriveService();
            if (mDriveService == null) {
                throw new IOException("Drive service not initialized after retry. Please sign in.");
            }
        }

        try (OutputStream outputStream = new FileOutputStream(destinationFile)) {
            mDriveService.files().get(fileId)
                    .executeMediaAndDownloadTo(outputStream);
            Log.d(TAG, "File downloaded from Drive: " + fileId + " to " + destinationFile.getAbsolutePath());
        }
    }

    /**
     * Ищет файл на Google Drive по имени.
     * @param fileName Имя файла для поиска
     * @return Идентификатор файла или null, если файл не найден
     * @throws IOException Если сервис не инициализирован
     */
    public String findFileByName(String fileName) throws IOException {
        if (mDriveService == null) {
            Log.w(TAG, "Drive service not initialized, attempting reinitialization");
            reinitializeDriveService();
            if (mDriveService == null) {
                throw new IOException("Drive service not initialized after retry. Please sign in.");
            }
        }

        FileList result = mDriveService.files().list()
                .setQ("name = '" + fileName + "' and trashed = false")
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        List<File> files = result.getFiles();
        if (files != null && !files.isEmpty()) {
            Log.d(TAG, "Found file on Drive: " + files.get(0).getName() + " (ID: " + files.get(0).getId() + ")");
            return files.get(0).getId();
        }
        Log.d(TAG, "File not found on Drive: " + fileName);
        return null;
    }

    /**
     * Синхронизирует локальную базу данных с Google Drive.
     * @throws IOException Если файл не найден или сервис не инициализирован
     */
    public void syncDatabaseToDrive() throws IOException {
        java.io.File dbFile = mContext.getDatabasePath(DATABASE_FILE_NAME);
        if (!dbFile.exists()) {
            throw new IOException("Local database file not found at " + dbFile.getAbsolutePath());
        }
        uploadFile(dbFile, MIME_TYPE_SQLITE);
    }

    /**
     * Скачивает базу данных с Google Drive и заменяет локальную.
     * @throws IOException Если файл не найден или сервис не инициализирован
     */
    public void downloadDatabaseFromDrive() throws IOException {
        String fileId = findFileByName(DATABASE_FILE_NAME);
        if (fileId == null) {
            throw new IOException("Database file '" + DATABASE_FILE_NAME + "' not found on Google Drive");
        }
        java.io.File dbFile = mContext.getDatabasePath(DATABASE_FILE_NAME);

        // Закрываем и сбрасываем текущую базу
        AppDatabase currentDb = AppDatabase.getInstance(mContext);
        if (currentDb.isOpen()) {
            currentDb.close();
        }
        AppDatabase.resetInstance();

        // Скачиваем файл
        downloadFile(fileId, dbFile);

        // Переинициализируем базу
        AppDatabase newDb = AppDatabase.getInstance(mContext);
        newDb.getOpenHelper().getWritableDatabase();
        while (!AppDatabase.isInitialized()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while waiting for database initialization", e);
                Thread.currentThread().interrupt();
            }
        }
        Log.d(TAG, "Database successfully downloaded and reinitialized");
    }

    /**
     * Проверяет, инициализирован ли сервис Drive.
     * @return true, если сервис готов к использованию
     */
    public boolean isDriveServiceInitialized() {
        return mDriveService != null;
    }

    /**
     * Принудительно переинициализирует сервис Drive.
     */
    public void reinitializeDriveService() {
        mDriveService = null;
        initializeDriveService();
    }
}