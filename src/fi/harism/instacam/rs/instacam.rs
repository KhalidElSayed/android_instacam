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


static void brightnessFunc(float3 color, float brightness) {
	float scaled = brightness / 2.0;
	if (scaled < 0.0) {
		color = color * (1.0f + scaled);
	} else {
		color = color + ((1.0f - color) * scaled);
	}
}

static void contrastFunc(float3 color, float contrast) {
	const float PI = 3.14159265;
	color = min(1.0f, ((color - 0.5f) * (tan((contrast + 1.0f) * PI / 4.0f) ) + 0.5f));
}

static float3 overlayFunc(float3 overlayComponent, float3 underlayComponent, float alpha) {
	float3 underlay = underlayComponent * alpha;
	return underlay * (underlay + (2.0f * overlayComponent * (1.0f - underlay)));
}

static float3 multiplyWithAlphaFunc(float3 overlayComponent, float alpha, float3 underlayComponent) {
	return underlayComponent * overlayComponent * alpha;
}

static float3 screenPixelComponentFunc(float3 maskPixelComponent, float alpha, float3 imagePixelComponent) {
	return 1.0f - (1.0f - (maskPixelComponent * alpha)) * (1.0f - imagePixelComponent);
}

static float hueToRGBFunc(float f1, float f2, float hue)
{
	if (hue < 0.0) hue += 1.0;
	else if (hue > 1.0)	hue -= 1.0;
	float res;
	if ((6.0 * hue) < 1.0) res = f1 + (f2 - f1) * 6.0 * hue;
	else if ((2.0 * hue) < 1.0) res = f2;
	else if ((3.0 * hue) < 2.0) res = f1 + (f2 - f1) * ((2.0 / 3.0) - hue) * 6.0;
	else res = f1;
	return res;
}

static float3 rgbToHslFunc(float3 color) {
	float3 hsl;
	
	float fmin = min(min(color.r, color.g), color.b);
	float fmax = max(max(color.r, color.g), color.b);
	float delta = fmax - fmin;

	hsl.z = (fmax + fmin) / 2.0;

	if (delta == 0.0) {
		hsl.x = 0.0;	// Hue
		hsl.y = 0.0;	// Saturation
	} else {
		if (hsl.z < 0.5) hsl.y = delta / (fmax + fmin);
		else hsl.y = delta / (2.0 - fmax - fmin); // Saturation
		
		float deltaR = (((fmax - color.r) / 6.0) + (delta / 2.0)) / delta;
		float deltaG = (((fmax - color.g) / 6.0) + (delta / 2.0)) / delta;
		float deltaB = (((fmax - color.b) / 6.0) + (delta / 2.0)) / delta;

		if (color.r == fmax) hsl.x = deltaB - deltaG;
		else if (color.g == fmax) hsl.x = (1.0 / 3.0) + deltaR - deltaB;
		else if (color.b == fmax) hsl.x = (2.0 / 3.0) + deltaG - deltaR;

		if (hsl.x < 0.0) hsl.x += 1.0;
		else if (hsl.x > 1.0) hsl.x -= 1.0;
	}
	return hsl;
}

static float3 hslToRgbFunc(float3 hsl) {
	float3 rgb;
	if (hsl.y == 0.0) rgb.rgb = hsl.z;
	else {
		float f2;
		
		if (hsl.z < 0.5) f2 = hsl.z * (1.0 + hsl.y);
		else f2 = (hsl.z + hsl.y) - (hsl.y * hsl.z);
		
		float f1 = 2.0 * hsl.z - f2;
		
		rgb.r = hueToRGBFunc(f1, f2, hsl.x + (1.0/3.0));
		rgb.g = hueToRGBFunc(f1, f2, hsl.x);
		rgb.b = hueToRGBFunc(f1, f2, hsl.x - (1.0/3.0));
	}
	return rgb;
}

static float3 saturateMatrixFunc(float3 color, float saturate) {
	float3 r = 0.3086;
	r.r += saturate;
	float3 g = 0.6094;
	g.g += saturate;
	float3 b = 0.0820;
	b.b += saturate;
	
	return r * color + g * color + b * color;
}

