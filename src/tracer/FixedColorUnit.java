/*
 * Copyright 2019 RCherry93.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tracer;

/**
 *
 * @author RCherry93
 */
public class FixedColorUnit {

    // Variable Declaration
    // Data Types - 
    // I use "public final" because these values do not change
    // and should not change.
    public final int argb;
    public final int alpha;
    public final int blue;
    public final int green;
    public final int red;
    public final int x;
    public final int y;
    // End of Variable Delcaration

    // Simply pairs an argb int and a point together in an object.
    public FixedColorUnit(int argb, int x, int y) {
        
        //
        this.argb = argb;
        
        // Set the position of the color
        this.x = x;
        this.y = y;

        // Parse out the ARGB values in case I want to turn this into a color chooser.
        // 0xFF removes all the bits to the right of it.
        alpha = ((argb >> 24) & 0xFF);
        red = ((argb >> 16) & 0xFF);
        green = ((argb >> 8) & 0xFF);
        blue = ((argb) & 0xFF);
    }
}
