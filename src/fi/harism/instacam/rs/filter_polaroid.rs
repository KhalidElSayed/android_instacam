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

#include <rs_matrix.rsh>

static rs_matrix4x4 mat = { 1.438, -0.062, -0.062, 0.0,
		                    -0.122, 1.378, -0.122, 0.0,
		                    -0.016, -0.016, 1.483, 0.0,
		                    -0.03, 0.05, -0.02, 0.0 };

void root(uchar4* v_color) {
	float3 color = rsUnpackColor8888(*v_color).rgb;
	
	rsMatrixMultiply(&mat, color);

	// Finally store color value back to allocation.
	color = clamp(color, 0.0f, 1.0f);
	*v_color = rsPackColorTo8888(color);
}
