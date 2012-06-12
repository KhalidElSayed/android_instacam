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

static float3 COLOR1 = { 1.0f, 0.891f, 0.733f };

void root(uchar4* v_color) {
	float3 color = rsUnpackColor8888(*v_color).rgb;
	
	color.r = color.r * 0.843 + 0.157;
	color.b = color.b * 0.882 + 0.118;
	
	float3 hsv = rgbToHsv(color);
	hsv.y = hsv.y * 0.55f;
	color = hsvToRgb(hsv);

	color = saturation(color, 0.65f);
	color *= COLOR1;
	
	// Finally store color value back to allocation.
	color = clamp(color, 0.0f, 1.0f);
	*v_color = rsPackColorTo8888(color);
}
