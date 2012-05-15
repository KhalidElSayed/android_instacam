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
	float invSqrt2 = 2.0f / sqrt(2.0f);
	
	for (uint32_t x = 0; x < width; ++x) {
		for (uint32_t y = 0; y < height; ++y) {
			
			uchar4* colorPtr = (uchar4*)rsGetElementAt(allocation, x, y);
			float3 color = rsUnpackColor8888(*colorPtr).rgb;
			
			color += brightness;
			if (contrast > 0.0) {
				color = (color - 0.5f) / (1.0f - contrast) + 0.5f;
			} else {
				color = (color - 0.5f) * (1.0f + contrast) + 0.5f;
			}
			float average = dot(color, 1.0f) / 3.0f;
			if (saturation > 0.0f) {
				color += (average - color) * (1.0f - 1.0f / (1.0f - saturation));
			} else {
				color += (average - color) * (-saturation);
			}
			
			float2 pos;
			pos.x = (float) x / width;
			pos.y = (float) y / height;			
			float len = length(pos - 0.5f) * invSqrt2;
			len = mix(1.0f + cornerRadius, 1.0f - cornerRadius, len);
			color *= clamp(len, 0.0f, 1.0f);
			
			color = clamp(color, 0.0f, 1.0f);
			*colorPtr = rsPackColorTo8888(color);			
		}
	}
}
