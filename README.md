# TerminalImageViewer

Small Java program to display images in a (modern) terminal using RGB ANSI codes and unicode block graphic characters; a C++ port is available at https://github.com/stefanhaustein/tiv

Algorithm (for each 4x8 pixel cell mapped to a unicode block graphics character):

1. Find the color channel (R, G or B) that has the biggest range of values for the current cell
2. Split this range in the middle 
3. Average the colors above and below and create a corresponding bitmap for the cell
3. Compare the bitmap to the assumed bitmaps for various unicode block graphics characters


## Usage

```
javac TerminalImageViewer.java

java TerminalImageViewer [-w <width-in-characters>] <image-filename-or-url>

```

## Common problems

 - If you see strange horizontal lines, the characters don't fully fill the character cell. Remove additional line spacing in your terminal app
 - Wrong colors? Try -256 to use a 256 color palette instead of 24 bit colors or -grayscale for grayscale.

## Examples

![Examples](http://i.imgur.com/8UyGjg8.png)

If multiple images match the filename spec, thumbnails are shown.

![Thumbnails](http://i.imgur.com/PTYgSqz.png)
