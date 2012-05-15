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
#pragma rs java_package_name(fi.harism.instacam.rs)

#include <rs_cl.rsh>
#include <rs_core.rsh>
#include <rs_types.rsh>
#include <rs_allocation.rsh>

void filterImpl(rs_allocation allocation, float brightness,
                float contrast, float saturation, float cornerRadius) {
	
	// Get image dimensions from allocation.
	uint32_t width = rsAllocationGetDimX(allocation);
	uint32_t height = rsAllocationGetDimY(allocation);
	
	// Calculate some inverse values for making
	// it possible to use multiplication instead.
	float invWidth = 1.0f / width;
	float invHeight = 1.0f / height;
	float invCornerRadius = 1.0f / cornerRadius;
	float sqrt2 = sqrt(2.0f);
	
	// Calculate contrast and saturation runtime values.
	float contrastPos = 1.0f / (1.0f - contrast);
	float contrastNeg = 1.0f + contrast;
	float saturationPos = 1.0f - (1.0f / (1.0f - saturation));
	float saturationNeg = -saturation;
	
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
			
			// Adjust color brightness.
			color += brightness;
			
			// Adjust color contrast.
			if (contrast > 0.0) {
				color = (color - 0.5f) * contrastPos + 0.5f;
			} else {
				color = (color - 0.5f) * contrastNeg + 0.5f;
			}
			
			// Adjust color saturation.
			float average = dot(color, 1.0f) / 3.0f;
			if (saturation > 0.0f) {
				color += (average - color) * saturationPos;
			} else {
				color += (average - color) * saturationNeg;
			}
			
			// Calculate darker corners.
			float len = length(texPos - 0.5f) * sqrt2;
			len = clamp((len - 1.0f + cornerRadius) * invCornerRadius, 0.0f, 1.0f);
			len = len * len * (3.0f - 2.0f * len);
			color *= mix(0.15f, 1.0f, 1.0f - len);
			
			// Finally store color value back to allocation.
			color = clamp(color, 0.0f, 1.0f);
			*colorPtr = rsPackColorTo8888(color);			
		}
	}
}
