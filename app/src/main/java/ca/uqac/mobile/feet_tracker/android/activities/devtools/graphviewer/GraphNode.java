package ca.uqac.mobile.feet_tracker.android.activities.devtools.graphviewer;

import java.util.Map;

/**
 * Created by MeTaL125 on 2017-11-29.
 */

public class GraphNode {

    private double east;
    private double north;
    private Map<String, GraphRoad> roads;

    public GraphNode() {
    }

    public double getEast() {
        return east;
    }

    public void setEast(double east) {
        this.east = east;
    }

    public double getNorth() {
        return north;
    }

    public void setNorth(double north) {
        this.north = north;
    }

    public Map<String, GraphRoad> getRoads() {
        return roads;
    }

    public void setRoads(Map<String, GraphRoad> roads) {
        this.roads = roads;
    }
}
