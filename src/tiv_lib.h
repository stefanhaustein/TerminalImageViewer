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

#ifndef TIV_LIB_H_
#define TIV_LIB_H_


#include <array>
#include <functional>

constexpr int FLAG_TELETEXT = 32;  // Bitset flag to use teletext characters.
// 32 for backwards-compatibility reasons
// as this was introduced after other bitset flags for the tiv CLI


// Color saturation value steps from 0 to 255
constexpr int COLOR_STEP_COUNT = 6;
constexpr int COLOR_STEPS[COLOR_STEP_COUNT] = {0, 0x5f, 0x87, 0xaf, 0xd7, 0xff};

// Grayscale saturation value steps from 0 to 255
constexpr int GRAYSCALE_STEP_COUNT = 24;
constexpr int GRAYSCALE_STEPS[GRAYSCALE_STEP_COUNT] = {
    0x08, 0x12, 0x1c, 0x26, 0x30, 0x3a, 0x44, 0x4e, 0x58, 0x62, 0x6c, 0x76,
    0x80, 0x8a, 0x94, 0x9e, 0xa8, 0xb2, 0xbc, 0xc6, 0xd0, 0xda, 0xe4, 0xee};

/**
* @brief Function that returns a pixel from the image given its coordinates
* @param x x-coordinate of the pixel
* @param y y-coordinate of the pixel
* @return The pixel value in 0xRRGGBB format
*/
typedef std::function<unsigned long(int, int)> GetPixelFunction;

/**
* @brief Get the value of a specific color channel for the specified pixel
* @param rgb A pixel in 0xRRGGBB format
* @param index 0, 1, or 2 for R, G, B
* @return The pixel value between 0 and 255
*/
unsigned char get_channel(unsigned long rgb, int index);

int clamp_byte(int value);

/**
* @brief Map color value to the closest possible color step
* @param value Color value from 0 to 255
* @param STEPS An array like COLOR_STEPS
* @param count The distance between each color step (e.g. COLOR_STEP_COUNT)
*/
int best_index(int value, const int STEPS[], int count);

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

#endif  // TIV_LIB_H_
