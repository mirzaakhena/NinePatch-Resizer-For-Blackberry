

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.util.Arrays;

/**
 *
 * @author mirza.akhena@gmail.com
 *
 *
 */ 
public class Util {

	public static Bitmap resize(Bitmap bmpSrc, int size, boolean isWidth) {

		int nAspectRatio = Bitmap.SCALE_TO_FIT;
		int nFilterType = Bitmap.FILTER_LANCZOS;

		int nWidth = 0;
		int nHeight = 0;

		if (isWidth) {
			nWidth = size;
			nHeight = bmpSrc.getHeight() * nWidth / bmpSrc.getWidth();
		} else {
			nHeight = size;
			nWidth = bmpSrc.getWidth() * nHeight / bmpSrc.getHeight();
		}

		return processingScalling(bmpSrc, nAspectRatio, nFilterType, nWidth, nHeight);
	}

	public static Bitmap scale(Bitmap bmpSrc, int percentage) {

		int nAspectRatio = Bitmap.SCALE_STRETCH;
		int nFilterType = Bitmap.FILTER_LANCZOS;

		int nWidth = percentage * bmpSrc.getWidth() / 100;
		int nHeight = percentage * bmpSrc.getHeight() / 100;

		return processingScalling(bmpSrc, nAspectRatio, nFilterType, nWidth, nHeight);
	}

	private static Bitmap processingScalling(Bitmap bmpSrc, int nAspectRatio, int nFilterType, int nWidth, int nHeight) {
		// Get the original dimensions of the bitmap
		int nOriginWidth = bmpSrc.getWidth();
		int nOriginHeight = bmpSrc.getHeight();
		// if (nWidth == nOriginWidth && nHeight == nOriginHeight)
		// return bmpSrc;

		// Prepare a drawing bitmap and graphic object
		Bitmap bmpOrigin = new Bitmap(nOriginWidth, nOriginHeight);
		Graphics graph = Graphics.create(bmpOrigin);

		// Create a line of transparent pixels for later use
		int[] aEmptyLine = new int[nWidth];
		for (int x = 0; x < nWidth; x++)
			aEmptyLine[x] = 0x00000000;
		// Create two scaled bitmaps
		Bitmap[] bmpScaled = new Bitmap[2];
		for (int i = 0; i < 2; i++) {
			// Draw the bitmap on a white background first, then on a black
			// background
			graph.setColor((i == 0) ? Color.WHITE : Color.BLACK);
			graph.fillRect(0, 0, nOriginWidth, nOriginHeight);
			graph.drawBitmap(0, 0, nOriginWidth, nOriginHeight, bmpSrc, 0, 0);

			// Create a new bitmap with the desired size
			bmpScaled[i] = new Bitmap(nWidth, nHeight);
			if (nAspectRatio == Bitmap.SCALE_TO_FIT) {
				// Set the alpha channel of all pixels to 0 to ensure
				// transparency is
				// applied around the picture, if needed by the transformation
				for (int y = 0; y < nHeight; y++)
					bmpScaled[i].setARGB(aEmptyLine, 0, nWidth, 0, y, nWidth, 1);
			}

			// Scale the bitmap
			bmpOrigin.scaleInto(bmpScaled[i], nFilterType, nAspectRatio);
		}

		// Prepare objects for final iteration
		Bitmap bmpFinal = bmpScaled[0];
		int[][] aPixelLine = new int[2][nWidth];

		// Iterate every line of the two scaled bitmaps
		for (int y = 0; y < nHeight; y++) {
			bmpScaled[0].getARGB(aPixelLine[0], 0, nWidth, 0, y, nWidth, 1);
			bmpScaled[1].getARGB(aPixelLine[1], 0, nWidth, 0, y, nWidth, 1);

			// Check every pixel one by one
			for (int x = 0; x < nWidth; x++) {
				// If the pixel was untouched (alpha channel still at 0), keep
				// it transparent
				if (((aPixelLine[0][x] >> 24) & 0xff) == 0)
					aPixelLine[0][x] = 0x00000000;
				else {
					// Compute the alpha value based on the difference of
					// intensity
					// in the red channel
					int nAlpha = ((aPixelLine[1][x] >> 16) & 0xff) - ((aPixelLine[0][x] >> 16) & 0xff) + 255;
					if (nAlpha == 0)
						aPixelLine[0][x] = 0x00000000; // Completely transparent
					else if (nAlpha >= 255)
						aPixelLine[0][x] |= 0xff000000; // Completely opaque
					else {
						// Compute the value of the each channel one by one
						int nRed = ((aPixelLine[0][x] >> 16) & 0xff);
						int nGreen = ((aPixelLine[0][x] >> 8) & 0xff);
						int nBlue = (aPixelLine[0][x] & 0xff);

						nRed = (int) (255 + (255.0 * ((double) (nRed - 255) / (double) nAlpha)));
						nGreen = (int) (255 + (255.0 * ((double) (nGreen - 255) / (double) nAlpha)));
						nBlue = (int) (255 + (255.0 * ((double) (nBlue - 255) / (double) nAlpha)));

						if (nRed < 0)
							nRed = 0;
						if (nGreen < 0)
							nGreen = 0;
						if (nBlue < 0)
							nBlue = 0;
						aPixelLine[0][x] = nBlue | (nGreen << 8) | (nRed << 16) | (nAlpha << 24);
					}
				}
			}

			// Change the pixels of this line to their final value
			bmpFinal.setARGB(aPixelLine[0], 0, nWidth, 0, y, nWidth, 1);
		}
		return bmpFinal;
	}

