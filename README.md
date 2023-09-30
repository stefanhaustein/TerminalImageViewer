# Terminal Image Viewer (tiv)

Small C++ program to display images in a (modern) terminal using RGB ANSI codes and unicode block graphic characters.

There are various similar tools (such as `timg`) that use the unicode half block character to display two 24bit pixels per character cell. This program enhances the resolution by mapping 4x8 pixel cells to different unicode characters, using the following algorithm for each 4x8 pixel cell of the (potentially downscaled) image:

1. Find the color channel (R, G or B) that has the biggest range of values for the current cell
2. Split this range in the middle and create a corresponding bitmap for the cell
3. Compare the bitmap to the assumed bitmaps for various unicode block graphics characters
4. Re-calculate the foreground and background colors for the chosen character.

See the difference by disabling this optimization using the `-0` option. Or just take a look at the comparison image at the end of this text.

## Usage

```sh
tiv [options] <filename> [<filename>...]
```

The shell will expand wildcards. By default, thumbnails and file names will be displayed if more than one image is provided. For a list of options, run the command without any parameters or with `--help`.

## News

- 2020-10-22: The Java version is now **deprecated**. Development has long shifted to the C++ version since that was created, and the last meaningful update to it was in 2016.
- 2021-05-22: We now support Apple Clang, thanks to the C++ filesystem library being no longer experimental. Issue forms have also been added to the GitHub repository.
- 2023-09-29: Today marks the 40th anniversary of the GNU project. If you haven't learned the news concerning it and Stallman, please do. In project news, @aaronliu0130 will probably be developing this project from now on as the original author is inactive. Support for MSVC has been added and the repository is now under an Apache 2.0 or GPL3 dual license. CI building for each release will hopefully be setup soon. The main program has also adopted a mostly Google code-style because I (aaron) think it simply makes sense.
`SPDX-License-Identifier: Apache-2.0 OR GPL-3.0-or-later`

## Installation

> [!IMPORTANT]
> All installation methods require installing ImageMagick, a required dependency. Most package managers should install it automatically.

### Build from source

Our makefile currently only supports `g++`. It should be possible to compile `tiv` manually using any of your favorite compilers that support C++20 and Unix headers (`ioctl.h`, specifically) or `windows.h`. PRs are welcome.

```sh
git clone https://github.com/stefanhaustein/TerminalImageViewer.git
cd TerminalImageViewer/src
make

# To move the tiv binary into your PATH (hopefully), also do
sudo make install
```

### Homebrew

Option 1:

```sh
brew install tiv
```

Option 2 (deprecated, for macOS Mojave and below):

```sh
brew tap stefanhaustein/TerminalImageViewer https://github.com/stefanhaustein/TerminalImageViewer
brew install terminalimageviewer
```

### Snap (outdated)

    sudo snap install --edge tiv

## Packages

- @aaronliu0130 has added homebrew support.
- @megamaced has created [an RPM for SUSE](https://build.opensuse.org/package/show/home:megamaced/terminalimageviewer)
- @bperel has created [a Docker image](https://hub.docker.com/r/bperel/terminalimageviewer)
- @teresaejunior has created a snapcraft.yaml file, which can build a Snap package with `sudo docker run -it --rm -v "$PWD:$PWD" -w "$PWD" snapcore/snapcraft sh -c 'apt-get update && snapcraft'`, and then installed with `sudo snap install --dangerous ./*.snap`. This is no longer supported.
- An AUR package is also available, though the maintainer appears to be inactive. Replacements coming soon.

## Contributions

- 2019-03-26: Exciting week: @Cableo has fixed output redirection, @boretom has added cross-compilation support to the build file and @AlanDeSmet has fixed tall thumbnails and greyscale images.
- 2020-07-05: @cxwx has fixed homebrew support.

I am happy to accept useful contributions under the Apache 2.0 licencse, but...

- Before investing in larger contributions, please use an issue to discuss this
- Pull requests should be as "atomic" as possible. I won't accept any pull request doing multiple things at once.
- This program currently only depends on CImg and ImageMagick as image processing libraries and I'd prefer to keep it that way.
- Support for additional platforms, CPUs or similar will require somebody who is happy to help with maintenance, in particular if I don't have access to it.

## Common problems

- On some linux platforms, an extra flag seems to be required: `make LDLIBS=-lstdc++fs` (but it also breaks MacOs), see  <https://github.com/stefanhaustein/TerminalImageViewer/issues/103>
- If you see strange horizontal lines, the characters don't fully fill the character cell. Remove additional line spacing in your terminal app
- Wrong colors? Try -256 to use a 256 color palette instead of 24 bit colors
- Strange characters? Try -0 or install an use full unicode font (e.g. inconsolata or firacode)

## Examples

Most examples were shot with the Java version of this program, which should have equivalent output but slower by millenia in CPU years.

![Examples](https://i.imgur.com/yWRZ3yk.png)

If multiple images match the filename spec, thumbnails are shown.

![Thumbnails](https://i.imgur.com/PTYgSqz.png)

For the example below, the top image was generated with the character optimization disabled via the `-0` option.

![Comparison](https://i.imgur.com/OzdCeh6.png)
