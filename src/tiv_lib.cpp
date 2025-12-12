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

#include "tiv_lib.h"

#include <algorithm>
#include <array>
#include <bitset>
#include <cmath>
#include <functional>
#include <map>

const int END_MARKER = 0;

// An interleaved map of 4x8 bit character bitmaps (each hex digit represents a
// row) to the corresponding Unicode character code point.
constexpr unsigned int BITMAPS[] = {
    0x00000000, 0x00a0, 0,

    // Block graphics
    // 0xffff0000, 0x2580, 0,  // upper 1/2; redundant with inverse lower 1/2

    0x0000000f, 0x2581, 0,  // lower 1/8
    0x000000ff, 0x2582, 0,  // lower 1/4
    0x00000fff, 0x2583, 0,
    0x0000ffff, 0x2584, 0,  // lower 1/2
    0x000fffff, 0x2585, 0,
    0x00ffffff, 0x2586, 0,  // lower 3/4
    0x0fffffff, 0x2587, 0,
    // 0xffffffff, 0x2588,  // full; redundant with inverse space

    0xeeeeeeee, 0x258a, 0,  // left 3/4
    0xcccccccc, 0x258c, 0,  // left 1/2
    0x88888888, 0x258e, 0,  // left 1/4

    0x0000cccc, 0x2596, 0,  // quadrant lower left
    0x00003333, 0x2597, 0,  // quadrant lower right
    0xcccc0000, 0x2598, 0,  // quadrant upper left
             // 0xccccffff, 0x2599,  // 3/4 redundant with inverse 1/4
    0xcccc3333, 0x259a, 0,  // diagonal 1/2
                         // 0xffffcccc, 0x259b,  // 3/4 redundant
    // 0xffff3333, 0x259c,  // 3/4 redundant
    0x33330000, 0x259d, 0,  // quadrant upper right
                         // 0x3333cccc, 0x259e,  // 3/4 redundant
    // 0x3333ffff, 0x259f,  // 3/4 redundant

    // Line drawing subset: no double lines, no complex light lines

    0x000ff000, 0x2501, 0,  // Heavy horizontal
    0x66666666, 0x2503, 0,  // Heavy vertical

    0x00077666, 0x250f, 0,  // Heavy down and right
    0x000ee666, 0x2513, 0,  // Heavy down and left
    0x66677000, 0x2517, 0,  // Heavy up and right
    0x666ee000, 0x251b, 0,  // Heavy up and left

    0x66677666, 0x2523, 0,  // Heavy vertical and right
    0x666ee666, 0x252b, 0,  // Heavy vertical and left
    0x000ff666, 0x2533, 0,  // Heavy down and horizontal
    0x666ff000, 0x253b, 0,  // Heavy up and horizontal
    0x666ff666, 0x254b, 0,  // Heavy cross

    0x000cc000, 0x2578, 0,  // Bold horizontal left
    0x00066000, 0x2579, 0,  // Bold horizontal up
    0x00033000, 0x257a, 0,  // Bold horizontal right
    0x00066000, 0x257b, 0,  // Bold horizontal down

    0x06600660, 0x254f, 0,  // Heavy double dash vertical

    0x000f0000, 0x2500, 0,  // Light horizontal
    0x0000f000, 0x2500, 0,  //
    0x44444444, 0x2502, 0,  // Light vertical
    0x22222222, 0x2502, 0,

    0x000e0000, 0x2574, 0,  // light left
    0x0000e000, 0x2574, 0,  // light left
    0x44440000, 0x2575, 0,  // light up
    0x22220000, 0x2575, 0,  // light up
    0x00030000, 0x2576, 0,  // light right
    0x00003000, 0x2576, 0,  // light right
    0x00004444, 0x2577, 0,  // light down
    0x00002222, 0x2577, 0,  // light down

    // Misc technical

    0x44444444, 0x23a2, 0,  // [ extension
    0x22222222, 0x23a5, 0,  // ] extension

    0x0f000000, 0x23ba, 0,  // Horizontal scanline 1
    0x00f00000, 0x23bb, 0,  // Horizontal scanline 3
    0x00000f00, 0x23bc, 0,  // Horizontal scanline 7
    0x000000f0, 0x23bd, 0,  // Horizontal scanline 9

    // Geometrical shapes. Tricky because some of them are too wide.

    // 0x00ffff00, 0x25fe, 0,  // Black medium small square
    0x00066000, 0x25aa, 0,  // Black small square

    // 0x11224488, 0x2571, 0,  // diagonals
    // 0x88442211, 0x2572, 0,
    // 0x99666699, 0x2573, 0,
    // 0x000137f0, 0x25e2, 0,  // Triangles
    // 0x0008cef0, 0x25e3, 0,
    // 0x000fec80, 0x25e4, 0,
    // 0x000f7310, 0x25e5, 0,

    // Teletext / legacy graphics 3x2 block character codes.
    // Using a 3-2-3 pattern consistently, perhaps we should create automatic
    // variations....

    0xccc00000, 0xfb00, FLAG_TELETEXT,
    0x33300000, 0xfb01, FLAG_TELETEXT,
    0xfff00000, 0xfb02, FLAG_TELETEXT,
    0x000cc000, 0xfb03, FLAG_TELETEXT,
    0xccccc000, 0xfb04, FLAG_TELETEXT,
    0x333cc000, 0xfb05, FLAG_TELETEXT,
    0xfffcc000, 0xfb06, FLAG_TELETEXT,
    0x00033000, 0xfb07, FLAG_TELETEXT,
    0xccc33000, 0xfb08, FLAG_TELETEXT,
    0x33333000, 0xfb09, FLAG_TELETEXT,
    0xfff33000, 0xfb0a, FLAG_TELETEXT,
    0x000ff000, 0xfb0b, FLAG_TELETEXT,
    0xcccff000, 0xfb0c, FLAG_TELETEXT,
    0x333ff000, 0xfb0d, FLAG_TELETEXT,
    0xfffff000, 0xfb0e, FLAG_TELETEXT,
    0x00000ccc, 0xfb0f, FLAG_TELETEXT,

    0xccc00ccc, 0xfb10, FLAG_TELETEXT,
    0x33300ccc, 0xfb11, FLAG_TELETEXT,
    0xfff00ccc, 0xfb12, FLAG_TELETEXT,
    0x000ccccc, 0xfb13, FLAG_TELETEXT,
    0x333ccccc, 0xfb14, FLAG_TELETEXT,
    0xfffccccc, 0xfb15, FLAG_TELETEXT,
    0x00033ccc, 0xfb16, FLAG_TELETEXT,
    0xccc33ccc, 0xfb17, FLAG_TELETEXT,
    0x33333ccc, 0xfb18, FLAG_TELETEXT,
    0xfff33ccc, 0xfb19, FLAG_TELETEXT,
    0x000ffccc, 0xfb1a, FLAG_TELETEXT,
    0xcccffccc, 0xfb1b, FLAG_TELETEXT,
    0x333ffccc, 0xfb1c, FLAG_TELETEXT,
    0xfffffccc, 0xfb1d, FLAG_TELETEXT,
    0x00000333, 0xfb1e, FLAG_TELETEXT,
    0xccc00333, 0xfb1f, FLAG_TELETEXT,

    0x33300333, 0x1b20, FLAG_TELETEXT,
    0xfff00333, 0x1b21, FLAG_TELETEXT,
    0x000cc333, 0x1b22, FLAG_TELETEXT,
    0xccccc333, 0x1b23, FLAG_TELETEXT,
    0x333cc333, 0x1b24, FLAG_TELETEXT,
    0xfffcc333, 0x1b25, FLAG_TELETEXT,
    0x00033333, 0x1b26, FLAG_TELETEXT,
    0xccc33333, 0x1b27, FLAG_TELETEXT,
    0xfff33333, 0x1b28, FLAG_TELETEXT,
    0x000ff333, 0x1b29, FLAG_TELETEXT,
    0xcccff333, 0x1b2a, FLAG_TELETEXT,
    0x333ff333, 0x1b2b, FLAG_TELETEXT,
    0xfffff333, 0x1b2c, FLAG_TELETEXT,
    0x00000fff, 0x1b2d, FLAG_TELETEXT,
    0xccc00fff, 0x1b2e, FLAG_TELETEXT,
    0x33300fff, 0x1b2f, FLAG_TELETEXT,

    0xfff00fff, 0x1b30, FLAG_TELETEXT,
    0x000ccfff, 0x1b31, FLAG_TELETEXT,
    0xcccccfff, 0x1b32, FLAG_TELETEXT,
    0x333ccfff, 0x1b33, FLAG_TELETEXT,
    0xfffccfff, 0x1b34, FLAG_TELETEXT,
    0x00033fff, 0x1b35, FLAG_TELETEXT,
    0xccc33fff, 0x1b36, FLAG_TELETEXT,
    0x33333fff, 0x1b37, FLAG_TELETEXT,
    0xfff33fff, 0x1b38, FLAG_TELETEXT,
    0x000fffff, 0x1b39, FLAG_TELETEXT,
    0xcccfffff, 0x1b3a, FLAG_TELETEXT,
    0x333fffff, 0x1b3b, FLAG_TELETEXT,

    0, END_MARKER, 0  // End marker 
};

