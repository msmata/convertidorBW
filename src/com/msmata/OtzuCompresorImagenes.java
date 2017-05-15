package com.msmata;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.plaf.BorderUIResource;

import com.sun.media.imageio.plugins.tiff.TIFFImageWriteParam;
import com.sun.media.imageioimpl.plugins.tiff.TIFFT6Compressor;

public class OtzuCompresorImagenes {
	private static final double MARGEN_DERECHO_UMBRAL = 0.70;
	private static final double MARGEN_SUP_UMBRAL = 0.15;//El porcentaje superior de la imagen al cual se aplica el umbral superior
	private static final int UMBRAL_SUPERIOR = 146;
	private static final int UMBRAL = 90;
	private static final double RESIZE_PERCENTAGE = 0.70;

	public static void main(String[] args) {

		String filename = "Pampa 5699.tif";
		String path = "C:\\cafi\\NUEVOS\\Grises\\";
		
		comprimirImagenEscalaGrises(filename, path);
	}

	public static void comprimirImagenEscalaGrises(String filename, String path) {
		File input = new File(path + filename);
		BufferedImage image;
		try {
//			long time = System.currentTimeMillis();
//			long totalTime = time;
			image = ImageIO.read(input);
//			System.out.println("Lectura de imagen " + input.getName() + " : " + (System.currentTimeMillis() - time) + " milisegundos");

			// Convertir los pixeles a blanco o negro
//			time = System.currentTimeMillis();
			int umbralOtsu = otsuTreshold(image);
			
			BufferedImage finalThresholdImage = aplicarUmbralSelectivo(image, umbralOtsu);
			
			// Achicar la imagen un determinado porcentaje
//			time = System.currentTimeMillis();
			BufferedImage resizedImage = resize(finalThresholdImage, RESIZE_PERCENTAGE);
//			System.out.println("Resized de imagen: " + (System.currentTimeMillis() - time) + " milisegundos");
			
			
//			aplicarRecuadroNegro(image, UMBRAL);
//			System.out.println("Aplicar umbral: " + (System.currentTimeMillis() - time) + " milisegundos");

			// Pasar de modo grayscale a modo indexado 1 bit
//			time = System.currentTimeMillis();
//			BufferedImage singleBitImage = ConvertUtil.convert1(finalThresholdImage);
//			System.out.println("Pasar de modo grayscale a modo indexado 1 bit: " + (System.currentTimeMillis() - time) + " milisegundos");
			// Se comprime a CCITT T.4
//			time = System.currentTimeMillis();
			comprimirImagen(resizedImage, path + input.getName(), umbralOtsu);
//			System.out.println("Compresion CCITT T.4: " + (System.currentTimeMillis() - time) + " milisegundos");
//			System.out.println("Proceso finalizado en " + (System.currentTimeMillis() - totalTime) + " milisegundos");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void comprimirImagen(BufferedImage singleBitImage, String filename, int umbralUtilizado) throws IOException {
		ImageWriter writer = ImageIO.getImageWritersByFormatName("tif").next();
		TIFFImageWriteParam writeParam = (TIFFImageWriteParam) writer.getDefaultWriteParam();
		writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		writeParam.setCompressionType("CCITT T.6");
		writeParam.setTIFFCompressor(new TIFFT6Compressor());

		File outputFile = new File(filename + "_procesado_otsu" + umbralUtilizado + ".tif");
		ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile);
		writer.setOutput(ios);
		// Pruebas 28-3
		writer.prepareWriteSequence(null);
		ImageTypeSpecifier spec = ImageTypeSpecifier.createFromRenderedImage(singleBitImage);
		javax.imageio.metadata.IIOMetadata metadata = writer.getDefaultImageMetadata(spec, writeParam);
		IIOImage iioImage = new IIOImage(singleBitImage, null, metadata);
		writer.writeToSequence(iioImage, writeParam);
		singleBitImage.flush();
		writer.endWriteSequence();
		ios.flush();
		writer.dispose();
		ios.close();
		// writer.write(singleBitImage);
	}

	private static BufferedImage aplicarUmbral(BufferedImage image, int umbral) {
		BufferedImage finalThresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		int pixelBlanco = mixColor(0, 0, 0);
		int pixelNegro = mixColor(255, 255, 255);
		
		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < image.getHeight(); j++) {
				Color c = new Color(image.getRGB(i, j));
				if (c.getRed() < umbral) {// Como son grises las 3 componentes son iguales
					finalThresholdImage.setRGB(i, j, pixelBlanco);
				} else {
					finalThresholdImage.setRGB(i, j, pixelNegro);
				}
			}
		}
		return finalThresholdImage;
	}
	
	private static BufferedImage aplicarUmbralSelectivo(BufferedImage image, int umbral) {
//		BufferedImage finalThresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		BufferedImage finalThresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		int pixelBlanco = mixColor(0, 0, 0);
		int pixelNegro = mixColor(255, 255, 255);
			
		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < image.getHeight(); j++) {
				Color c = new Color(image.getRGB(i, j));
				//Probar esto
//				int umbralUtilizado = j < image.getHeight() * MARGEN_SUP_UMBRAL ? UMBRAL_SUPERIOR : umbral;
				int umbralUtilizado = j < image.getHeight() * MARGEN_SUP_UMBRAL && i > image.getWidth() * MARGEN_DERECHO_UMBRAL ? UMBRAL_SUPERIOR : umbral;
				if (c.getRed() < umbralUtilizado) {// Como son grises las 3 componentes son iguales
					finalThresholdImage.setRGB(i, j, pixelBlanco);
				} else {
					finalThresholdImage.setRGB(i, j, pixelNegro);
				}
			}
		}
		return finalThresholdImage;
	}

	private static BufferedImage resize(BufferedImage originalImage, double percent) {
		// creates output image
		int newWidth = (int) (originalImage.getWidth() * percent);
		int newHeight = (int) (originalImage.getHeight() * percent);
		BufferedImage resizedImage = new BufferedImage(newWidth, newHeight,	originalImage.getType());

		// scales the input image to the output image
		Graphics2D g2d = resizedImage.createGraphics();
		
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		
		g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
		g2d.dispose();

		return resizedImage;
	}

	private static int mixColor(int red, int green, int blue) {
		return red << 16 | green << 8 | blue;
	}
	
	private static int[] imageHistogram(BufferedImage input) {
		 
        int[] histogram = new int[256];
 
        for(int i=0; i<histogram.length; i++) histogram[i] = 0;
 
        for(int i=0; i<input.getWidth(); i++) {
            for(int j=0; j<input.getHeight(); j++) {
                int red = new Color(input.getRGB (i, j)).getRed();
                histogram[red]++;
            }
        }
 
        return histogram;
 
    }
	
	public static int otsuTreshold(BufferedImage original) {
		 
        int[] histogram = imageHistogram(original);
        int total = original.getHeight() * original.getWidth();
 
        float sum = 0;
        for(int i=0; i<256; i++) sum += i * histogram[i];
 
        float sumB = 0;
        int wB = 0;
        int wF = 0;
 
        float varMax = 0;
        int threshold = 0;
 
        for(int i=0 ; i<256 ; i++) {
            wB += histogram[i];
            if(wB == 0) continue;
            wF = total - wB;
 
            if(wF == 0) break;
 
            sumB += (float) (i * histogram[i]);
            float mB = sumB / wB;
            float mF = (sum - sumB) / wF;
 
            float varBetween = (float) wB * (float) wF * (mB - mF) * (mB - mF);
 
            if(varBetween > varMax) {
                varMax = varBetween;
                threshold = i;
            }
        }
 
        return threshold;
 
    }
}
