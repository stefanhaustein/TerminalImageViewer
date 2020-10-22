import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.Matcher;

import javax.imageio.ImageIO;

/**
 * Simple program to print images to the shell using 24 bit ANSI color codes and Unicode block graphics characters.
 * 
 * License: Apache 2.0
 * @author Stefan Haustein
 */
public class TerminalImageViewer {

  static boolean grayscale = false;
  static int mode = Ansi.MODE_24BIT;
  static boolean html = false;


  /**
   * Main method, handles command line arguments and loads and scales images.
   */
  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      System.out.println(
          "Image file name required.\n\n" +
          "TerminalImageViewer Java\n" +
          " - Use -w and -h to set the maximum width and height in characters (defaults: 80, 24).\n" +
          " - Use -256 for 256 color mode, -grayscale for grayscale and -stdin to obtain file names from stdin.\n" +
          " - When multiple files are supplied, -c sets the number of images per row (default: 4)."
          "NOTE: This version of TerminalImageViewer is not regularly updated. Please use the C++ version instead.\n");
      return;
    }

    int start = 0;
    int maxWidth = 80;
    int maxHeight = 24;
    int columns = 4;
    boolean stdin = false;
    while (start < args.length && args[start].startsWith("-")) {
      String option = args[start];
      if (option.equals("-w") && args.length > start + 1) {
        maxWidth = Integer.parseInt(args[++start]);
      } else if (option.equals("-h") && args.length > start + 1) {
        maxHeight = Integer.parseInt(args[++start]);
      } else if (option.equals("-c") && args.length > start + 1) {
        columns = Integer.parseInt(args[++start]);
      } else if (option.equals("-256")) {
        mode = (mode & ~Ansi.MODE_24BIT) | Ansi.MODE_256;
      } else if (option.equals("-grayscale")) {
        grayscale = true;
      } else if (option.equals("-html")) {
        html = true;
      } else if (option.equals("-stdin")) {
        stdin = true;
      }
      start++;
    }

    maxWidth *= 4;
    maxHeight *= 8;

    if (stdin) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      while (true) {
        String name = reader.readLine();
        if (name == null || name.isEmpty()) {
          break;
        }
        convert(name, maxWidth, maxHeight);
      }
    } else if (start == args.length - 1 && (isUrl(args[start]) || !new File(args[start]).isDirectory())) {
      convert(args[start], maxWidth, maxHeight);
    } else {
      // Directory-style rendering.
      int index = 0;
      int cw = (maxWidth - 2 * (columns - 1) * 4) / (4 * columns);
      int tw = cw * 4;

      while (index < args.length) {
        BufferedImage image = new BufferedImage(tw * columns + 24, tw, grayscale ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        int count = 0;
        StringBuilder sb = new StringBuilder();
        while (index < args.length && count < columns) {
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
            // Probably no image; ignore.
          }
        }
        dump(image, mode);
        System.out.println(sb.toString());
        System.out.println();
      }
    }
  }

  static boolean isUrl(String name) {
    return name.startsWith("http://") || name.startsWith("https://");
  }

  static void convert(String name, int maxWidth, int maxHeight) throws IOException {
    BufferedImage original = loadImage(name);

    float originalWidth = original.getWidth();
    float originalHeight = original.getHeight();
    float scale = Math.min(maxWidth / originalWidth, maxHeight / originalHeight);
    int height = (int) (originalHeight * scale);
    int width = (int) (originalWidth * scale);

    if (originalWidth == width && !grayscale) {
      dump(original, mode);
    } else {
      BufferedImage image = new BufferedImage(width, height, grayscale ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics = image.createGraphics();
      graphics.drawImage(original, 0, 0, width, height, null);
      dump(image, mode);
    }
  }

  static BufferedImage loadImage(String name) throws IOException {
    if (isUrl(name)) {
      URL url = new URL(name);
      return ImageIO.read(url);
    }
    return ImageIO.read(new File(name));
  }

  static void dump(BufferedImage image, int mode) {
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
    System.out.print(imageData.dump(mode));
  }



  /**
   * ANSI control code helpers
   */
  static class Ansi {
    public static final String RESET = "\u001b[0m";
    public static int FG = 1;
    public static int BG = 2;
    public static int MODE_256 = 4;
    public static int MODE_24BIT = 8;

    public static final int[] COLOR_STEPS = {0, 0x5f, 0x87, 0xaf, 0xd7, 0xff};
    public static final int[] GRAYSCALE = {0x08, 0x12, 0x1c, 0x26, 0x30, 0x3a, 0x44, 0x4e, 0x58, 0x62, 0x6c, 0x76,
                                           0x80, 0x8a, 0x94, 0x9e, 0xa8, 0xb2, 0xbc, 0xc6, 0xd0, 0xda, 0xe4, 0xee};

    static int bestIndex(int v, int[] options) {
      int index = Arrays.binarySearch(options, v);
      if (index < 0) {
        index = -index - 1;
        // need to check [index] and [index - 1]
        if (index == options.length) {
          index = options.length - 1;
        } else if (index > 0) {
          int val0 = options[index - 1];
          int val1 = options[index];
          if (v - val0 < val1 - v) {
            index = index - 1;
          }
        }
      }
      return index;
    }

    static int sqr(int i) {
      return i * i;
    }

    public static int clamp(int value, int min, int max) {
      return Math.min(Math.max(value, min), max);
    }

    public static String color(int flags, int r, int g, int b) {
      r = clamp(r, 0, 255);
      g = clamp(g, 0, 255);
      b = clamp(b, 0, 255);

      boolean bg = (flags & BG) != 0;

      if ((flags & MODE_256) == 0) {
        return (bg ? "\u001b[48;2;" : "\u001b[38;2;") + r + ";" + g + ";" + b + "m";
      }
      int rIdx = bestIndex(r, COLOR_STEPS);
      int gIdx = bestIndex(g, COLOR_STEPS);
      int bIdx = bestIndex(b, COLOR_STEPS);

      int rQ = COLOR_STEPS[rIdx];
      int gQ = COLOR_STEPS[gIdx];
      int bQ = COLOR_STEPS[bIdx];

      int gray = Math.round(r * 0.2989f + g * 0.5870f + b * 0.1140f);

      int grayIdx = bestIndex(gray, GRAYSCALE);
      int grayQ = GRAYSCALE[grayIdx];

      int colorIndex;
      if (0.3 * sqr(rQ-r) + 0.59 * sqr(gQ-g) + 0.11 *sqr(bQ-b) <
          0.3 * sqr(grayQ-r) + 0.59 * sqr(grayQ-g) + 0.11 * sqr(grayQ-b)) {
        colorIndex = 16 + 36 * rIdx + 6 * gIdx + bIdx;
      } else {
        colorIndex = 232 + grayIdx;  // 1..24 -> 232..255
      }
      return (bg ? "\u001B[48;5;" : "\u001B[38;5;") + colorIndex + "m";
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
        0x00000000, '\u00a0',

        // Block graphics

     // 0xffff0000, '\u2580',  // upper 1/2; redundant with inverse lower 1/2

        0x0000000f, '\u2581',  // lower 1/8
        0x000000ff, '\u2582',  // lower 1/4
        0x00000fff, '\u2583',
        0x0000ffff, '\u2584',  // lower 1/2
        0x000fffff, '\u2585',
        0x00ffffff, '\u2586',  // lower 3/4
        0x0fffffff, '\u2587',
     // 0xffffffff, '\u2588',  // full; redundant with inverse space

        0xeeeeeeee, '\u258a',  // left 3/4
        0xcccccccc, '\u258c',  // left 1/2
        0x88888888, '\u258e',  // left 1/4

        0x0000cccc, '\u2596',  // quadrant lower left
        0x00003333, '\u2597',  // quadrant lower right
        0xcccc0000, '\u2598',  // quadrant upper left
     // 0xccccffff, '\u2599',  // 3/4 redundant with inverse 1/4
        0xcccc3333, '\u259a',  // diagonal 1/2
     // 0xffffcccc, '\u259b',  // 3/4 redundant
     // 0xffff3333, '\u259c',  // 3/4 redundant
        0x33330000, '\u259d',  // quadrant upper right
     // 0x3333cccc, '\u259e',  // 3/4 redundant
     // 0x3333ffff, '\u259f',  // 3/4 redundant

        // Line drawing subset: no double lines, no complex light lines
        // Simple light lines duplicated because there is no center pixel int the 4x8 matrix

        0x000ff000, '\u2501',  // Heavy horizontal
        0x66666666, '\u2503',  // Heavy vertical

        0x00077666, '\u250f',  // Heavy down and right
        0x000ee666, '\u2513',  // Heavy down and left
        0x66677000, '\u2517',  // Heavy up and right
        0x666ee000, '\u251b',  // Heavy up and left

        0x66677666, '\u2523',  // Heavy vertical and right
        0x666ee666, '\u252b',  // Heavy vertical and left
        0x000ff666, '\u2533',  // Heavy down and horizontal
        0x666ff000, '\u253b',  // Heavy up and horizontal
        0x666ff666, '\u254b',  // Heavy cross

        0x000cc000, '\u2578',  // Bold horizontal left
        0x00066000, '\u2579',  // Bold horizontal up
        0x00033000, '\u257a',  // Bold horizontal right
        0x00066000, '\u257b',  // Bold horizontal down

        0x06600660, '\u254f',  // Heavy double dash vertical

        0x000f0000, '\u2500',  // Light horizontal
        0x0000f000, '\u2500',  //
        0x44444444, '\u2502',  // Light vertical
        0x22222222, '\u2502',

        0x000e0000, '\u2574',  // light left
        0x0000e000, '\u2574',  // light left
        0x44440000, '\u2575',  // light up
        0x22220000, '\u2575',  // light up
        0x00030000, '\u2576',  // light right
        0x00003000, '\u2576',  // light right
        0x00004444, '\u2575',  // light down
        0x00002222, '\u2575',  // light down

        // Misc technical

        0x44444444, '\u23a2',  // [ extension
        0x22222222, '\u23a5',  // ] extension

        //12345678
        0x0f000000, '\u23ba',  // Horizontal scanline 1
        0x00f00000, '\u23bb',  // Horizontal scanline 3
        0x00000f00, '\u23bc',  // Horizontal scanline 7
        0x000000f0, '\u23bd',  // Horizontal scanline 9

        // Geometrical shapes. Tricky because some of them are too wide.

//      0x00ffff00, '\u25fe',  // Black medium small square
        0x00066000, '\u25aa',  // Black small square

/*
        0x11224488, '\u2571',  // diagonals
        0x88442211, '\u2572',
        0x99666699, '\u2573',

        0x000137f0, '\u25e2',  // Triangles
        0x0008cef0, '\u25e3',
        0x000fec80, '\u25e4',
        0x000f7310, '\u25e5'
*/
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

      // Determine the minimum and maximum value for each color channel
      int pos = p0;
      for (int y = 0; y < 8; y++) {
        for (int x = 0; x < 4; x++) {
          for (int i = 0; i < 3; i++) {
            int d = data[pos++] & 255;
            min[i] = Math.min(min[i], d);
            max[i] = Math.max(max[i], d);
          }
          pos++;  // Alpha
        }
        pos += scanWidth - 16;
      }

      // Determine the color channel with the greatest range.
      int splitIndex = 0;
      int bestSplit = 0;
      for (int i = 0; i < 3; i++) {
        if (max[i] - min[i] > bestSplit) {
          bestSplit = max[i] - min[i];
          splitIndex = i;
        }
      }
      // We just split at the middle of the interval instead of computing the median.
      int splitValue = min[splitIndex] + bestSplit / 2;

      // Compute a bitmap using the given split and sum the color values for both buckets.
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
          pos++;  // Alpha
        }
        pos += scanWidth - 16;
      }

      // Calculate the average color value for each bucket
      for (int i = 0; i < 3; i++) {
        if (bgCount != 0) {
          bgColor[i] /= bgCount;
        }
        if (fgCount != 0) {
          fgColor[i] /= fgCount;
        }
      }

      // Find the best bitmap match by counting the bits that don't match, including
      // the inverted bitmaps.
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

      // If the match is quite bad, use a shade image instead.
      if (bestDiff > 10) {
        invert = false;
        character = " \u2591\u2592\u2593\u2588".charAt(Math.min(4, fgCount * 5 / 32));
      }

      // If we use an inverted character, we need to swap the colors.
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

    public String hex6(int r, int g, int b) {
      return Integer.toHexString((1 << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255)).substring(1);
    }
    /**
     * Convert the image to an Ansi control character string setting the colors
     */
    public String dump(int mode) {
      StringBuilder sb = new StringBuilder();
      BlockChar blockChar = new BlockChar();

      for (int y = 0; y < height - 7; y += 8) {
        int pos = y * width * 4;
        if (html) {
          String last = "";
          for (int x = 0; x < width - 3; x += 4) {
            blockChar.load(data, pos, width * 4);
            String fg = hex6(blockChar.fgColor[0], blockChar.fgColor[1], blockChar.fgColor[2]);
            String bg = hex6(blockChar.bgColor[0], blockChar.bgColor[1], blockChar.bgColor[2]);
            String style = "background-color:#" + bg + ";color:#" + fg;
            if (!style.equals(last)) {
              if (!last.isEmpty()) {
                sb.append("</tt>");
              }
              sb.append("<tt style='").append(style).append("'>");
              last = style;
            }
            sb.append("&#" + ((int) blockChar.character) + ";");
            pos += 16;
          }
          sb.append("</tt><br />\n");
        } else {
          String lastFg = "";
          String lastBg = "";
          for (int x = 0; x < width - 3; x += 4) {
            blockChar.load(data, pos, width * 4);
            String fg = Ansi.color(Ansi.FG | mode, blockChar.fgColor[0], blockChar.fgColor[1], blockChar.fgColor[2]);
            String bg = Ansi.color(Ansi.BG | mode, blockChar.bgColor[0], blockChar.bgColor[1], blockChar.bgColor[2]);
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
      }
      return sb.toString();
    }
  }
}
