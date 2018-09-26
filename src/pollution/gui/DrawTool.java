package pollution.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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
			g.setColor(new Color((int) ((val.getV() - set.getMin()) / (set.getMax() - set.getMin()) * 255), 0, 0));
			//GUI.log(Double.toString(val.getV()));
			guiPoint = GUI.locatePoint(PollutionParam.origin.Easting + (val.getX() * PollutionParam.density), PollutionParam.origin.Northing + (val.getY() * PollutionParam.density));
			ellipse = new Ellipse2D.Double(guiPoint.getX() - 5, guiPoint.getY() - 5, 10, 10);
			g.draw(ellipse);
			g.drawString(String.format("%.2f", val.getV()), (float) guiPoint.getX() + 10, (float) guiPoint.getY());
		}
	}
	
	public static void drawBounds(Graphics2D g) {
		Point2D.Double guiPoint, guiPoint2;
		Rectangle2D.Double rectangle;
		g.setColor(Color.BLACK);
		guiPoint = GUI.locatePoint(PollutionParam.origin.Easting, PollutionParam.origin.Northing);
		guiPoint2 = GUI.locatePoint(PollutionParam.origin.Easting + (PollutionParam.width * PollutionParam.density), PollutionParam.origin.Northing + (PollutionParam.density * PollutionParam.density));
		rectangle = new Rectangle2D.Double(guiPoint.getX(), guiPoint.getY(), guiPoint2.getX() - guiPoint.getX(), guiPoint2.getY() - guiPoint.getY());
		GUI.log(Double.toString(rectangle.getWidth()));
		g.draw(rectangle);
	}
}
