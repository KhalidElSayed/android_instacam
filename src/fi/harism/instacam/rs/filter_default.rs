/*
   Copyright 2012 Harri Smatt

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

#pragma version(1)
#pragma rs java_package_name(fi.harism.instacam)

#include "utils.rsh"

static float brightness_value;
static float contrast_value;
static float saturation_value;

static float corner_radius;
static float inv_corner_radius;

static float inv_width;
static float inv_height;

static float2 tex_pos;
static float sqrt2 = 1.41421356f;

void setBrightness(float value) {
	brightness_value = value;
}

void setContrast(float value) {
	contrast_value = value;
}

void setSaturation(float value) {
	saturation_value = value;
}

void setCornerRadius(float value) {
	corner_radius = value;
	inv_corner_radius = 1.0f / value;
}

void setSize(float width, float height) {
	inv_width = 1.0f / width;
	inv_height = 1.0f / height;
}

void root(uchar4* v_color, uint32_t x, uint32_t y) {
	tex_pos.x = x * inv_width;
	tex_pos.y = y * inv_height;
	
	float3 color = rsUnpackColor8888(*v_color).rgb;
	
	// Adjust color brightness, contrast and saturation.
	color = brightness(color, brightness_value);
	color = contrast(color, contrast_value);
	color = saturation(color, saturation_value);
	
	// Calculate darker rounded corners.
	float len = distance(tex_pos, 0.5f) * sqrt2;
	len = (len - 1.0f + corner_radius) * inv_corner_radius;
	len = clamp(len, 0.0f, 1.0f);
	len = len * len * (3.0f - 2.0f * len);
	color *= mix(0.5f, 1.0f, 1.0f - len);
	
	// Finally store color value back to allocation.
	color = clamp(color, 0.0f, 1.0f);
	*v_color = rsPackColorTo8888(color);
}
