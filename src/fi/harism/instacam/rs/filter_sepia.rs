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

static float3 COLOR1 = { 0.21f, 0.72f, 0.07f };
static float3 COLOR2 = { 0.419f, 0.259f, 0.047f };

void root(uchar4* v_color) {
	float3 color = rsUnpackColor8888(*v_color).rgb;
	
	float luminosity = dot(color, COLOR1);
	float brightGray = brightness(luminosity, 0.234375f).r;
	
	float3 tinted = overlay(COLOR2, brightGray, 1.0f);
	
	float invertMask = 1.0f - luminosity;
	color = pow(luminosity, 3.0f) + (tinted * invertMask * (luminosity + 1.0f));
	
	// Finally store color value back to allocation.
	color = clamp(color, 0.0f, 1.0f);
	*v_color = rsPackColorTo8888(color);
}
