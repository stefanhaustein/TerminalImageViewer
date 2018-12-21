# TerminalImageViewer (tiv)

Small C++ program to display images in a (modern) terminal using RGB ANSI codes and unicode block graphic characters.

There are various similar tools (such as `timg`) using the unicode half block character to display two 24bit pixels per character cell. This program enhances the resolution by mapping 4x8 pixel cells to different unicode characters, using the following algorithm:

For each 4x8 pixel cell of the (potentially downscaled) image:

1. Find the color channel (R, G or B) that has the biggest range of values for the current cell
2. Split this range in the middle and create a corresponding bitmap for the cell
4. Compare the bitmap to the assumed bitmaps for various unicode block graphics characters
5. Re-calculate the foreground and background colors for the chosen character.

See the difference by disabling this optimization using the `-0` option. Or just take a look at the comparison image at the end of this text.

## "News"

- We'd appreciate endorsement here if you like the tool: https://github.com/k4m4/terminals-are-sexy/pull/198

## Installation

    sudo apt install imagemagick || yum install ImageMagick
    git clone https://github.com/stefanhaustein/TerminalImageViewer.git
    cd TerminalImageViewer/src/main/cpp
    make
    sudo make install

## Usage

    tiv [options] <filename(s)>

The shell will expand wildcards. By default, thumbnails and file names will be displayed if more than one image is provided. To display a list of options, just run the command without any parameters. 

## Packages / Contributions

 - megamaced has created a RPM for SUSE:
   https://build.opensuse.org/package/show/home:megamaced/terminalimageviewer
 - bperel has created a Docker image:
   https://hub.docker.com/r/bperel/terminalimageviewer
 - teresaejunior has created a snapcraft.yaml file, which you can use to build a Snap package.
   tiv will soon be available in the Snap Store.

## Common problems

 - If you see strange horizontal lines, the characters don't fully fill the character cell. Remove additional line spacing in your terminal app
 - Wrong colors? Try -256 to use a 256 color palette instead of 24 bit colors
 
## Examples

![Examples](https://i.imgur.com/8UyGjg8.png)

If multiple images match the filename spec, thumbnails are shown.

![Thumbnails](https://i.imgur.com/PTYgSqz.png)

## Comparison to Using Half-Block Characters Only

The top image was generated with the character optimization disabled via the `-0` option.

![Comparison](https://i.imgur.com/OzdCeh6.png)

