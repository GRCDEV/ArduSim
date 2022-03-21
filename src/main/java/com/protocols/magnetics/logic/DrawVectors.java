package com.protocols.magnetics.logic;

import com.api.ArduSim;
import com.api.copter.Copter;
import com.protocols.magnetics.pojo.Vector;
import com.setup.Param;
import es.upv.grc.mapper.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

class DrawVectors {

    private final List<AtomicReference<DrawableLinesGeo>> vectorLines;
    private final Copter copter;

    public DrawVectors(Copter copter){
        this.copter = copter;
        vectorLines = new ArrayList<>();
        for(int i = 0;i<3;i++){
            vectorLines.add(new AtomicReference<>());
        }
    }

    public void update(Vector attraction, Vector repulsion, Vector resulting) {
        if(Param.role == ArduSim.SIMULATOR_GUI) {
            try {
                removeOldDrawings();
                Location2DGeo start = copter.getLocationUTM().getGeo();
                drawAttractionVector(start, attraction);
                drawRepulsionVector(start, repulsion);
                drawResultingVector(start, resulting);

            } catch (LocationNotReadyException | GUIMapPanelNotReadyException e) {
                e.printStackTrace();
            }
        }
    }

    private void removeOldDrawings() {
        for(AtomicReference<DrawableLinesGeo> l: vectorLines) {
            DrawableLinesGeo current = l.getAndSet(null);
            if (current != null) {
                try {
                    Mapper.Drawables.removeDrawable(current);
                } catch (GUIMapPanelNotReadyException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void drawAttractionVector(Location2DGeo start, Vector attraction) throws LocationNotReadyException, GUIMapPanelNotReadyException {
        Location2DUTM stop = copter.getLocationUTM();
        stop.x += attraction.x;
        stop.y += attraction.y;
        List<Location2DGeo> attractionVector = new ArrayList<>();
        attractionVector.add(start);
        attractionVector.add(stop.getGeo());
        vectorLines.get(0).set(Mapper.Drawables.addLinesGeo(3, attractionVector, Color.BLUE, new BasicStroke(2f)));
    }

    private void drawRepulsionVector(Location2DGeo start, Vector repulsion) throws LocationNotReadyException, GUIMapPanelNotReadyException {
        Location2DUTM stop = copter.getLocationUTM();
        stop.x += repulsion.x;
        stop.y += repulsion.y;
        List<Location2DGeo> attractionVector = new ArrayList<>();
        attractionVector.add(start);
        attractionVector.add(stop.getGeo());
        vectorLines.get(1).set(Mapper.Drawables.addLinesGeo(3, attractionVector, Color.RED, new BasicStroke(2f)));
    }

    private void drawResultingVector(Location2DGeo start, Vector resulting) throws LocationNotReadyException, GUIMapPanelNotReadyException {
        Location2DUTM stop = copter.getLocationUTM();
        stop.x += resulting.x;
        stop.y += resulting.y;
        List<Location2DGeo> attractionVector = new ArrayList<>();
        attractionVector.add(start);
        attractionVector.add(stop.getGeo());
        vectorLines.get(1).set(Mapper.Drawables.addLinesGeo(3, attractionVector, Color.GREEN, new BasicStroke(2f)));
    }




}