	public static Bitmap scaleNinePatchBitmap(int scale, Bitmap gambarSource) {
		Bitmap gambarCrop = getBitmapWithout1Pixel(gambarSource);
		Bitmap gambarScale = Util.scale(gambarCrop, scale);
		Bitmap gambarFrame = getBitmapFrame(gambarScale);

		int[] top = new int[gambarSource.getWidth()];
		gambarSource.getARGB(top, 0, gambarSource.getWidth(), 0, 0, gambarSource.getWidth(), 1);
		int[] ol = countMark(top);
		int xTop = scale * ol[0] / 100;
		int lTop = scale * ol[1] / 100;

		int[] left = new int[gambarSource.getHeight()];
		gambarSource.getARGB(left, 0, 1, 0, 0, 1, gambarSource.getHeight());
		ol = countMark(left);
		int yLeft = ol[0] == 1 ? 1 : (scale * ol[0] / 100);
		int lLeft = ol[0] == 1 ? gambarFrame.getHeight() - 2 : (scale * ol[1] / 100);

		return getBitmapMark(gambarFrame, xTop, lTop, yLeft, lLeft);
	}

	private static Bitmap getBitmapMark(Bitmap gambarFrame, int x, int lengthTop, int y, int lengthLeft) {

		int w = gambarFrame.getWidth();
		int h = gambarFrame.getHeight();

		Graphics g = Graphics.create(gambarFrame);
		g.setColor(Color.BLACK);
		g.drawLine(x,     0, x + lengthTop,     0);
		g.drawLine(x, h - 1, x + lengthTop, h - 1);
		
		g.drawLine(    0, y,     0, y + lengthLeft-1);
		g.drawLine(w - 1, y, w - 1, y + lengthLeft-1);

		return gambarFrame;
	}

	private static Bitmap getBitmapFrame(Bitmap gambarScale) {

		// kita siapkan gambar yang besar
		int w = gambarScale.getWidth();
		int h = gambarScale.getHeight();

		// ambil dari gambarScale masukkan ke argbData
		int[] argbData = new int[w * h];
		gambarScale.getARGB(argbData, 0, w, 0, 0, w, h);

		Bitmap bmpResult = new Bitmap(w + 2, h + 2);

		bmpResult.setARGB(argbData, 0, w, 1, 1, w, h);

		int[] transparent = new int[w + 2];
		Arrays.fill(transparent, 0x00FFFFFF);
		bmpResult.setARGB(transparent, 0, w + 2, 0, 0, w + 2, 1);
		bmpResult.setARGB(transparent, 0, w + 2, 0, h + 1, w + 2, 1);

		transparent = new int[h + 2];
		Arrays.fill(transparent, 0x00FFFFFF);
		bmpResult.setARGB(transparent, 0, 1, 0, 0, 1, h + 2);
		bmpResult.setARGB(transparent, 0, 1, w + 1, 0, 1, h + 2);

		return bmpResult;
	}

	private static Bitmap getBitmapWithout1Pixel(Bitmap gambarSource) {
		// simpan w dan h
		int w = gambarSource.getWidth();
		int h = gambarSource.getHeight();

		// siapkan array
		int[] argbData = new int[(w - 2) * (h - 2)];
		gambarSource.getARGB(argbData, 0, w - 2, 1, 1, w - 2, h - 2);

		// siapkan gambar tanpa 1 pixel
		Bitmap bmpDest = new Bitmap(w - 2, h - 2);
		bmpDest.setARGB(argbData, 0, w - 2, 0, 0, w - 2, h - 2);

		return bmpDest;
	}

	private static int[] countMark(int[] array) {
		int offset = -1;
		int length = 0;
		boolean firstFound = true;
		for (int i = 0; i < array.length; i++) {
			if (array[i] == 0xFF000000) {
				if (firstFound) {
					firstFound = false;
					offset = i;
				}
				length++;
			}
		}
		return new int[] { offset, length };
	}

}
