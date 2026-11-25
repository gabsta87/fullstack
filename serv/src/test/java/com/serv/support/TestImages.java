package com.serv.support;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Builds real, decodable images (and some malicious look-alikes) for upload tests. */
public final class TestImages {

    private TestImages() {}

    public static byte[] jpeg(int width, int height) {
        return encode(width, height, "jpg", BufferedImage.TYPE_INT_RGB);
    }

    public static byte[] png(int width, int height) {
        return encode(width, height, "png", BufferedImage.TYPE_INT_ARGB);
    }

    /** An HTML/JS payload that claims to be an image. */
    public static byte[] htmlPayload() {
        return "<html><script>alert(document.cookie)</script></html>".getBytes();
    }

    /** A valid JPEG header followed by garbage: passes a naive magic-byte check but cannot be decoded. */
    public static byte[] truncatedJpeg() {
        byte[] real = jpeg(50, 50);
        byte[] cut = new byte[40];
        System.arraycopy(real, 0, cut, 0, cut.length);
        return cut;
    }

    /** A JPEG with a comment segment containing a script, appended after a valid image. */
    public static byte[] polyglotJpeg() {
        byte[] img = jpeg(40, 40);
        byte[] payload = "<?php system($_GET['c']); ?><script>alert(1)</script>".getBytes();
        byte[] out = new byte[img.length + payload.length];
        System.arraycopy(img, 0, out, 0, img.length);
        System.arraycopy(payload, 0, out, img.length, payload.length);
        return out;
    }

    /**
     * A tiny PNG whose header declares a gigantic canvas (decompression bomb).
     * Only the IHDR is valid, which is enough for the dimension check to trigger.
     */
    public static byte[] pngDeclaring(int width, int height) {
        byte[] small = png(1, 1);
        // IHDR width/height are stored big-endian at offsets 16 and 20.
        writeInt(small, 16, width);
        writeInt(small, 20, height);
        return small;
    }

    /** Minimal MP4 header ('ftyp' box). */
    public static byte[] mp4() {
        byte[] b = new byte[2048];
        byte[] head = {0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'};
        System.arraycopy(head, 0, b, 0, head.length);
        return b;
    }

    private static void writeInt(byte[] b, int offset, int value) {
        b[offset] = (byte) (value >>> 24);
        b[offset + 1] = (byte) (value >>> 16);
        b[offset + 2] = (byte) (value >>> 8);
        b[offset + 3] = (byte) value;
    }

    private static byte[] encode(int width, int height, String format, int type) {
        BufferedImage image = new BufferedImage(width, height, type);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(200, 60, 90));
        g.fillRect(0, 0, width, height);
        g.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
