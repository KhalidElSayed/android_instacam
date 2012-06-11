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

static float3 brightness(float3 color, float brightness) {
	float scaled = brightness / 2.0;
	if (scaled < 0.0) {
		return color * (1.0f + scaled);
	} else {
		return color + ((1.0f - color) * scaled);
	}
}

static float3 contrast(float3 color, float contrast) {
	const float PI = 3.14159265;
	return min(1.0f, ((color - 0.5f) * (tan((contrast + 1.0f) * PI / 4.0f) ) + 0.5f));
}

static float3 overlay(float3 overlayComponent, float3 underlayComponent, float alpha) {
	float3 underlay = underlayComponent * alpha;
	return underlay * (underlay + (2.0f * overlayComponent * (1.0f - underlay)));
}

static float3 multiplyWithAlpha(float3 overlayComponent, float alpha, float3 underlayComponent) {
	return underlayComponent * overlayComponent * alpha;
}

static float3 screenPixelComponent(float3 maskPixelComponent, float alpha, float3 imagePixelComponent) {
	return 1.0f - (1.0f - (maskPixelComponent * alpha)) * (1.0f - imagePixelComponent);
}

static float hueToRGB(float f1, float f2, float hue)
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

static float3 rgbToHsl(float3 color) {
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

static float3 hslToRgb(float3 hsl) {
	float3 rgb;
	if (hsl.y == 0.0) rgb.rgb = hsl.z;
	else {
		float f2;
		
		if (hsl.z < 0.5) f2 = hsl.z * (1.0 + hsl.y);
		else f2 = (hsl.z + hsl.y) - (hsl.y * hsl.z);
		
		float f1 = 2.0 * hsl.z - f2;
		
		rgb.r = hueToRGB(f1, f2, hsl.x + (1.0/3.0));
		rgb.g = hueToRGB(f1, f2, hsl.x);
		rgb.b = hueToRGB(f1, f2, hsl.x - (1.0/3.0));
	}
	return rgb;
}

static float3 saturate(float3 color, float sat) {
	float3 r = 0.3086;
	r.r += sat;
	float3 g = 0.6094;
	g.g += sat;
	float3 b = 0.0820;
	b.b += sat;
	
	return r * color + g * color + b * color;
}
