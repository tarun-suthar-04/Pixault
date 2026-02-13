package com.pixault.stego;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class StegoEngine {

    // Header: 32 bits (4 bytes) representing the length of the data
    private static final int HEADER_SIZE = 32;

    public static BufferedImage embed(BufferedImage img, byte[] data) {
        int width = img.getWidth();
        int height = img.getHeight();

        // Check capacity: 3 bits per pixel (R, G, B)
        // Need: 32 bits (length) + data.length * 8 bits
        long requiredBits = HEADER_SIZE + (data.length * 8L);
        long availableBits = (long) width * height * 3;

        if (requiredBits > availableBits) return null;

        BufferedImage stego = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int dataIndex = 0; // byte index
        int bitIndex = 0; // bit index (0-7) inside byte
        boolean writingHeader = true;
        int headerValue = data.length;
        int headerBitsRead = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int p = img.getRGB(x, y);
                int a = (p >> 24) & 0xFF;
                int r = (p >> 16) & 0xFF;
                int g = (p >> 8) & 0xFF;
                int b = p & 0xFF;

                // Embed in R, then G, then B
                for (int i = 0; i < 3; i++) {
                    int val = 0;
                    if (writingHeader) {
                        if (headerBitsRead < 32) {
                            val = (headerValue >> (31 - headerBitsRead)) & 1;
                            headerBitsRead++;
                        }
                        if (headerBitsRead == 32) writingHeader = false;
                    } else {
                        if (dataIndex < data.length) {
                            val = (data[dataIndex] >> (7 - bitIndex)) & 1;
                            bitIndex++;
                            if (bitIndex == 8) {
                                bitIndex = 0;
                                dataIndex++;
                            }
                        } else {
                            // Done writing
                            stego.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                            return stego;
                        }
                    }

                    // Modify appropriate channel
                    if (i == 0) r = (r & 0xFE) | val;
                    if (i == 1) g = (g & 0xFE) | val;
                    if (i == 2) b = (b & 0xFE) | val;

                    // Check completion inside inner loop to break early
                    if (!writingHeader && dataIndex >= data.length && bitIndex == 0) {
                        stego.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                        // Copy remaining pixels
                        copyRemaining(img, stego, x, y);
                        return stego;
                    }
                }
                stego.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return stego;
    }

    private static void copyRemaining(BufferedImage src, BufferedImage dest, int startX, int startY) {
        int width = src.getWidth();
        int height = src.getHeight();
        // For LSB the rest MUST match original image to avoid visual artifacts
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (y < startY || (y == startY && x <= startX)) continue;
                dest.setRGB(x, y, src.getRGB(x, y));
            }
        }
    }

    public static byte[] extract(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        int length = 0;
        int bitsRead = 0;

        int currentByte = 0;
        int bitCount = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        boolean readingLength = true;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int p = img.getRGB(x, y);
                int[] channels = {(p >> 16) & 0xFF, (p >> 8) & 0xFF, p & 0xFF}; // R, G, B

                for (int c : channels) {
                    int bit = c & 1;

                    if (readingLength) {
                        length = (length << 1) | bit;
                        bitsRead++;
                        if (bitsRead == 32) {
                            readingLength = false;
                            if (length <= 0 || length > 100_000_000) return null; // Sanity check
                        }
                    } else {
                        currentByte = (currentByte << 1) | bit;
                        bitCount++;
                        if (bitCount == 8) {
                            bos.write(currentByte);
                            currentByte = 0;
                            bitCount = 0;
                            if (bos.size() == length) {
                                return bos.toByteArray();
                            }
                        }
                    }
                }
            }
        }
        return null; // Incomplete data
    }
}
