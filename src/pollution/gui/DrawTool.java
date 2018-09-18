package pollution.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.Iterator;

import api.GUI;
import pollution.PollutionParam;
import pollution.pojo.*;

public class DrawTool {
	public static void drawValueSet(Graphics2D g, ValueSet set) {
		Point2D.Double guiPoint;
		Ellipse2D.Double ellipse;
		Iterator<Value> itr;
		Value val;
		
		itr = set.iterator();
		while(itr.hasNext()) {
			val = itr.next();
			g.setColor(new Color(Math.min((int) val.getV(), 255), 0, 0));
			guiPoint = GUI.locatePoint(PollutionParam.origin.Easting + (val.getX() * PollutionParam.density), PollutionParam.origin.Northing + (val.getY() * PollutionParam.density));
			ellipse = new Ellipse2D.Double(guiPoint.getX() - 5, guiPoint.getY() - 5, 10, 10);
			g.draw(ellipse);
		}
	}
}
