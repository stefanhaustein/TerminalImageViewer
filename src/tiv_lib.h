
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

#ifndef TIV_LIB_H
#define TIV_LIB_H


#include <array>
#include <functional>

// Implementation of flag representation for flags in the main() method
constexpr int FLAG_FG = 1;
constexpr int FLAG_BG = 2;
constexpr int FLAG_MODE_256 = 4;   // Limit colors to 256-color mode
constexpr int FLAG_24BIT = 8;      // 24-bit color mode
constexpr int FLAG_NOOPT = 16;     // Only use the same half-block character
constexpr int FLAG_TELETEXT = 32;  // Use teletext characters


// Color saturation value steps from 0 to 255
constexpr int COLOR_STEP_COUNT = 6;
constexpr int COLOR_STEPS[COLOR_STEP_COUNT] = {0, 0x5f, 0x87, 0xaf, 0xd7, 0xff};

// Grayscale saturation value steps from 0 to 255
constexpr int GRAYSCALE_STEP_COUNT = 24;
constexpr int GRAYSCALE_STEPS[GRAYSCALE_STEP_COUNT] = {
    0x08, 0x12, 0x1c, 0x26, 0x30, 0x3a, 0x44, 0x4e, 0x58, 0x62, 0x6c, 0x76,
    0x80, 0x8a, 0x94, 0x9e, 0xa8, 0xb2, 0xbc, 0xc6, 0xd0, 0xda, 0xe4, 0xee};


typedef std::function<unsigned long(int, int)> GetPixelFunction;

int clamp_byte(int value);

int best_index(int value, const int STEPS[], int count);

double sqr(double n);

/**
 * @brief Struct to represent a character to be drawn.
 * @param fgColor RGB
 * @param bgColor RGB
 * @param codePoint The code point of the character to be drawn.
 */
struct CharData {
    std::array<int, 3> fgColor = std::array<int, 3>{0, 0, 0};
    std::array<int, 3> bgColor = std::array<int, 3>{0, 0, 0};
    int codePoint;
};

// Return a CharData struct with the given code point and corresponding averag
// fg and bg colors.
CharData createCharData(GetPixelFunction get_pixel, int x0, int y0,
                        int codepoint, int pattern);

/**
 * @brief Find the best character and colors
 * for a 4x8 part of the image at the given position
 *
 * @param image
 * @param x0
 * @param y0
 * @param flags
 * @return CharData
 */
CharData findCharData(GetPixelFunction get_pixel, int x0, int y0,
                      const int &flags);

#endif 