/*
 * Copyright (c) 2017-2023, Stefan Haustein, Aaron Liu
 *
 *     This file is free software: you may copy, redistribute and/or modify it
 *     under the terms of the GNU General Public License as published by the
 *     Free Software Foundation, either version 3 of the License, or (at your
 *     option) any later version.
 *
 *     This file is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Alternatively, you may copy, redistribute and/or modify this file under
 * the terms of the Apache License, version 2.0:
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         https://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

#include <array>
#include <bitset>
#include <cmath>
#include <filesystem>
#include <fstream>
#include <functional>
#include <iostream>
#include <map>
#include <string>
#include <vector>

#include "tiv_lib.h"

// This #define tells CImg that we use the library without any display options
// -- just for loading images.
#define cimg_display 0
#include "CImg.h"

#ifdef _POSIX_VERSION
// Console output size detection
#include <sys/ioctl.h>
// Error explanation, for some reason
#include <cstring>
#endif

#ifdef _WIN32
#include <windows.h>
// Error explanation
#include <system_error>
#endif

// Program exit code constants compatible with sysexits.h.
#define EXITCODE_OK 0
#define EXITCODE_COMMAND_LINE_USAGE_ERROR 64
#define EXITCODE_DATA_FORMAT_ERROR 65
#define EXITCODE_NO_INPUT_ERROR 66

void printTermColor(const int &flags, int r, int g, int b) {
    r = clamp_byte(r);
    g = clamp_byte(g);
    b = clamp_byte(b);

    bool bg = (flags & FLAG_BG) != 0;

    if ((flags & FLAG_MODE_256) == 0) {
        std::cout << (bg ? "\x1b[48;2;" : "\x1b[38;2;") << r << ';' << g << ';'
                  << b << 'm';
        return;
    }

    int ri = best_index(r, COLOR_STEPS, COLOR_STEP_COUNT);
    int gi = best_index(g, COLOR_STEPS, COLOR_STEP_COUNT);
    int bi = best_index(b, COLOR_STEPS, COLOR_STEP_COUNT);

    int rq = COLOR_STEPS[ri];
    int gq = COLOR_STEPS[gi];
    int bq = COLOR_STEPS[bi];

    int gray =
        static_cast<int>(std::round(r * 0.2989f + g * 0.5870f + b * 0.1140f));

    int gri = best_index(gray, GRAYSCALE_STEPS, GRAYSCALE_STEP_COUNT);
    int grq = GRAYSCALE_STEPS[gri];

    int color_index;
    if (0.3 * sqr(rq - r) + 0.59 * sqr(gq - g) + 0.11 * sqr(bq - b) <
        0.3 * sqr(grq - r) + 0.59 * sqr(grq - g) + 0.11 * sqr(grq - b)) {
        color_index = 16 + 36 * ri + 6 * gi + bi;
    } else {
        color_index = 232 + gri;  // 1..24 -> 232..255
    }
    std::cout << (bg ? "\x1B[48;5;" : "\u001B[38;5;") << color_index << "m";
}

void printCodepoint(int codepoint) {
    if (codepoint < 128) {
        std::cout << static_cast<char>(codepoint);
    } else if (codepoint < 0x7ff) {
        std::cout << static_cast<char>(0xc0 | (codepoint >> 6));
        std::cout << static_cast<char>(0x80 | (codepoint & 0x3f));
    } else if (codepoint < 0xffff) {
        std::cout << static_cast<char>(0xe0 | (codepoint >> 12));
        std::cout << static_cast<char>(0x80 | ((codepoint >> 6) & 0x3f));
        std::cout << static_cast<char>(0x80 | (codepoint & 0x3f));
    } else if (codepoint < 0x10ffff) {
        std::cout << static_cast<char>(0xf0 | (codepoint >> 18));
        std::cout << static_cast<char>(0x80 | ((codepoint >> 12) & 0x3f));
        std::cout << static_cast<char>(0x80 | ((codepoint >> 6) & 0x3f));
        std::cout << static_cast<char>(0x80 | (codepoint & 0x3f));
    } else {
        std::cerr << "ERROR";
    }
}

void printImage(const cimg_library::CImg<unsigned char> &image,
                const int &flags) {
    GetPixelFunction get_pixel = [&](int x, int y) -> unsigned long {
        return (((unsigned long) image(x, y, 0, 0)) << 16) 
            | (((unsigned long) image(x, y, 0, 1)) << 8)
            | (((unsigned long) image(x, y, 0, 2)));
    };

    CharData lastCharData;
    for (int y = 0; y <= image.height() - 8; y += 8) {
        for (int x = 0; x <= image.width() - 4; x += 4) {
            CharData charData =
                flags & FLAG_NOOPT
                    ? createCharData(get_pixel, x, y, 0x2584, 0x0000ffff)
                    : findCharData(get_pixel, x, y, flags);
            if (x == 0 || charData.bgColor != lastCharData.bgColor)
                printTermColor(flags | FLAG_BG, charData.bgColor[0],
                               charData.bgColor[1], charData.bgColor[2]);
            if (x == 0 || charData.fgColor != lastCharData.fgColor)
                printTermColor(flags | FLAG_FG, charData.fgColor[0],
                               charData.fgColor[1], charData.fgColor[2]);
            printCodepoint(charData.codePoint);
            lastCharData = charData;
        }
        std::cout << "\x1b[0m" << std::endl;
    }
}

struct size {
    size(unsigned int in_width, unsigned int in_height)
        : width(in_width), height(in_height) {}
    explicit size(cimg_library::CImg<unsigned int> img)
        : width(img.width()), height(img.height()) {}
    unsigned int width;
    unsigned int height;
    size scaled(double scale) { return size(width * scale, height * scale); }
    size fitted_within(size container) {
        double scale = std::min(container.width / static_cast<double>(width),
                                container.height / static_cast<double>(height));
        return scaled(scale);
    }
};
std::ostream &operator<<(std::ostream &stream, size sz) {
    stream << sz.width << "x" << sz.height;
    return stream;
}

/**
 * @brief Wrapper around CImg<T>(const char*) constructor
 * that always returns a CImg image with 3 channels (RGB)
 *
 * @param filename The file to construct a CImg object on
 * @return cimg_library::CImg<unsigned char> Constructed CImg RGB image
 */
