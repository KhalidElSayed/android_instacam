#pragma version(1)
#pragma rs java_package_name(fi.harism.instacam.rs)

#include <rs_core.rsh>
#include <rs_types.rsh>
#include <rs_allocation.rsh>

//multipliers to convert a RGB colors to black and white
const static float3 gMonoMult = {0.299f, 0.587f, 0.114f};

void monoImpl(rs_allocation allocation) {
	uint32_t width = rsAllocationGetDimX(allocation);
	uint32_t height = rsAllocationGetDimY(allocation);
	
	for (uint32_t x = 0; x < width; ++x) {
		for (uint32_t y = 0; y < height; ++y) {
			uchar4* colorPtr = (uchar4*)rsGetElementAt(allocation, x, y);
			float3 color = rsUnpackColor8888(*colorPtr).rgb;
			color = dot(color, gMonoMult);
			*colorPtr = rsPackColorTo8888(color);
		}
	}
}
