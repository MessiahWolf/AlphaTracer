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

import java.awt.Point;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author RCherry93
 */
public class FormContainer {

    // Variable Declaration
    // Java Native Classes
    private final HashMap<Point, Boolean> pointMap;
    private final ArrayList<Polygon> polyList;
    private Polygon polygon;
    // Project Classes
    // Data Types
    // End of Variable Declaration

    public FormContainer() {

        // Create a map to keep track of vertical and horizontal lines.
        // 0 / false = horizontal line
        // 1 / true = vertical line
        pointMap = new HashMap();
        polyList = new ArrayList();
        polygon = new Polygon();
    }

    public void addPolygonPoint(int x, int y) {
        polygon.addPoint(x, y);
    }

    public void addCutPoint(Point p, Boolean lineType) {

        // Add to the collection
        pointMap.put(p, lineType);
    }

    public void addCutMap(HashMap<Point, Boolean> map) {
        pointMap.putAll(map);
    }

    public void resetMap() {
        pointMap.clear();
    }
    
    public void resetPolygon() {
        polygon.reset();
        polyList.clear();
    }
    
    public void close() {
        polyList.add(polygon);
        polygon = new Polygon();
    }

    public HashMap<Point, Boolean> getCutMap() {
        return pointMap;
    }

    public Polygon getActivePolygon() {
        return polygon;
    }
    
    public ArrayList<Polygon> getPolygonList() {
        return polyList;
    }
}
