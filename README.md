# TerminalImageViewer

Small Java program to display images in a (modern) terminal using RGB ANSI codes and unicode block graphic characters

## Algorithm
For each 4x8 pixel cell mapped to a unicode block graphics character:
1. Find the color channel (R, G or B) that has the biggest range of values for the current cell.
2. Split this range in the middle.
3. Average the colors above and below and create a corresponding bitmap for the cell.
4. Compare the bitmap to the assumed bitmaps for various unicode block graphics characters.


## Usage

### javac & java

```
javac TerminalImageViewer.java
java TerminalImageViewer [-weight <width-in-characters>] [-height <max-height-in-characters>] <image-filename-or-url>
```

### gradle & bash

```
./gradlew build
./tiv.sh [-width <width-in-characters>] [-height <max-height-in-characters>] <image-filename-or-url>
```

## Examples

Try the `cats.sh` script!


![Examples](http://i.imgur.com/8UyGjg8.png)

If multiple images match the filename spec, thumbnails are shown.

![Thumbnails](http://i.imgur.com/PTYgSqz.png)
