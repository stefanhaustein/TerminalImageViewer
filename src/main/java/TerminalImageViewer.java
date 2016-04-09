import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import javax.imageio.ImageIO;

public class TerminalImageViewer {

  /**
   * Main method, handles command line arguments and loads and scales images.
   */
  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      System.out.println("Image file name required. Use -w to set the width in characters (default: 80).");
      return;
    }

    int start = 0;
    int w = 80 * 4;
    if (args[0].equals("-w") && args.length > 2) {
      w = 4 * Integer.parseInt(args[1]);
      start = 2;
    }

    if (start == args.length - 1) {
      String name = args[start];

      BufferedImage original = loadImage(args[start]);

      int ow = original.getWidth();
      int oh = original.getHeight();
      int h = oh * w / ow;

      if (w == ow) {
        dump(original);
      } else {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(original, 0, 0, w, h, null);
        dump(image);
      }
    } else {
      // Directory-style rendering.
      int index = 0;
      int cw = (w - 2 * 3 * 4) / 16;
      int tw = cw * 4;
      while (index < args.length) {
        BufferedImage image = new BufferedImage(tw * 4 + 24, tw, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        int count = 0;
        StringBuilder sb = new StringBuilder();
        while (index < args.length && count < 4) {
          String name = args[index++];
          try {
            BufferedImage original = loadImage(name);
            int cut = name.lastIndexOf('/');
            sb.append(name.substring(cut + 1));
            int th = original.getHeight() * tw / original.getWidth();
            graphics.drawImage(original, count * (tw + 8), (tw - th) / 2, tw, th, null);
            count++;
            int sl = count * (cw + 2);
            while (sb.length() < sl - 2) {
              sb.append(' ');
            }
            sb.setLength(sl - 2);
            sb.append("  ");
          } catch (Exception e) {
            // Probably no image; ignore.
          }
        }
        dump(image);
        System.out.println(sb.toString());
        System.out.println();
      }
    }
  }

  static BufferedImage loadImage(String name) throws IOException {
    if (name.startsWith("http://") || name.startsWith("https://")) {
      URL url = new URL(name);
      return ImageIO.read(url);
    }
    return ImageIO.read(new File(name));
  }

  static void dump(BufferedImage image) {
    int w = image.getWidth();
    ImageData imageData = new ImageData(w, image.getHeight());
    byte[] data = imageData.data;
    int[] rgbArray = new int[w];
    for (int y = 0; y < image.getHeight(); y++) {
      image.getRGB(0, y, image.getWidth(), 1, rgbArray, 0, w);
      int pos = y * w * 4;
      for (int x = 0; x < w; x++) {
        int rgb = rgbArray[x];
        data[pos++] = (byte) (rgb >> 16);
        data[pos++] = (byte) (rgb >> 8);
        data[pos++] = (byte) rgb;
        pos++;
      }
    }
    System.out.print(imageData.dump());
  }

  /**
   * ANSI control code helpers
   */
  static class Ansi {
    public static final String RESET = "\u001b[0m";

    public static String fgColor(int r, int g, int b) {
      return "\u001b[38;2;" + (r & 255) + ";" + (g & 255) + ";" + (b & 255) + "m";
    }

    public static String bgColor(int r, int g, int b) {
      return "\u001b[48;2;" + (r & 255) + ";" + (g & 255) + ";" + (b & 255) + "m";
    }
  }

  /**
   * Converts 4x8 RGB pixel to a unicode character and a foreground and background color:
   * Uses a variation of the median cut algorithm to determine a two-color palette for the
   * character, then creates a corresponding bitmap for the partial image covered by the
   * character and finds the best match in the character bitmap table.
   */
  static class BlockChar {

    /**
     * Assumed bitmaps of the supported characters
     */
    static int[] BITMAPS = new int[] {
        0x00000000, ' ',

        0x000ff000, '\u2501',
        0x000cc000, '\u2578',
        0x00033000, '\u257a',

        0xffff0000, '\u2580',  // upper 1/2

        0x0000000f, '\u2581',  // lower 1/8
        0x000000ff, '\u2582',  // lower 1/4
        0x00000fff, '\u2583',
        0x0000ffff, '\u2584',  // lower 1/2
        0x000fffff, '\u2585',
        0x00ffffff, '\u2586',  // lower 3/4
        0x0fffffff, '\u2587',
        0xffffffff, '\u2588',  // full

        0xeeeeeeee, '\u258a',  // left 3/4
        0xcccccccc, '\u258c',  // left 1/2
        0x88888888, '\u258e',  // left 1/4

        0x0000cccc, '\u2596',  // quadrant lower left
        0x00003333, '\u2597',  // quadrant lower right
        0xcccc0000, '\u2598',  // quadrant upper left
        0xccccffff, '\u2599',  // ...
        0xcccc3333, '\u259a',
        0xffffcccc, '\u259b',
        0xffff3333, '\u259c',
        0x33330000, '\u259d',
        0x3333cccc, '\u259e',
        0x3333ffff, '\u259f',

        0x0006ff60, '\u25cf',  // Black circle

        0x000137f0, '\u25e2',  // Triangles
        0x0008cef0, '\u25e3',
        0x000fec80, '\u25e4',
        0x000f7310, '\u25e5'
    };

    /** Minimum value for each color channel. */
    int[] min = new int[3];

    /**  Maximum value for each color channel. */
    int[] max = new int[3];

    /** Red, green and blue components of the selected background color. */
    int[] bgColor = new int[3];

    /** Red, green and blue components of the selected background color. */
    int[] fgColor = new int[3];

    /** The selected character. */
    char character;

    /**
     * Converts a set of pixels to a unicode character and a background and foreground color.
     * data contains the rgba values, p0 is the start point in data and scanWidth the number
     * of bytes in each row of data.
     */
    void load(byte[] data, int p0, int scanWidth) {
      Arrays.fill(min, 255);
      Arrays.fill(max, 0);
      Arrays.fill(bgColor, 0);
      Arrays.fill(fgColor, 0);

      // Determine the minimum and maximum value for each color channel
      int pos = p0;
      for (int y = 0; y < 8; y++) {
        for (int x = 0; x < 4; x++) {
          for (int i = 0; i < 3; i++) {
            int d = data[pos++] & 255;
            min[i] = Math.min(min[i], d);
            max[i] = Math.max(max[i], d);
          }
          pos++;  // Alpha
        }
        pos += scanWidth - 16;
      }

      // Determine the color channel with the greatest range.
      int splitIndex = 0;
      int bestSplit = 0;
      for (int i = 0; i < 3; i++) {
        if (max[i] - min[i] > bestSplit) {
          bestSplit = max[i] - min[i];
          splitIndex = i;
        }
      }
      // We just split at the middle of the interval instead of computing the median.
      int splitValue = min[splitIndex] + bestSplit / 2;

      // Compute a bitmap using the given split and sum the color values for both buckets.
      int bits = 0;
      int fgCount = 0;
      int bgCount = 0;

      pos = p0;
      for (int y = 0; y < 8; y++) {
        for (int x = 0; x < 4; x++) {
          bits = bits << 1;
          int[] avg;
          if ((data[pos + splitIndex] & 255) > splitValue) {
            avg = fgColor;
            bits |= 1;
            fgCount++;
          } else {
            avg = bgColor;
            bgCount++;
          }
          for (int i = 0; i < 3; i++) {
            avg[i] += data[pos++] & 255;
          }
          pos++;  // Alpha
        }
        pos += scanWidth - 16;
      }

      // Calculate the average color value for each bucket
      for (int i = 0; i < 3; i++) {
        if (bgCount != 0) {
          bgColor[i] /= bgCount;
        }
        if (fgCount != 0) {
          fgColor[i] /= fgCount;
        }
      }

      // Find the best bitmap match by counting the bits that don't match, including
      // the inverted bitmaps.
      int bestDiff = Integer.MAX_VALUE;
      boolean invert = false;
      for (int i = 0; i < BITMAPS.length; i += 2) {
        int diff = Integer.bitCount(BITMAPS[i] ^ bits);
        if (diff < bestDiff) {
          character = (char) BITMAPS[i + 1];
          bestDiff = diff;
          invert = false;
        }
        diff = Integer.bitCount((~BITMAPS[i]) ^ bits);
        if (diff < bestDiff) {
          character = (char) BITMAPS[i + 1];
          bestDiff = diff;
          invert = true;
        }
      }

      // If the match is quite bad, use a shade image instead.
      if (bestDiff > 12) {
        invert = false;
        character = " \u2591\u2592\u2593\u2588".charAt(Math.min(4, fgCount * 5 / 32));
      }

      // If we use an inverted character, we need to swap the colors.
      if (invert) {
        int[] tmp = bgColor;
        bgColor = fgColor;
        fgColor = tmp;
      }
    }
  }

  /**
   * Roughly modeled after the corresponding HTML 5 class.
   */
  static class ImageData {
    public final int width;
    public final int height;
    public final byte[] data;

    public ImageData(int width, int height) {
      this.width = width;
      this.height = height;
      this.data = new byte[width * height * 4];
    }

    /**
     * Convert the image to an Ansi control character string setting the colors
     */
    public String dump() {
      StringBuilder sb = new StringBuilder();
      BlockChar blockChar = new BlockChar();

      for (int y = 0; y < height - 7; y += 8) {
        int pos = y * width * 4;
        String lastFg = "";
        String lastBg = "";
        for (int x = 0; x < width - 3; x += 4) {
          blockChar.load(data, pos, width * 4);
          String fg = Ansi.fgColor(blockChar.fgColor[0], blockChar.fgColor[1], blockChar.fgColor[2]);
          String bg = Ansi.bgColor(blockChar.bgColor[0], blockChar.bgColor[1], blockChar.bgColor[2]);
          if (!fg.equals(lastFg)) {
            sb.append(fg);
            lastFg = fg;
          }
          if (!bg.equals(lastBg)) {
            sb.append(bg);
            lastBg = bg;
          }
          sb.append(blockChar.character);
          pos += 16;
        }
        sb.append(Ansi.RESET).append("\n");
      }
      return sb.toString();
    }
  }
}
