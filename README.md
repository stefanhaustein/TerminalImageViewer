# TerminalImageViewer

Small Java program to display images in a (modern) terminal using RGB ANSI codes and unicode block graphic characters

Algorithm (for each 4x8 pixel cell mapped to a unicode block graphics character):

1. Find the color channel that has the biggest range of values for the current cell
2. Split the range in the middle 
3. Average the colors above and below and create a corresponding bitmap for the cell
3. Compare the bitmap to the assumed bitmaps for the unicode block graphics characters


Usage:

```
javac TerminalImageViewer.java

java TerminalImageViewer [-w <width-in-characters>] <image-filename-or-url>

```

If multiple images match the filename spec, thumbnails are shown.

![Examples](http://i.imgur.com/8UyGjg8.png)

