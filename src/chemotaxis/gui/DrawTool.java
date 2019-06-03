package chemotaxis.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import api.API;
import chemotaxis.logic.ChemotaxisParam;
import chemotaxis.pojo.*;
import main.api.GUI;

public class DrawTool {
	public static void drawValueSet(Graphics2D g, ValueSet set) {
		Point2D.Double guiPoint;
		Ellipse2D.Double ellipse;
		Iterator<Value> itr;
		Value val;
		
		itr = set.iterator();
		GUI gui = API.getGUI(0);
		while(itr.hasNext()) {
			val = itr.next();
			g.setColor(new Color((int) ((val.getV() - set.getMin()) / (set.getMax() - set.getMin()) * 255), 0, 0));
			//GUI.log(Double.toString(val.getV()));
			guiPoint = gui.locatePoint(ChemotaxisParam.origin.x + (val.getX() * ChemotaxisParam.density), ChemotaxisParam.origin.y + (val.getY() * ChemotaxisParam.density));
			ellipse = new Ellipse2D.Double(guiPoint.getX() - 5, guiPoint.getY() - 5, 10, 10);
			g.draw(ellipse);
			g.drawString(String.format("%.2f", val.getV()), (float) guiPoint.getX() + 10, (float) guiPoint.getY());
		}
	}
	
	public static void drawBounds(Graphics2D g) {
		Point2D.Double guiPoint, guiPoint2;
		Rectangle2D.Double rectangle;
		g.setColor(Color.BLACK);
		GUI gui = API.getGUI(0);
		guiPoint = gui.locatePoint(ChemotaxisParam.origin.x, ChemotaxisParam.origin.y);
		guiPoint2 = gui.locatePoint(ChemotaxisParam.origin.x + (ChemotaxisParam.width * ChemotaxisParam.density), ChemotaxisParam.origin.y + (ChemotaxisParam.density * ChemotaxisParam.density));
		rectangle = new Rectangle2D.Double(guiPoint.getX(), guiPoint.getY(), guiPoint2.getX() - guiPoint.getX(), guiPoint2.getY() - guiPoint.getY());
		gui.log(Double.toString(rectangle.getWidth()));
		g.draw(rectangle);
	}
}
