#pragma version(1)
#pragma rs java_package_name(fi.harism.instacam.rs)

#include <rs_cl.rsh>
#include <rs_core.rsh>
#include <rs_types.rsh>
#include <rs_allocation.rsh>

void filterImpl(rs_allocation allocation, float brightness, float contrast, float saturation) {
	uint32_t width = rsAllocationGetDimX(allocation);
	uint32_t height = rsAllocationGetDimY(allocation);
	
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
			
			color = clamp(color, 0.0f, 1.0f);
			*colorPtr = rsPackColorTo8888(color);
			
		}
	}
}
