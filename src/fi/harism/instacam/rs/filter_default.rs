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
	
void setBrightness(float value) {
	brightness_value = value;
}

void setContrast(float value) {
	contrast_value = value;
}

void setSaturation(float value) {
	if (value > 0.0f) {
		saturation_value = 1.0f - (1.0f / (1.0f - value));
	} else {
		saturation_value = -value;
	}
}

void root(uchar4* v_color) {
	float3 color = rsUnpackColor8888(*v_color).rgb;
	
	// Adjust color brightness, contrast and saturation.
	color = brightness(color, brightness_value);
	color = contrast(color, contrast_value);
	float average = dot(color, 1.0f) / 3.0f;
	color += (average - color) * saturation_value;

	// Finally store color value back to allocation.
	color = clamp(color, 0.0f, 1.0f);
	*v_color = rsPackColorTo8888(color);
}

void corners(rs_allocation allocation, float cornerRadius) {

	// Get image dimensions from allocation.
	uint32_t width = rsAllocationGetDimX(allocation);
	uint32_t height = rsAllocationGetDimY(allocation);
	
	// Calculate some inverse values for making
	// it possible to use multiplication instead.
	float invWidth = 1.0f / width;
	float invHeight = 1.0f / height;
	float invCornerRadius = 1.0f / cornerRadius;
	float sqrt2 = sqrt(2.0f);
	
	// Texture position within [0.0f, 1.0f] range.
	float2 texPos;
	
	// Outer loop horizontally.
	for (uint32_t xx = 0; xx < width; ++xx) {
		
		// Calculate texture position from xx.
		texPos.x = xx * invWidth;
	
		// Inner loop vertically.
		for (uint32_t yy = 0; yy < height; ++yy) {
			
			// Calculate texture position from yy.
			texPos.y = yy * invHeight;
			
			// Get color value for current position from allocation.
			uchar4* colorPtr = (uchar4*)rsGetElementAt(allocation, xx, yy);
			float3 color = rsUnpackColor8888(*colorPtr).rgb;

			// Calculate darker rounded corners.
			float len = distance(texPos, 0.5f) * sqrt2;
			len = clamp((len - 1.0f + cornerRadius) * invCornerRadius, 0.0f, 1.0f);
			len = len * len * (3.0f - 2.0f * len);
			color *= mix(0.5f, 1.0f, 1.0f - len);
			
			// Finally store color value back to allocation.
			color = clamp(color, 0.0f, 1.0f);
			*colorPtr = rsPackColorTo8888(color);			
		}
	}
}