void filterImpl(rs_allocation allocation, int filter, float brightness,
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
			
			switch (filter) {
			// Black and White filter.
			case 1:
			{
				float3 tmp1 = { 0.299f, 0.587f, 0.114f };
				float gray = dot(color, tmp1);
				color = gray;
				break;
			}
			// Ansel filter.
			case 2:
			{
				float3 tmp1 = { 0.299f, 0.587f, 0.114f };
				float gray = dot(color, tmp1);
				if (gray > 0.5f) {
					color = 1.0f - (1.0f - 2.0f * (gray - 0.5f)) * (1.0f - gray);
				} else {
					color = 2.0f * gray * gray;
				}
				break;
			}
			// Sepia filter.
			case 3:
			{
				float3 tmp1 = { 0.21f, 0.72f, 0.07f };
				float luminosity = dot(color, tmp1);
				tmp1 = luminosity;
				brightnessFunc(tmp1, 0.234375f);
				float brightGray = tmp1.r;
				
				float3 tmp2 = { 0.419f, 0.259f, 0.047f };
				float3 tmp3 = brightGray;
				float3 tinted = overlayFunc(tmp2, tmp3, 1.0f);
				
				float invertMask = 1.0f - luminosity;
				float luminosity3 = pow(luminosity, 3.0f);
		
				float3 tmp4 = tinted * invertMask;
				float3 tmp5 = luminosity3;
				color = tmp5 + (tmp4 * luminosity) + tmp4;
				break;
			}
			// Retro filter.
			case 4:
			{
				float3 tmp1 = { 0.299f, 0.587f, 0.114f };
				float gray = dot(color, tmp1);
				color = overlayFunc(gray, color, 1.0f);
				float3 tmp2 = { 0.984f, 0.949f, 0.639f };
				color = multiplyWithAlphaFunc(tmp2, 0.588235f, color);
				float3 tmp3 = { 0.909f, 0.396f, 0.702f };
				color = screenPixelComponentFunc(tmp3, 0.2, color);
				float3 tmp4 = { 0.035f, 0.286f, 0.914f };
				color = screenPixelComponentFunc(tmp4, 0.168627f, color);
				break;
			}
			// Georgia filter.
			case 5:
			{
				brightnessFunc(color, 0.4724f);
				contrastFunc(color, 0.3149f);
		
				color.g = color.g * 0.87f + 0.13f;
				color.b = color.b * 0.439f + 0.561f;
		
				float3 tmp1 = { 0.981, 0.862, 0.686 };
				color *= tmp1;
				break;
			}
			// Sahara filter.
			case 6:
			{
				color.r = color.r * 0.843 + 0.157;
				color.b = color.b * 0.882 + 0.118;
		
				float3 hsl = rgbToHslFunc(color);
				hsl.y = hsl.y * 0.55f;
				color = hslToRgbFunc(hsl);
		
				float3 tmp1 = { 1.0f, 0.891f, 0.733f };
				color = saturateMatrixFunc(color, 0.65f);
				color *= tmp1;
			}
			default:
			{
				break;
			}
			}
			
			
			
			// Adjust color brightness, contrast and saturation.
			brightnessFunc(color, contrast);
			contrastFunc(color, contrast);
			float average = dot(color, 1.0f) / 3.0f;
			if (saturation > 0.0f) {
				color += (average - color) * saturationPos;
			} else {
				color += (average - color) * saturationNeg;
			}
			
			// Calculate darker rounded corners.
			float len = distance(texPos, 0.5f) * sqrt2;
			len = clamp((len - 1.0f + cornerRadius) * invCornerRadius, 0.0f, 1.0f);
			len = len * len * (3.0f - 2.0f * len);
			color *= mix(0.15f, 1.0f, 1.0f - len);
			
			// Finally store color value back to allocation.
			color = clamp(color, 0.0f, 1.0f);
			*colorPtr = rsPackColorTo8888(color);			
		}
	}
}