// The channel indices are 0, 1, 2 for R, G, B
unsigned char get_channel(unsigned long rgb, int index) {
    return (unsigned char) ((rgb >> ((2 - index) * 8)) & 255);
}

CharData createCharData(GetPixelFunction get_pixel, int x0, int y0,
                        int codepoint, int pattern) {
    CharData result;
    result.codePoint = codepoint;
    int fg_count = 0;
    int bg_count = 0;
    unsigned int mask = 0x80000000;

    for (int y = 0; y < 8; y++) {
        for (int x = 0; x < 4; x++) {
            int *avg;
            if (pattern & mask) {
                avg = result.fgColor.data();
                fg_count++;
            } else {
                avg = result.bgColor.data();
                bg_count++;
            }
            long rgb = get_pixel(x0 + x, y0 + y);
            for (int i = 0; i < 3; i++) {
                avg[i] += get_channel(rgb, i);
            }
            mask = mask >> 1;
        }
    }

    // Calculate the average color value for each bucket
    for (int i = 0; i < 3; i++) {
        if (bg_count != 0) {
            result.bgColor[i] /= bg_count;
        }
        if (fg_count != 0) {
            result.fgColor[i] /= fg_count;
        }
    }
    return result;
}

CharData findCharData(GetPixelFunction get_pixel, int x0, int y0,
                      const int &flags) {
    int min[3] = {255, 255, 255};
    int max[3] = {0};
    std::map<long, int> count_per_color;

    // Determine the minimum and maximum value for each color channel
    for (int y = 0; y < 8; y++) {
        for (int x = 0; x < 4; x++) {
            long color = 0;
            long rgb = get_pixel(x0 + x, y0 + y);
            for (int i = 0; i < 3; i++) {
                int d = get_channel(rgb, i);
                min[i] = std::min(min[i], d);
                max[i] = std::max(max[i], d);
                color = (color << 8) | d;
            }
            count_per_color[color]++;
        }
    }

    std::multimap<int, long> color_per_count;
    for (auto i = count_per_color.begin(); i != count_per_color.end(); ++i) {
        color_per_count.insert(std::pair<int, long>(i->second, i->first));
    }

    auto iter = color_per_count.rbegin();
    int count2 = iter->first;
    long max_count_color_1 = iter->second;
    long max_count_color_2 = max_count_color_1;
    if ((++iter) != color_per_count.rend()) {
        count2 += iter->first;
        max_count_color_2 = iter->second;
    }

    unsigned int bits = 0;
    bool direct = count2 > (8 * 4) / 2;

    if (direct) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 4; x++) {
                bits = bits << 1;
                int d1 = 0;
                int d2 = 0;
                unsigned long rgb = get_pixel(x0 + x, y0 + y);
                for (int i = 0; i < 3; i++) {
                    int shift = 16 - 8 * i;
                    int c1 = (max_count_color_1 >> shift) & 255;
                    int c2 = (max_count_color_2 >> shift) & 255;
                    int c = get_channel(rgb, i);
                    d1 += (c1 - c) * (c1 - c);
                    d2 += (c2 - c) * (c2 - c);
                }
                if (d1 > d2) {
                    bits |= 1;
                }
            }
        }
    } else {
        // Determine the color channel with the greatest range.
        int splitIndex = 0;
        int bestSplit = 0;
        for (int i = 0; i < 3; i++) {
            if (max[i] - min[i] > bestSplit) {
                bestSplit = max[i] - min[i];
                splitIndex = i;
            }
        }

        // We just split at the middle of the interval instead of computing the
        // median.
        int splitValue = min[splitIndex] + bestSplit / 2;

        // Compute a bitmap using the given split and sum the color values for
        // both buckets.
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 4; x++) {
                bits = bits << 1;
                if (get_channel(get_pixel(x0 + x, y0 + y),
                                splitIndex) > splitValue) {
                    bits |= 1;
                }
            }
        }
    }

    // Find the best bitmap match by counting the bits that don't match,
    // including the inverted bitmaps.
    int best_diff = 8;
    unsigned int best_pattern = 0x0000ffff;
    int codepoint = 0x2584;
    bool inverted = false;
    for (int i = 0; BITMAPS[i + 1] != END_MARKER; i += 3) {
        if ((BITMAPS[i + 2] & flags) != BITMAPS[i + 2]) {
            continue;
        }
        unsigned int pattern = BITMAPS[i];
        for (int j = 0; j < 2; j++) {
            int diff = (std::bitset<32>(pattern ^ bits)).count();
            if (diff < best_diff) {
                best_pattern = BITMAPS[i];  // pattern might be inverted.
                codepoint = BITMAPS[i + 1];
                best_diff = diff;
                inverted = best_pattern != pattern;
            }
            pattern = ~pattern;
        }
    }

    if (direct) {
        CharData result;
        if (inverted) {
            long tmp = max_count_color_1;
            max_count_color_1 = max_count_color_2;
            max_count_color_2 = tmp;
        }
        for (int i = 0; i < 3; i++) {
            int shift = 16 - 8 * i;
            result.fgColor[i] = (max_count_color_2 >> shift) & 255;
            result.bgColor[i] = (max_count_color_1 >> shift) & 255;
            result.codePoint = codepoint;
        }
        return result;
    }
    return createCharData(get_pixel, x0, y0, codepoint, best_pattern);
}

int clamp_byte(int value) {
    return value < 0 ? 0 : (value > 255 ? 255 : value);
}

int best_index(int value, const int STEPS[], int count) {
    int best_diff = std::abs(STEPS[0] - value);
    int result = 0;
    for (int i = 1; i < count; i++) {
        int diff = std::abs(STEPS[i] - value);
        if (diff < best_diff) {
            result = i;
            best_diff = diff;
        }
    }
    return result;
}
