package com.github.stefanhaustein;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

public class TerminalImageViewer {
  private Integer mHeight = 24;
  private Integer mWidth = 80;

  /**
   * Main method, handles command line arguments and loads and scales images.
   */
  public static void main(String... args) {
    TerminalImageViewer main = new TerminalImageViewer();
    try {
      main.run(args);
    } catch (NumberFormatException e) {
      System.out.println("Wrong value provided for argument!");
    } catch (Exception e) {
      System.out.println(e.getLocalizedMessage());
    }
  }

  static <T> List<List<T>> slice(List<T> list, final int sliceSize) {
    List<List<T>> parts = new ArrayList<>();
    final int listSize = list.size();
    for (int index = 0; index < listSize; index += sliceSize) {
      parts.add(new ArrayList<>(
          list.subList(index, Math.min(listSize, index + sliceSize))));
    }
    return parts;
  }

  static BufferedImage loadImage(String name) throws IOException {
    if (isValidURL(name)) {
      return ImageIO.read(new URL(name));
    } else {
      return ImageIO.read(new File(name));
    }
  }

  static boolean isValidURL(String url) {
    try {
      new URL(url).toURI();
    } catch (MalformedURLException | URISyntaxException e) {
      return false;
    }
    return true;
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
        data[pos++] = (byte)(rgb >> 16);
        data[pos++] = (byte)(rgb >> 8);
        data[pos++] = (byte)rgb;
        pos++;
      }
    }
    System.out.print(imageData.dump());
  }

  private int getParameter(List<String> arguments, int index) throws Exception {
    if (arguments.size() > index) {
      return Integer.parseInt(arguments.get(index));
    } else {
      throw new Exception("Parameter value was not provided!");
    }
  }

  private void run(String... args)
      throws Exception, IOException, NumberFormatException {
    List<String> arguments = Arrays.asList(args);
    List<String> files = new ArrayList();

    int index = 0;
    while (index < arguments.size()) {
      String argument = arguments.get(index);
      int nextIndex = index + 1;
      boolean isWidth = argument.equals("-width");
      boolean isHeight = argument.equals("-height");
      if (isWidth || isHeight) {
        if (isWidth) {
          mWidth = getParameter(arguments, nextIndex);
        } else if (isHeight) {
          mHeight = getParameter(arguments, nextIndex);
        }
        index += 1;
      } else {
        if (argument.startsWith("-")) {
          throw new Exception("Unrecognized parameter name!");
        } else {
          files.add(argument);
        }
      }
      index += 1;
    }

    int filesCount = files.size();
    if (filesCount == 0) {
      throw new Exception(
          "Usage:\n\tTerminalImageViewer [-width WIDTH] [-height HEIGHT] FILE...");
    } else if (filesCount == 1) {
      String filepath = files.get(0);
      BufferedImage original = loadImage(filepath);

      int ow = original.getWidth();
      int oh = original.getHeight();
      int w = mWidth * 4;
      int mh = mHeight * 8;
      int h = oh * w / ow;

      if (h > mh) {
        h = mh;
        w = ow * h / oh;
      }

      if (w == ow) {
        dump(original);
      } else {
        BufferedImage image =
            new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(original, 0, 0, w, h, null);
        dump(image);
      }
    } else { // Directory-style rendering.
      List<List<String>> imageSlices = slice(files, 4);

      int w = mWidth * 4;
      int cw = (w - 2 * 3 * 4) / 16;
      int tw = cw * 4;

      for (List<String> imagesRow : imageSlices) {
        BufferedImage image =
            new BufferedImage(tw * 4 + 24, tw, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        int count = 0;
        for (String filepath : imagesRow) {
          BufferedImage original = loadImage(filepath);
          int th = original.getHeight() * tw / original.getWidth();
          graphics.drawImage(original, count * (tw + 8), (tw - th) / 2, tw, th,
                             null);
          count++;
        }
        dump(image);
        System.out.println();
      }
    }
  }

  /**
   * ANSI control code helpers.
   */
  static class Ansi {
    public static final String RESET = "\u001b[0m";

    public static String fgColor(int[] color) {
      int r = color[0], g = color[1], b = color[2];
      return "\u001b[38;2;" + (r & 255) + ";" + (g & 255) + ";" + (b & 255) +
          "m";
    }

    public static String bgColor(int[] color) {
      int r = color[0], g = color[1], b = color[2];
      return "\u001b[48;2;" + (r & 255) + ";" + (g & 255) + ";" + (b & 255) +
          "m";
    }
  }

  /**
   * Converts 4x8 RGB pixel to a unicode character and a foreground and
   * background color:
   * Uses a variation of the median cut algorithm to determine a two-color
   * palette for the
   * character, then creates a corresponding bitmap for the partial image
   * covered by the
   * character and finds the best match in the character bitmap table.
   */
  static class BlockChar {

    /**
     * Assumed bitmaps of the supported characters.
     */
    static int[] BITMAPS = new int[] {
        0x00000000, ' ',

        // Block graphics

        // 0xffff0000, '\u2580',  // upper 1/2; redundant with inverse lower
        // 1/2

        0x0000000f, '\u2581',                       // lower 1/8
        0x000000ff, '\u2582',                       // lower 1/4
        0x00000fff, '\u2583', 0x0000ffff, '\u2584', // lower 1/2
        0x000fffff, '\u2585', 0x00ffffff, '\u2586', // lower 3/4
        0x0fffffff, '\u2587',
        // 0xffffffff, '\u2588',  // Full; redundant with inverse space

        0xeeeeeeee, '\u258a', // Left 3/4
        0xcccccccc, '\u258c', // Left 1/2
        0x88888888, '\u258e', // Left 1/4

        0x0000cccc, '\u2596', // Quadrant lower left
        0x00003333, '\u2597', // Quadrant lower right
        0xcccc0000, '\u2598', // Quadrant upper left
        // 0xccccffff, '\u2599',  // 3/4 redundant with inverse 1/4
        0xcccc3333, '\u259a', // Diagonal 1/2
        // 0xffffcccc, '\u259b',  // 3/4 redundant
        // 0xffff3333, '\u259c',  // 3/4 redundant
        0x33330000, '\u259d', // Quadrant upper right
        // 0x3333cccc, '\u259e',  // 3/4 redundant
        // 0x3333ffff, '\u259f',  // 3/4 redundant

        // Line drawing subset: no double lines, no complex light lines.
        // Simple light lines duplicated because there is no center pixel in
        // the 4x8 matrix.

        0x000ff000, '\u2501', // Heavy horizontal
        0x66666666, '\u2503', // Heavy vertical

        0x00077666, '\u250f', // Heavy down and right
        0x000ee666, '\u2513', // Heavy down and left
        0x66677000, '\u2517', // Heavy up and right
        0x666ee000, '\u251b', // Heavy up and left

        0x66677666, '\u2523', // Heavy vertical and right
        0x666ee666, '\u252b', // Heavy vertical and left
        0x000ff666, '\u2533', // Heavy down and horizontal
        0x666ff000, '\u253b', // Heavy up and horizontal
        0x666ff666, '\u254b', // Heavy cross

        0x000cc000, '\u2578', // Bold horizontal left
        0x00066000, '\u2579', // Bold horizontal up
        0x00033000, '\u257a', // Bold horizontal right
        0x00066000, '\u257b', // Bold horizontal down

        0x06600660, '\u254f', // Heavy double dash vertical

        0x000f0000, '\u2500', // Light horizontal
        0x0000f000, '\u2500', //
        0x44444444, '\u2502', // Light vertical
        0x22222222, '\u2502',

        0x000e0000, '\u2574', // Light left
        0x0000e000, '\u2574', // Light left
        0x44440000, '\u2575', // Light up
        0x22220000, '\u2575', // Light up
        0x00030000, '\u2576', // Light right
        0x00003000, '\u2576', // Light right
        0x00004444, '\u2575', // Light down
        0x00002222, '\u2575', // Light down

        // Misc technical

        0x44444444, '\u23a2', // [ extension
        0x22222222, '\u23a5', // ] extension

        // 12345678
        0x0f000000, '\u23ba', // Horizontal scan line 1
        0x00f00000, '\u23bb', // Horizontal scan line 3
        0x00000f00, '\u23bc', // Horizontal scan line 7
        0x000000f0, '\u23bd', // Horizontal scan line 9

        // Geometrical shapes. Tricky because some of them are too wide.

        // 0x00ffff00, '\u25fe',  // Black medium small square
        0x00066000, '\u25aa', // Black small square

        // 0x11224488, '\u2571',  // diagonals
        // 0x88442211, '\u2572',
        // 0x99666699, '\u2573',

        // 0x000137f0, '\u25e2',  // Triangles
        // 0x0008cef0, '\u25e3',
        // 0x000fec80, '\u25e4',
        // 0x000f7310, '\u25e5'
    };

    /**
     * Minimum value for each color channel.
     */
    int[] min = new int[3];

    /**
     * Maximum value for each color channel.
     */
    int[] max = new int[3];

    /**
     * Red, green and blue components of the selected background color.
     */
    int[] bgColor = new int[3];

    /**
     * Red, green and blue components of the selected background color.
     */
    int[] fgColor = new int[3];

    /**
     * The selected character.
     */
    char character;

    /**
     * Converts a set of pixels to a unicode character and a background and
     * foreground color.
     * data contains the rgba values, p0 is the start point in data and
     * scanWidth the number
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
          pos++; // Alpha
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
      // We just slice at the middle of the interval instead of computing the
      // median.
      int splitValue = min[splitIndex] + bestSplit / 2;

      // Compute a bitmap using the given slice and sum the color values for
      // both buckets.
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
          pos++; // Alpha
        }
        pos += scanWidth - 16;
      }

      // Calculate the average color value for each bucket.
      for (int i = 0; i < 3; i++) {
        if (bgCount != 0) {
          bgColor[i] /= bgCount;
        }
        if (fgCount != 0) {
          fgColor[i] /= fgCount;
        }
      }

      // Find the best bitmap match by counting the bits that don't match,
      // including
      // the inverted bitmaps.
      int bestDiff = Integer.MAX_VALUE;
      boolean invert = false;
      for (int i = 0; i < BITMAPS.length; i += 2) {
        int diff = Integer.bitCount(BITMAPS[i] ^ bits);
        if (diff < bestDiff) {
          character = (char)BITMAPS[i + 1];
          bestDiff = diff;
          invert = false;
        }
        diff = Integer.bitCount((~BITMAPS[i]) ^ bits);
        if (diff < bestDiff) {
          character = (char)BITMAPS[i + 1];
          bestDiff = diff;
          invert = true;
        }
      }

      // If the match is quite bad, use a shade image instead.
      if (bestDiff > 10) {
        invert = false;
        character =
            " \u2591\u2592\u2593\u2588".charAt(Math.min(4, fgCount * 5 / 32));
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
     * Convert the image to an Ansi control character string setting the
     * colors.
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
          String fg = Ansi.fgColor(blockChar.fgColor);
          String bg = Ansi.bgColor(blockChar.bgColor);
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
      sb.setLength(sb.length() - 1);
      return sb.toString();
    }
  }
}
