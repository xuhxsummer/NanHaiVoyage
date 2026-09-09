package com.shipgame.nanhai.data;

/** Stable Catalog-indexed geometry shared by the renderer and navigation.
 * Land radii enclose shore, rocks, buildings and piers; ship radii enclose the
 * whole deck at every heading, including after turning or changing ships.
 */
public final class VoyageGeometry {
    private VoyageGeometry() {}
    public static final float PORT_SCALE = 4.5f, ISLAND_SCALE = 4f;
    public static final int PIRATE_SHIP = 4;
    public static float pirateSeparation(int playerShip) {
        return ship(playerShip).radius()+ship(PIRATE_SHIP).radius()+6f;
    }
    public static final class Ship {
        public final float length, beam, height;
        public final int sails, color;
        private Ship(float length, float beam, float height, int sails, int color) {
            this.length=length; this.beam=beam; this.height=height; this.sails=sails; this.color=color;
        }
        public float radius() { return (float)Math.hypot(29*length, 16*beam)+1; }
    }
    private static final Ship[] SHIPS = {
        new Ship(.78f,.82f,.78f,1,0xe8ca87), new Ship(1.02f,1.04f,1.12f,3,0xd59c50),
        new Ship(.91f,.76f,.83f,2,0x985344), new Ship(1.04f,1.08f,.85f,2,0xbca97b),
        new Ship(1.03f,.96f,1.0f,3,0x8b3434), new Ship(.84f,.65f,.94f,1,0xd9ddd3),
        new Ship(1.06f,1.10f,1.07f,3,0xb9905b), new Ship(.88f,.83f,1.03f,2,0x4e9fa5),
        new Ship(1.08f,.89f,1.16f,3,0x314d72)
    };
    public static int shipIndex(int id) { return id>=0 && id<SHIPS.length ? id:0; }
    public static Ship ship(int id) { return SHIPS[shipIndex(id)]; }
    public static float landRadius(boolean port, int id) {
        return ((port ? 34f:25f)+(id%4)) * (port ? PORT_SCALE:ISLAND_SCALE);
    }
    public static float shoreRadius(boolean port, int id, int vertex) {
        // An irregular, star-shaped shore, with its outermost tip exactly on the envelope.
        return landRadius(port,id)*(vertex==0 ? 1f : .80f+.18f*(float)(.5+.5*Math.sin(id*2.31+vertex*1.73)));
    }
}
