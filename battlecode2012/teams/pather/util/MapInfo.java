package pather.util;

import battlecode.common.TerrainTile;

public class MapInfo{
    private int[] loc;
    private TerrainTile value;

    MapInfo(int[] loc, TerrainTile value) {
          this.loc = loc;
          this.value = value;
    }

    public TerrainTile getValue() {
          return value;
    }

    public void setValue(TerrainTile value) {
          this.value = value;
    }

    public int[] getKey() {
          return loc;
    }
}
