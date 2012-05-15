#pragma version(1)
#pragma rs java_package_name(fi.harism.instacam.rs)

#include <rs_cl.rsh>
#include <rs_core.rsh>
#include <rs_types.rsh>
#include <rs_allocation.rsh>

void filterImpl(rs_allocation allocation, float brightness,
                float contrast, float saturation, float cornerRadius) {
	uint32_t width = rsAllocationGetDimX(allocation);
	uint32_t height = rsAllocationGetDimY(allocation);
	
	float invWidth = 1.0f / width;
	float invHeight = 1.0f / height;
	float invCornerRadius = 1.0f / cornerRadius;
	float invSqrt2 = 2.0f / sqrt(2.0f);
	
	float contrastPos = 1.0f / (1.0f - contrast);
	float contrastNeg = 1.0f + contrast;
	float saturationPos = 1.0f - (1.0f / (1.0f - saturation));
	float saturationNeg = -saturation;
	
	float2 texPos;
	
	for (uint32_t xx = 0; xx < width; ++xx) {
		
		texPos.x = xx * invWidth;
	
		for (uint32_t yy = 0; yy < height; ++yy) {
			
			texPos.y = yy * invHeight;
			
			uchar4* colorPtr = (uchar4*)rsGetElementAt(allocation, xx, yy);
			float3 color = rsUnpackColor8888(*colorPtr).rgb;
			
			color += brightness;
			if (contrast > 0.0) {
				color = (color - 0.5f) * contrastPos + 0.5f;
			} else {
				color = (color - 0.5f) * contrastNeg + 0.5f;
			}
			float average = dot(color, 1.0f) / 3.0f;
			if (saturation > 0.0f) {
				color += (average - color) * saturationPos;
			} else {
				color += (average - color) * saturationNeg;
			}
			
			float len = length(texPos - 0.5f) * invSqrt2;
			len = clamp((len - 1.0f + cornerRadius) * invCornerRadius, 0.0f, 1.0f);
			len = len * len * (3.0f - 2.0f * len);
			color *= mix(0.3f, 1.0f, 1.0f - len);
			
			color = clamp(color, 0.0f, 1.0f);
			*colorPtr = rsPackColorTo8888(color);			
		}
	}
}
