package com.esprit.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for generating and handling QR codes for event participation
 */
public class QRCodeUtil {

    private static final String QR_CODE_DIRECTORY = "src/main/resources/qrcodes/";
    private static final int QR_CODE_SIZE = 300;

    /**
     * Generates a QR code for event participation
     * @param userId The user ID
     * @param eventId The event ID
     * @param fileName The filename to save the QR code
     * @return Path to the generated QR code image
     * @throws WriterException If an error occurs during QR code generation
     * @throws IOException If an error occurs during file operations
     */
    public static String generateQRCode(Long userId, Long eventId, String fileName) throws WriterException, IOException {
        // Create directory if it doesn't exist
        File directory = new File(QR_CODE_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Create the QR code content (format: userId:eventId:timestamp for uniqueness)
        String qrCodeData = userId + ":" + eventId + ":" + System.currentTimeMillis();

        // Configure QR code parameters
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        // Generate the QR code
        BitMatrix matrix = new MultiFormatWriter().encode(
                qrCodeData,
                BarcodeFormat.QR_CODE,
                QR_CODE_SIZE,
                QR_CODE_SIZE,
                hints
        );

        // Save QR code as image file
        Path path = FileSystems.getDefault().getPath(QR_CODE_DIRECTORY + fileName);
        MatrixToImageWriter.writeToPath(matrix, "PNG", path);

        return path.toString();
    }

    /**
     * Creates a JavaFX Image from a QR code without saving to file
     * @param userId The user ID
     * @param eventId The event ID
     * @return JavaFX Image containing the QR code
     * @throws WriterException If an error occurs during QR code generation
     * @throws IOException If an error occurs during image creation
     */
    public static Image createQRCodeImage(Long userId, Long eventId) throws WriterException, IOException {
        // Create the QR code content
        String qrCodeData = userId + ":" + eventId + ":" + System.currentTimeMillis();

        // Configure QR code parameters
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        // Generate the QR code
        BitMatrix matrix = new MultiFormatWriter().encode(
                qrCodeData,
                BarcodeFormat.QR_CODE,
                QR_CODE_SIZE,
                QR_CODE_SIZE,
                hints
        );

        // Convert to JavaFX Image
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        return new Image(inputStream);
    }

    /**
     * Generates a unique filename for the QR code based on user and event IDs
     * @param userId The user ID
     * @param eventId The event ID
     * @return A unique filename for the QR code
     */
    public static String generateQRCodeFileName(Long userId, Long eventId) {
        return "qrcode_" + userId + "_" + eventId + "_" + System.currentTimeMillis() + ".png";
    }
}