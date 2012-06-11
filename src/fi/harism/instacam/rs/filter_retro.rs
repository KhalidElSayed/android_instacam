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

static float3 COLOR1 = { 0.299f, 0.587f, 0.114f };
static float3 COLOR2 = { 0.984f, 0.949f, 0.639f };
static float3 COLOR3 = { 0.909f, 0.396f, 0.702f };
static float3 COLOR4 = { 0.035f, 0.286f, 0.914f };

void root(uchar4* v_color) {
	float3 color = rsUnpackColor8888(*v_color).rgb;
	
	float gray = dot(color, COLOR1);
	color = overlay(gray, color, 1.0f);
	color = multiplyWithAlpha(COLOR2, 0.588235f, color);
	color = screenPixelComponent(COLOR3, 0.2f, color);
	color = screenPixelComponent(COLOR4, 0.168627f, color);
	
	// Finally store color value back to allocation.
	color = clamp(color, 0.0f, 1.0f);
	*v_color = rsPackColorTo8888(color);
}