cimg_library::CImg<unsigned char> load_rgb_CImg(const char *const &filename) {
    cimg_library::CImg<unsigned char> image(filename);
    if (image.spectrum() == 1) {
        // Greyscale. Just copy greyscale data to all channels
        cimg_library::CImg<unsigned char> rgb_image(
            image.width(), image.height(), image.depth(), 3);
        for (unsigned int chn = 0; chn < 3; chn++) {
            rgb_image.draw_image(0, 0, 0, chn, image);
        }
        return rgb_image;
    }
    return image;
}

// Implements --help
void printUsage() {
    std::cerr << R"(
Terminal Image Viewer v1.2.1
usage: tiv [options] <image> [<image>...]
-0        : No block character adjustment, always use top half block char.
-2, --256 : Use 256-bit colors. Needed to display properly on macOS Terminal.
-c <num>  : Number of thumbnail columns in 'dir' mode (3 by default).
-d, --dir : Force 'dir' mode. Automatially selected for more than one input.
-f, --full: Force 'full' mode. Automatically selected for one input.
--help    : Display this help text.
-h <num>  : Set the maximum output height to <num> lines.
-w <num>  : Set the maximum output width to <num> characters.
-x        : Use new Unicode Teletext/legacy characters (experimental).)"
              << std::endl;
}

enum Mode { AUTO, THUMBNAILS, FULL_SIZE };

