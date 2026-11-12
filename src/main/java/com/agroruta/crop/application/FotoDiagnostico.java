package com.agroruta.crop.application;

import com.agroruta.shared.exception.BusinessException;
import com.agroruta.shared.exception.ErrorCode;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * Revisa la foto que sube la persona y la vuelve a codificar. Al redibujarla se pierden los
 * metadatos (ubicación GPS, modelo del teléfono) y cualquier cosa que no sea la imagen.
 */
public final class FotoDiagnostico {

    /** Tamaño máximo del archivo recibido */
    public static final int MAX_BYTES = 6 * 1024 * 1024;
    /** Lado mayor de la copia que se analiza: suficiente para ver manchas e insectos */
    static final int LADO_ANALISIS = 1280;
    /** Lado mayor de la copia que queda en la bitácora */
    static final int LADO_GUARDADO = 640;
    private static final int LADO_MINIMO = 200;
    /** Una imagen enorme en píxeles puede pesar poco en disco y agotar la memoria al abrirla */
    private static final int LADO_MAXIMO = 8000;
    private static final Set<String> FORMATOS = Set.of("jpeg", "jpg", "png");

    /** @param paraAnalizar JPEG de hasta 1280 px; @param paraGuardar JPEG de hasta 640 px */
    public record Preparada(byte[] paraAnalizar, byte[] paraGuardar) {}

    private FotoDiagnostico() {}

    public static Preparada preparar(byte[] archivo) {
        if (archivo == null || archivo.length == 0) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "Falta la foto.");
        }
        if (archivo.length > MAX_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "La foto pesa demasiado. El máximo es 6 MB.");
        }
        BufferedImage imagen = leer(archivo);
        try {
            return new Preparada(jpeg(escalar(imagen, LADO_ANALISIS), 0.85f), jpeg(escalar(imagen, LADO_GUARDADO), 0.8f));
        } catch (IOException | RuntimeException e) {
            throw noEsFoto();
        }
    }

    private static BufferedImage leer(byte[] archivo) {
        int ancho;
        int alto;
        BufferedImage imagen;
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(archivo))) {
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) throw noEsFoto();
            ImageReader lector = lectores.next();
            try {
                if (!FORMATOS.contains(lector.getFormatName().toLowerCase(Locale.ROOT))) throw noEsFoto();
                lector.setInput(entrada, true, true);
                ancho = lector.getWidth(0);
                alto = lector.getHeight(0);
                // Las medidas se revisan antes de abrir la imagen completa
                if (ancho > LADO_MAXIMO || alto > LADO_MAXIMO) {
                    throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "La foto es demasiado grande. Tómala de nuevo con menos resolución.");
                }
                if (ancho < LADO_MINIMO || alto < LADO_MINIMO) {
                    throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "La foto es muy pequeña para analizarla.");
                }
                imagen = lector.read(0);
            } finally {
                lector.dispose();
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw noEsFoto();
        }
        if (imagen == null) throw noEsFoto();
        return imagen;
    }

    /** Reduce la imagen para que su lado mayor no pase de {@code lado}; nunca la agranda */
    static BufferedImage escalar(BufferedImage origen, int lado) {
        double factor = Math.min(1.0, (double) lado / Math.max(origen.getWidth(), origen.getHeight()));
        int ancho = Math.max(1, (int) Math.round(origen.getWidth() * factor));
        int alto = Math.max(1, (int) Math.round(origen.getHeight() * factor));
        // RGB sin transparencia: es lo que admite JPEG. El fondo blanco cubre las zonas transparentes de un PNG
        BufferedImage destino = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, ancho, alto);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(origen, 0, 0, ancho, alto, null);
        } finally {
            g.dispose();
        }
        return destino;
    }

    private static byte[] jpeg(BufferedImage imagen, float calidad) throws IOException {
        Iterator<ImageWriter> escritores = ImageIO.getImageWritersByFormatName("jpeg");
        if (!escritores.hasNext()) throw new IOException("Sin codificador JPEG");
        ImageWriter escritor = escritores.next();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ImageOutputStream destino = ImageIO.createImageOutputStream(salida)) {
            escritor.setOutput(destino);
            ImageWriteParam ajustes = escritor.getDefaultWriteParam();
            ajustes.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            ajustes.setCompressionQuality(calidad);
            escritor.write(null, new IIOImage(imagen, null, null), ajustes);
        } finally {
            escritor.dispose();
        }
        return salida.toByteArray();
    }

    private static BusinessException noEsFoto() {
        return new BusinessException(ErrorCode.INVALID_ARGUMENT, "El archivo no es una foto válida. Usa una imagen JPG o PNG.");
    }
}
