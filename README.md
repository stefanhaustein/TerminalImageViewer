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

- 2024-03-20: Added a section on how to use the API.
- 2024-02-01: We are currently working on splitting the source code into dependency-free library files and a client that uses CImg.
- 2023-09-29: Today marks the 40th anniversary of the GNU project. If you haven't learned the news concerning it and Stallman, please do. 
Support for MSVC has been added and the repository is now under an Apache 2.0 or GPL3 dual license. CI building for each release will hopefully be setup soon. The main program has also adopted a mostly Google code-style because I (aaron) think it simply makes sense.
`SPDX-License-Identifier: Apache-2.0 OR GPL-3.0-or-later`
- 2021-05-22: We now support Apple Clang, thanks to the C++ filesystem library being no longer experimental. Issue forms have also been added to the GitHub repository.
- 2020-10-22: The Java version is now **deprecated**. Development has long shifted to the C++ version since that was created, and the last meaningful update to it was in 2016.

## Installation

> [!IMPORTANT]
> All installation methods require installing ImageMagick, a required dependency. Most package managers should install it automatically.

### All platforms: Build from source

Our makefile currently only supports `g++`. It should be possible to compile `tiv` manually using any of your favorite compilers that support C++17 and Unix headers (`ioctl.h` and `sysexits.h`, specifically) or `windows.h`. PRs are welcome.

```sh
git clone https://github.com/stefanhaustein/TerminalImageViewer.git
cd TerminalImageViewer/src
make

# To move the tiv binary into your PATH (hopefully), also do
sudo make install
```

Please don't forget to install ImageMagick... On Debian based Linux via `sudo apt install imagemagick` and
on MacOS via `brew install imagemagick`.

### Mac: Homebrew


```sh
brew install tiv
```

As the original Apple Shell only supports 256 color mode (-256) and there seems to be some extra 
line spacing, distorting the image, we also recommend installing iTerm2:

```
brew install --cask iterm2
```

### Third-Party Packages

- @megamaced has created [an RPM for SUSE](https://build.opensuse.org/package/show/home:megamaced/terminalimageviewer)
- @bperel has created [a Docker image](https://hub.docker.com/r/bperel/terminalimageviewer)


## Common problems / Troubleshooting

- Errors such as "unrecognized file format"? Make sure ImageMagic is installed.
- On some linux platforms, an extra flag seems to be required: `make LDLIBS=-lstdc++fs` (but it also breaks MacOs), see  <https://github.com/stefanhaustein/TerminalImageViewer/issues/103>
- If you see strange horizontal lines, the characters don't fully fill the character cell. Remove additional line spacing in your terminal app
- Wrong colors? Try -256 to use a 256 color palette instead of 24 bit colors
- Strange characters? Try -0 or install an use full unicode font (e.g. inconsolata or firacode)

## Using the TIV API

Tiv can be used as an API. So if you always wanted to run your favorite FPS in a shell, this is the opportunity.

All the code useful as a library is isolated in [tiv_lib.h](https://github.com/stefanhaustein/TerminalImageViewer/blob/master/src/tiv_lib.h) 
and [tiv_lib.cc](https://github.com/stefanhaustein/TerminalImageViewer/blob/master/src/tiv_lib.cc).

The main entry point is 

```CharData findCharData(GetPixelFunction get_pixel, int x0, int y0,
                         const int &flags)
```

The call takes a std::Function that allows the TIV code to request pixels from your framebuffer.

From this framebuffer, the call will query pixels for a 4x8 pixel rectangle, where x0 and y0 
define the top left corner. The call searches the best unicode graphics character and colors to approximate this 
cell of the image, and returns these in a CharData struct.




## Contributions

- 2019-03-26: Exciting week: @cabelo has fixed output redirection, @boretom has added cross-compilation support to the build file and @AlanDeSmet has fixed tall thumbnails and greyscale images.
- 2020-07-05: @cxwx has fixed homebrew support.

I am happy to accept useful contributions under the Apache 2.0 license, but...

- Before investing in larger contributions, please use an issue to discuss this
- Pull requests should be as "atomic" as possible. I won't accept any pull request doing multiple things at once.
- This program currently only depends on CImg and ImageMagick as image processing libraries and I'd prefer to keep it that way.
- Support for additional platforms, CPUs or similar will require somebody who is happy to help with maintenance, in particular if I don't have access to it.

## Examples

Most examples were shot with the Java version of this program, which should have equivalent output but slower by millenia in CPU years.

![Examples](https://i.imgur.com/yWRZ3yk.png)

If multiple images match the filename spec, thumbnails are shown.

![Thumbnails](https://i.imgur.com/PTYgSqz.png)

For the example below, the top image was generated with the character optimization disabled via the `-0` option.

![Comparison](https://i.imgur.com/OzdCeh6.png)

## Licensing

You are free to use this code under either the GPL (3 or later) or version 2.0 of the Apache license. We include the CImg library, which
is licensed under either [CeCILL 2.0](https://spdx.org/licenses/CECILL-2.0.html) (close to GPL and compatible with it) or [CeCILL-C] (https://spdx.org/licenses/CECILL-C) (close to LGPL and compatible with Apache).