int main(int argc, char *argv[]) {
    std::ios::sync_with_stdio(false);  // apparently makes printing faster
    bool detectSize = true;

    // Platform-specific implementations for determining console size, better
    // implementations are welcome Fallback sizes when unsuccesful
    int maxWidth = 80;
    int maxHeight = 24;

    // Reading input
    char flags = 0;    // bitwise representation of flags,
                       // see https://stackoverflow.com/a/14295472
    Mode mode = AUTO;  // either THUMBNAIL or FULL_SIZE
    int columns = 3;

    std::vector<std::string> file_names;
    int ret = EXITCODE_OK;  // The return code for the program

    if (argc <= 1) {
        printUsage();
        return EXITCODE_COMMAND_LINE_USAGE_ERROR;
    }

    for (int i = 1; i < argc; i++) {
        std::string arg(argv[i]);
        if (arg == "-0") {
            flags |= FLAG_NOOPT;
        } else if (arg == "-c") {
            if (i < argc - 1) {
                columns = std::stoi(argv[++i]);
            } else {
                std::cerr << "Error: -c requires a number" << std::endl;
                ret = EXITCODE_COMMAND_LINE_USAGE_ERROR;
            }
        } else if (arg == "-d" || arg == "--dir") {
            mode = THUMBNAILS;
        } else if (arg == "-f" || arg == "--full") {
            mode = FULL_SIZE;
        } else if (arg == "-w") {
            if (i < argc - 1) {
                maxWidth = 4 * std::stoi(argv[++i]), detectSize = false;
            } else {
                std::cerr << "Error: -w requires a number" << std::endl;
                ret = EXITCODE_COMMAND_LINE_USAGE_ERROR;
            }
        } else if (arg == "-h") {
            if (i < argc - 1)
                maxHeight = 8 * std::stoi(argv[++i]), detectSize = false;
            else
                printUsage();  // people might confuse this with help
        } else if (arg == "--256" || arg == "-2" || arg == "-256") {
            flags |= FLAG_MODE_256;
        } else if (arg == "--help" || arg == "-help") {
            printUsage();
        } else if (arg == "-x") {
            flags |= FLAG_TELETEXT;
        } else if (arg[0] == '-') {
            std::cerr << "Error: Unrecognized argument: " << arg << std::endl;
            ret = EXITCODE_COMMAND_LINE_USAGE_ERROR;
        } else {
            // Arguments that will be displayed
            if (std::filesystem::is_directory(arg)) {
                for (auto &p : std::filesystem::directory_iterator(arg))
                    if (std::filesystem::is_regular_file(p.path()))
                        file_names.push_back(p.path().string());
            } else {
                // Check if file can be opened, @TODO find better way
                std::ifstream fin(arg.c_str());
                if (fin) {
                    file_names.push_back(arg);
                } else {
                    std::cerr << "Error: Cannot open '" << arg
                              << "', permission issue?" << std::endl;
                    ret = EXITCODE_NO_INPUT_ERROR;
                }
            }
        }
    }

    if (detectSize) {
#ifdef _POSIX_VERSION
        struct winsize w;
        // If redirecting STDOUT to one file ( col or row == 0, or the previous
        // ioctl call's failed )
        if (ioctl(STDOUT_FILENO, TIOCGWINSZ, &w) != 0 ||
            (w.ws_col | w.ws_row) == 0) {
            std::cerr << "Warning: failed to determine most reasonable size: "
                      << strerror(errno) << ", defaulting to 20x6" << std::endl;
        } else {
            maxWidth = w.ws_col * 4;
            maxHeight = w.ws_row * 8;
        }
#elif defined _WIN32
        CONSOLE_SCREEN_BUFFER_INFO w;
        if (GetConsoleScreenBufferInfo(
                GetStdHandle(STD_OUTPUT_HANDLE),
                &w)) {  // just like powershell, but without the hyphens, hooray
            maxWidth = w.dwSize.X * 4;
            maxHeight = w.dwSize.Y * 8;
        } else {
            std::cerr << "Warning: failed to determine most reasonable size: "
                      << std::system_category().message(GetLastError())
                      << ", defaulting to 80x24" << std::endl;
        }
#else
        std::cerr << "Warning: failed to determine most reasonable size: "
                     "unrecognized system, defaulting to 80x24"
                  << std::endl;
#endif
    }

    if (mode == FULL_SIZE || (mode == AUTO && file_names.size() == 1)) {
        for (const auto &filename : file_names) {
            try {
                cimg_library::CImg<unsigned char> image =
                    load_rgb_CImg(filename.c_str());
                if (image.width() > maxWidth || image.height() > maxHeight) {
                    // scale image down to fit terminal size
                    size new_size =
                        size(image).fitted_within(size(maxWidth, maxHeight));
                    image.resize(new_size.width, new_size.height, -100, -100,
                                 5);
                }
                // the acutal magic which generates the output
                printImage(image, flags);
            } catch (cimg_library::CImgIOException &e) {
                std::cerr << "Error: '" << filename
                          << "' has an unrecognized file format" << std::endl;
                ret = EXITCODE_DATA_FORMAT_ERROR;
            }
        }
    } else {  // Thumbnail mode
        unsigned int index = 0;
        int cw = (((maxWidth / 4) - 2 * (columns - 1)) / columns);
        int tw = cw * 4;
        cimg_library::CImg<unsigned char> image(
            tw * columns + 2 * 4 * (columns - 1), tw, 1, 3);
        size maxThumbSize(tw, tw);

        while (index < file_names.size()) {
            image.fill(0);
            int count = 0;
            std::string sb;
            while (index < file_names.size() && count < columns) {
                std::string name = file_names[index++];
                try {
                    cimg_library::CImg<unsigned char> original =
                        load_rgb_CImg(name.c_str());
                    auto cut = name.find_last_of("/");
                    sb +=
                        cut == std::string::npos ? name : name.substr(cut + 1);
                    size newSize = size(original).fitted_within(maxThumbSize);
                    original.resize(newSize.width, newSize.height, 1, -100, 5);
                    image.draw_image(
                        count * (tw + 8) + (tw - newSize.width) / 2,
                        (tw - newSize.height) / 2, 0, 0, original);
                    count++;
                    unsigned int sl = count * (cw + 2);
                    sb.resize(sl - 2, ' ');
                    sb += "  ";
                } catch (std::exception &e) {
                    // Probably no image; ignore.
                }
            }
            if (count) printImage(image, flags);
            std::cout << sb << std::endl << std::endl;
        }
    }
    return ret;
}
