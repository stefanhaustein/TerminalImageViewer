# TerminalImageViewer (tiv)

Small C++ program to display images in a (modern) terminal using RGB ANSI codes and unicode block graphic characters.

The main difference to various existing similar tools is that instead of having two fixed pixels per character
cell utilizing the unicode half block character, this program enhances the resolution using the following algorithm:

For each 4x8 pixel cell mapped to a unicode block graphics character:

1. Find the color channel (R, G or B) that has the biggest range of values for the current cell
2. Split this range in the middle and create a corresponding bitmap for the cell
4. Compare the bitmap to the assumed bitmaps for various unicode block graphics characters
5. Re-calculate the foregound and background colors for the chosen character.

Please find a comparsion image at the end of this file.

## Installation

    git clone https://github.com/stefanhaustein/TerminalImageViewer.git
    cd TerminalImageViewer/src/main/cpp
    make
    sudo make install

## Usage

    tiv <filenames>

## Common problems

 - If you see strange horizontal lines, the characters don't fully fill the character cell. Remove additional line spacing in your terminal app
 - Wrong colors? Try -256 to use a 256 color palette instead of 24 bit colors
 
## Examples

![Examples](http://i.imgur.com/8UyGjg8.png)

If multiple images match the filename spec, thumbnails are shown.

![Thumbnails](http://i.imgur.com/PTYgSqz.png)

## Comparison to Using Half-Block Characters Only

![Comparison](http://i.imgur.com/OzdCeh6.png)

