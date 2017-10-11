package sim.board;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;

import api.GUIHelper;
import api.MissionHelper;
import api.SwarmHelper;
import api.pojo.GeoCoordinates;
import api.pojo.LogPoint;
import api.pojo.Point3D;
import api.pojo.WaypointSimplified;
import api.pojo.UTMCoordinates;
import main.Param;
import main.Param.SimulatorState;
import main.Text;
import sim.board.pojo.MercatorProjection;
import sim.gui.MainWindow;
import sim.logic.InitialConfigurationThread;
import sim.logic.SimParam;
import sim.logic.SimTools;
import uavController.UAVParam;

/** This class contains methods used to show information on the board of the main window. */

public class BoardHelper {
	
	/** Builds the wind image (arrow) shown in the drawing panel. */
	public static void buildWindImage(BoardPanel p) {
		BufferedImage arrowImageRotated = GraphicsEnvironment.getLocalGraphicsEnvironment().
				getDefaultScreenDevice().getDefaultConfiguration().
				createCompatibleImage(p.getWidth(), SimParam.arrowImage.getHeight(), Transparency.TRANSLUCENT);
		Graphics2D g2 = arrowImageRotated.createGraphics();
		AffineTransform trans = new AffineTransform();
		trans.translate(p.getWidth() - SimParam.arrowImage.getWidth()*0.5,
				SimParam.arrowImage.getHeight()*0.5);
		trans.rotate(Param.windDirection*Math.PI/180.0 + Math.PI/2.0);
		trans.translate(-SimParam.arrowImage.getWidth()*0.5, -SimParam.arrowImage.getHeight()*0.5);
		g2.drawImage(SimParam.arrowImage, trans, p);
		g2.dispose();
		BoardParam.arrowImageRotated = arrowImageRotated;
	}

	/** Downloads the background images. */
	public static void downloadBackground() {
		//  Upper left UTM coordinates
		BoardParam.boardUpLeftUTMX = BoardParam.boardUTMx0;
		BoardParam.boardUpLeftUTMY = BoardParam.boardUTMy0 + SimParam.boardPXHeight/BoardParam.screenScale;
	
		// UTM screen dimensions
		double UTMWidth = SimParam.boardPXWidth/BoardParam.screenScale;
		double UTMHeight = SimParam.boardPXHeight/BoardParam.screenScale;
	
		// Mercator corners surrounding the panel
		double boardUpLeftUTMX = BoardParam.boardUpLeftUTMX;
		double boardUpLeftUTMY = BoardParam.boardUpLeftUTMY;
		double boardUpRightUTMX = BoardParam.boardUpLeftUTMX + UTMWidth;
		double boardUpRightUTMY = BoardParam.boardUpLeftUTMY;
		double boardDownLeftUTMX = BoardParam.boardUpLeftUTMX;
		double boardDownLeftUTMY = BoardParam.boardUpLeftUTMY - UTMHeight;
		double boardDownRightUTMX = BoardParam.boardUpLeftUTMX + UTMWidth;
		double boardDownRightUTMY = BoardParam.boardUpLeftUTMY - UTMHeight;
		GeoCoordinates boardLeftUpGeo = GUIHelper.UTMToGeo(boardUpLeftUTMX, boardUpLeftUTMY);
		GeoCoordinates boardRightUpGeo = GUIHelper.UTMToGeo(boardUpRightUTMX, boardUpRightUTMY);
		GeoCoordinates boardLeftBottomGeo = GUIHelper.UTMToGeo(boardDownLeftUTMX, boardDownLeftUTMY);
		GeoCoordinates boardRightBottomGeo = GUIHelper.UTMToGeo(boardDownRightUTMX, boardDownRightUTMY);
		double minLatitude = Math.min(Math.min(boardLeftBottomGeo.latitude, boardLeftUpGeo.latitude), Math.min(boardRightBottomGeo.latitude, boardRightUpGeo.latitude));
		double maxLatitude = Math.max(Math.max(boardLeftBottomGeo.latitude, boardLeftUpGeo.latitude), Math.max(boardRightBottomGeo.latitude, boardRightUpGeo.latitude));
		double minLongitude = Math.min(Math.min(boardLeftBottomGeo.longitude, boardLeftUpGeo.longitude), Math.min(boardRightBottomGeo.longitude, boardRightUpGeo.longitude));
		double maxLongitude = Math.max(Math.max(boardLeftBottomGeo.longitude, boardLeftUpGeo.longitude), Math.max(boardRightBottomGeo.longitude, boardRightUpGeo.longitude));
	
		// UTM and Screen size to be drawn
		UTMCoordinates upLeftUTMCorner = GUIHelper.geoToUTM(maxLatitude, minLongitude);
		UTMCoordinates upRightUTMCorner = GUIHelper.geoToUTM(maxLatitude, maxLongitude);
		UTMCoordinates downLeftUTMCorner = GUIHelper.geoToUTM(minLatitude, minLongitude);
		double imagesUTMTotalWidth = Point2D.distance(upLeftUTMCorner.Easting, upLeftUTMCorner.Northing, upRightUTMCorner.Easting, upRightUTMCorner.Northing);
		double imagesUTMTotalHeight = Point2D.distance(upLeftUTMCorner.Easting, upLeftUTMCorner.Northing, downLeftUTMCorner.Easting, downLeftUTMCorner.Northing);
		int imagesPXTotalWidth = (int)(imagesUTMTotalWidth*BoardParam.screenScale);
		int imagesPXTotalHeight = (int)(imagesUTMTotalHeight*BoardParam.screenScale);
	
		//  Google Static Map zoom level calculated for the first tile
		int tile1PXWidth = Math.min(BoardParam.MAX_IMAGE_PX, imagesPXTotalWidth);
		int tile1PXHeight = Math.min(BoardParam.MAX_IMAGE_PX, imagesPXTotalHeight);
		double tile1UTMWidth = imagesUTMTotalWidth*tile1PXWidth/imagesPXTotalWidth;
		double tile1UTMHeight = imagesUTMTotalHeight*tile1PXHeight/imagesPXTotalHeight;
		double incXX = (upRightUTMCorner.Easting-upLeftUTMCorner.Easting)*tile1UTMWidth/imagesUTMTotalWidth/2;
		double incXY = (downLeftUTMCorner.Easting-upLeftUTMCorner.Easting)*tile1UTMHeight/imagesUTMTotalHeight/2;
		double tile1UTMCenterX = upLeftUTMCorner.Easting + incXX + incXY;
		double incYX = (upRightUTMCorner.Northing-upLeftUTMCorner.Northing)*tile1UTMWidth/imagesUTMTotalWidth/2;
		double incYY = (downLeftUTMCorner.Northing-upLeftUTMCorner.Northing)*tile1UTMHeight/imagesUTMTotalHeight/2;
		double tile1UTMCenterY = upLeftUTMCorner.Northing + incYX + incYY;
	
		GeoCoordinates tile1GeoCenter = GUIHelper.UTMToGeo(tile1UTMCenterX, tile1UTMCenterY);
		int zoom = BoardHelper.getMapZoom(tile1GeoCenter.latitude,
				tile1GeoCenter.longitude, tile1PXWidth, tile1PXHeight, tile1UTMWidth, tile1UTMHeight);
	
		//  Number of tiles calculus
		int xTiles, yTiles;
		xTiles = yTiles = 0;
		int remainingPX = imagesPXTotalWidth;
		while (remainingPX>BoardParam.MAX_IMAGE_PX) {
			xTiles++;
			remainingPX = remainingPX - BoardParam.MAX_IMAGE_PX;
		}
		xTiles++;
		remainingPX = imagesPXTotalHeight;
		while (remainingPX>BoardParam.MAX_IMAGE_PX) {
			yTiles++;
			remainingPX = remainingPX - BoardParam.MAX_IMAGE_PX;
		}
		yTiles++;
	
		// Center of each tile calculus and image download
		BoardParam.map = new BackgroundMap[xTiles][yTiles];
		BoardParam.mapDownloadErrorText = new String[xTiles][yTiles];
		GeoCoordinates tileCenter;
		int PXWidth, PXHeight;
		for (int i=0; i<BoardParam.map.length; i++) {
			for (int j=0; j<BoardParam.map[i].length; j++) {
				double originX = upLeftUTMCorner.Easting + i*incXX*2 + j*incXY*2;
				double originY = upLeftUTMCorner.Northing + i*incYX*2 + j*incYY*2;
				PXWidth = Math.min(imagesPXTotalWidth - tile1PXWidth*i, BoardParam.MAX_IMAGE_PX);
				PXHeight = Math.min(imagesPXTotalHeight - tile1PXHeight*j, BoardParam.MAX_IMAGE_PX);
				UTMWidth = PXWidth/BoardParam.screenScale;
				UTMHeight = PXHeight/BoardParam.screenScale;
				double UTMx = originX + incXX*UTMWidth/tile1UTMWidth + incXY*UTMHeight/tile1UTMHeight;
				double UTMy = originY + incYX*UTMWidth/tile1UTMWidth + incYY*UTMHeight/tile1UTMHeight;
				tileCenter = GUIHelper.UTMToGeo(UTMx, UTMy);
				BoardParam.map[i][j] = new BackgroundMap(tileCenter.latitude,
						tileCenter.longitude, zoom, PXWidth, PXHeight, UTMx, UTMy);
				if (BoardParam.map[i][j].img == null) {
					BoardParam.mapDownloadErrorText[i][j] = Text.DOWNLOAD_ERROR;
				}
			}
		}
	}

	/** Calculates the Google Static Maps zoom level. */
	private static int getMapZoom(double latitude, double longitude,
			int pxWidth, int pxHeight,
			double utmWidth, double utmHeight) {
		int zoom = 21;	// We start on the maximum zoom level
		MercatorProjection projection;
		boolean scaleFound = false;
		while (zoom>0 && !scaleFound) {
			projection = new MercatorProjection(
					latitude,
					longitude,
					pxWidth,
					pxHeight,
					Math.pow(2, zoom));
			GeoCoordinates upLeftGeo = projection.getGeoLocation(0, 0);
			UTMCoordinates upLeftUTM = GUIHelper.geoToUTM(upLeftGeo.latitude, upLeftGeo.longitude);
			GeoCoordinates bottomRightGeo = projection.getGeoLocation(pxWidth, pxHeight);
			UTMCoordinates bottomRightUTM = GUIHelper.geoToUTM(bottomRightGeo.latitude, bottomRightGeo.longitude);
			double width, height;
			width = bottomRightUTM.Easting - upLeftUTM.Easting;
			height = upLeftUTM.Northing - bottomRightUTM.Northing;
			if (width>=utmWidth && height>=utmHeight) {
				scaleFound = true;
			}
			if (!scaleFound) {
				zoom--;
			}
		}
		return zoom;
	}
	
	/** Draws the main panel. */
	public static void paintBoard(Graphics g, BoardPanel p) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
				RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g2.setRenderingHint( RenderingHints.  KEY_STROKE_CONTROL,
				RenderingHints.VALUE_STROKE_PURE);
	
		// At the beginning, only the UAVs can be drawn
		if (!BoardParam.drawAll) {
			int numUAVsConnected = BoardHelper.getNumUAVsConnected();
			if (numUAVsConnected > BoardParam.numUAVsDrawn.get()) {
				BoardHelper.setScale();
				if (Param.simulationIsMissionBased) {
					MissionHelper.rescaleMissionDataStructures();
				} else {
					SwarmHelper.rescaleSwarmDataStructures();
				}
				BoardHelper.rescaleScaleBar();
				BoardParam.numUAVsDrawn.set(numUAVsConnected);
				if (numUAVsConnected == Param.numUAVs) {
					BoardParam.drawAll = true;
				}
			}
		} else {
			// All UAVs connected. Draw everything
			if (BoardParam.rescaleQueries.getAndSet(0) > 0) {
				BoardHelper.setScale();
				if (Param.simulationIsMissionBased) {
					MissionHelper.rescaleMissionDataStructures();
				} else {
					SwarmHelper.rescaleSwarmDataStructures();
				}
				BoardHelper.rescaleUAVPath();
				BoardHelper.rescaleMissions();
				if (Param.simulationIsMissionBased) {
					MissionHelper.rescaleMissionResources();
				} else {
					SwarmHelper.rescaleSwarmResources();
				}
				BoardHelper.rescaleScaleBar();	// Gets the scale bar
			}
		}
		BoardHelper.drawAll(g2, p);
	}
	
	/** Counts the number of UAVs that can be drawn (valid coordinates received). */
	private static int getNumUAVsConnected() {
		int numUAVsConnected = 0;
		for (int i=0; i<Param.numUAVs; i++) {
			// When UAVs have just connected, the previous position is null
			if (BoardParam.uavPrevUTMLocation[i] == null) {
				BoardParam.uavPrevUTMLocation[i] = SimParam.uavUTMPathReceiving[i].poll();// If queue already empty, tries to get some data is received
			}
			if (BoardParam.uavPrevUTMLocation[i] != null) {
				numUAVsConnected++;
			}
		}
		return numUAVsConnected;
	}
	
	/** Calculates the screen-UTM scale when the UAVs are connected.
	 * <p>If none is connected, returns false. */
	private static void setScale() {
		BoardHelper.getObjectsBoundaries();
		
		// 1. UTM rectangle extended to make room on the screen limits
		double widthUTM, heightUTM;
		widthUTM = (BoardParam.xUTMmax - BoardParam.xUTMmin)*BoardParam.SCALE_MAGNIFIER;
		heightUTM = (BoardParam.yUTMmax - BoardParam.yUTMmin)*BoardParam.SCALE_MAGNIFIER;
		// If only one UAV is on the screen, put it in the middle on a fixed UTM size
		if (Param.numUAVs==1 && widthUTM==0 && heightUTM==0) {
			widthUTM = BoardParam.MAX_SINGLE_RANGE*2*BoardParam.SCALE_MAGNIFIER;
			heightUTM = widthUTM;
		}
		// The UAVs could be horizontally or vertically aligned
		if (widthUTM==0) {
			widthUTM = heightUTM * SimParam.boardPXWidth/((double)SimParam.boardPXHeight);
		}
		if (heightUTM==0) {
			heightUTM = widthUTM * SimParam.boardPXHeight/((double)SimParam.boardPXWidth);
		}
	
		// 2. Screen-UTM scale factor and UTM bottom-left origin
		if (SimParam.boardPXWidth/((double)SimParam.boardPXHeight)>widthUTM/heightUTM) {
			BoardParam.screenScale = SimParam.boardPXHeight/heightUTM;
			BoardParam.boardUTMx0 = BoardParam.xUTMmin + (BoardParam.xUTMmax - BoardParam.xUTMmin)/2 - (SimParam.boardPXWidth/BoardParam.screenScale)/2;
			BoardParam.boardUTMy0 = BoardParam.yUTMmin - (heightUTM - (BoardParam.yUTMmax - BoardParam.yUTMmin))/2;
		} else {
			BoardParam.screenScale = SimParam.boardPXWidth/widthUTM;
			BoardParam.boardUTMx0 = BoardParam.xUTMmin - (widthUTM - (BoardParam.xUTMmax - BoardParam.xUTMmin))/2;
			BoardParam.boardUTMy0 = BoardParam.yUTMmin + (BoardParam.yUTMmax - BoardParam.yUTMmin)/2 - (SimParam.boardPXHeight/BoardParam.screenScale)/2;
		}
	
		// 3. Fist position of each UAV transformation from UTM to Screen
		LogPoint locationUTM;
		Point2D.Double locationPX;
		for (int i=0; i<Param.numUAVs; i++) {
			if (BoardParam.uavPrevUTMLocation[i] != null) {
				locationUTM = BoardParam.uavPrevUTMLocation[i];
				locationPX = BoardHelper.locatePoint(locationUTM.x, locationUTM.y);
				BoardParam.uavPrevPXLocation[i]
						= new LogPoint(locationUTM.time, locationPX.x, locationPX.y, locationUTM.z, locationUTM.heading, locationUTM.speed, locationUTM.inTest);
			}
		}
	
		// 4. Detect if the needed missions are loaded (and scale is set)
		if (Param.simStatus == SimulatorState.STARTING_UAVS
				&& InitialConfigurationThread.UAVS_CONFIGURED.get() == Param.numUAVs	// All configured
				&& BoardParam.numMissionsDrawn == Param.numMissionUAVs.get()) {			// All already drawn
			Param.simStatus = SimulatorState.UAVS_CONFIGURED;
		}
	}
	
	/** Calculates the boundaries of the rectangle that includes all the objects to be drawn. */
	private static void getObjectsBoundaries() {
		int numMissionsDrawn = 0;
		LogPoint location;
		WaypointSimplified location2;
		for (int i=0; i<Param.numUAVs; i++) {
			// The UAV location
			if (BoardParam.uavPrevUTMLocation[i] != null) {
				location = BoardParam.uavPrevUTMLocation[i];
				if (location.x < BoardParam.xUTMmin) BoardParam.xUTMmin = location.x;
				if (location.x > BoardParam.xUTMmax) BoardParam.xUTMmax = location.x;
				if (location.y < BoardParam.yUTMmin) BoardParam.yUTMmin = location.y;
				if (location.y > BoardParam.yUTMmax) BoardParam.yUTMmax = location.y;
			}
	
			// The UAV mission
			if (UAVParam.missionUTMSimplified != null) {
				List<WaypointSimplified> list = UAVParam.missionUTMSimplified.get(i);
				if (list != null) {
					if (list.size()>0) {
						numMissionsDrawn++;
						for (int j=0; j<list.size(); j++) {
							location2 = list.get(j);
							if (location2.x < BoardParam.xUTMmin) BoardParam.xUTMmin = location2.x;
							if (location2.x > BoardParam.xUTMmax) BoardParam.xUTMmax = location2.x;
							if (location2.y < BoardParam.yUTMmin) BoardParam.yUTMmin = location2.y;
							if (location2.y > BoardParam.yUTMmax) BoardParam.yUTMmax = location2.y;
						}
					}
				}
			}
		}
		BoardParam.numMissionsDrawn = numMissionsDrawn;
	}
	
	/** Builds the scale bar shown each time the scale changes. */
	private static void rescaleScaleBar() {
		Rectangle r = MainWindow.boardPanel.getBounds();
		BufferedImage scaleBarImage = GraphicsEnvironment.getLocalGraphicsEnvironment().
				getDefaultScreenDevice().getDefaultConfiguration().
				createCompatibleImage(Math.min(r.width, BoardParam.MIN_SCALE_PANEL_WIDTH),
						Math.min(r.height, BoardParam.MIN_SCALE_PANEL_HEIGHT), Transparency.TRANSLUCENT);
		Graphics2D g = scaleBarImage.createGraphics();
		double minUTMSize = BoardParam.MIN_PX_SCALE_LENGTH/BoardParam.screenScale;
		double maxUTMSize = BoardParam.MAX_PX_SCALE_LENGTH/BoardParam.screenScale;
		g.setColor(BoardParam.SCALE_COLOR);
		g.setStroke(BoardParam.STROKE_TRACK);
		if (minUTMSize>1) {
			int minUTMCharacterslength = (int)(Math.log10((int)minUTMSize)+1);	// Length of the String that represents the size
			int intervalUTMLength = (int)Math.pow(10, minUTMCharacterslength-1);
			int intervals = (int)Math.floor(maxUTMSize/intervalUTMLength);
			int scaleBarPXLength = (int)(intervals*intervalUTMLength*BoardParam.screenScale);
			Shape line = new Line2D.Double(BoardParam.SCALE_ORIGIN, BoardParam.SCALE_ORIGIN,
					BoardParam.SCALE_ORIGIN + scaleBarPXLength, BoardParam.SCALE_ORIGIN);
			g.draw(line);
			line = new Line2D.Double(BoardParam.SCALE_ORIGIN, BoardParam.SCALE_ORIGIN - BoardParam.HALF_LINE_LENGTH,
					BoardParam.SCALE_ORIGIN, BoardParam.SCALE_ORIGIN + BoardParam.HALF_LINE_LENGTH);
			g.draw(line);
			line = new Line2D.Double(BoardParam.SCALE_ORIGIN + scaleBarPXLength, BoardParam.SCALE_ORIGIN - BoardParam.HALF_LINE_LENGTH,
					BoardParam.SCALE_ORIGIN + scaleBarPXLength, BoardParam.SCALE_ORIGIN + BoardParam.HALF_LINE_LENGTH);
			g.draw(line);
			g.setFont(g.getFont().deriveFont(Font.BOLD));
			g.drawString(intervalUTMLength*intervals + " " + Text.METERS,
					(int)(BoardParam.SCALE_ORIGIN + scaleBarPXLength) + BoardParam.TEXT_OFFSET, BoardParam.SCALE_ORIGIN);
			g.dispose();
			BoardParam.scaleBarImage = scaleBarImage;
		}
	}
	
	/** Calculates the screen position of the UAV path. */
	private static void rescaleUAVPath() {
		LogPoint locationUTM;
		Point2D.Double locationPX;
		for (int i=0; i<Param.numUAVs; i++) {
			if (SimParam.uavUTMPath[i].size()>1) {
				BoardParam.uavPXPathLines[i].clear();
				locationUTM = SimParam.uavUTMPath[i].get(0);
				locationPX = BoardHelper.locatePoint(locationUTM.x, locationUTM.y);
				LogPoint prevLocation = new LogPoint(locationUTM.time, locationPX.x, locationPX.y, locationUTM.z, locationUTM.heading, locationUTM.speed, locationUTM.inTest);
				LogPoint nextLocation;
				Shape line;
				for (int j=1; j<SimParam.uavUTMPath[i].size(); j++) {
					locationUTM = SimParam.uavUTMPath[i].get(j);
					locationPX = BoardHelper.locatePoint(locationUTM.x, locationUTM.y);
					nextLocation = new LogPoint(locationUTM.time, locationPX.x, locationPX.y, locationUTM.z, locationUTM.heading, locationUTM.speed, locationUTM.inTest);
					if (nextLocation.distance(prevLocation.x, prevLocation.y)>=BoardParam.minScreenMovement) {
						line = new Line2D.Double(prevLocation.x, prevLocation.y, nextLocation.x, nextLocation.y);
						BoardParam.uavPXPathLines[i].add(line);
						prevLocation = nextLocation;
						BoardParam.uavPrevPXLocation[i] = prevLocation;
					}
				}
			}
		}
	}
	
	/** Calculates the mission path of each UAV in screen coordinates, each time the visualization scale changes. */
	private static void rescaleMissions() {
		WaypointSimplified locationUTM;
		Point2D.Double locationPX;
		Point3D prevWaypoint, nextWaypoint;
		if (UAVParam.missionUTMSimplified != null) {
			for (int i=0; i<Param.numUAVs; i++) {
				List<WaypointSimplified> mission = UAVParam.missionUTMSimplified.get(i);
				if (mission != null && mission.size() > 0) {
					UAVParam.MissionPx[i].clear();
					locationUTM = mission.get(0);
					locationPX = BoardHelper.locatePoint(locationUTM.x, locationUTM.y);
					prevWaypoint = new Point3D(locationPX.x, locationPX.y, locationUTM.z);
					for (int j=1; j<mission.size(); j++) {
						locationUTM = mission.get(j);
						locationPX = BoardHelper.locatePoint(locationUTM.x, locationUTM.y);
						nextWaypoint = new Point3D(locationPX.x, locationPX.y, locationUTM.z);
						UAVParam.MissionPx[i].add(new Line2D.Double(prevWaypoint.x, prevWaypoint.y, nextWaypoint.x, nextWaypoint.y));
						prevWaypoint = nextWaypoint;
					}
				}
			}
		}
	}
	
	/** Auxiliary method to draw all the elements calculated. */
	private static void drawAll(Graphics2D g, BoardPanel p) {
		// 1. Draw the background, if it is available
		BoardHelper.drawBackground(g, p);
	
		// 2. Draw the waypoints of the missions
		//   It's not necessary to check if the experiment is based on missions. They are printed only if not null
		g.setStroke(BoardParam.STROKE_WP_LIST);
		BoardHelper.drawMissions(g);
	
		// 3. Draw the UAVs path
		g.setStroke(BoardParam.STROKE_TRACK);
		BoardHelper.drawUAVPath(g);
	
		// 4. Draw aditional resources
		if (Param.simulationIsMissionBased) {
			MissionHelper.drawMissionResources(g, p);
		} else {
			SwarmHelper.drawSwarmResources(g, p);
		}
	
		// 5. Draw the UAVs image, identifier and altitude value
		BoardHelper.drawUAVs(g, p);
	
		// 6. Draw the wind orientation, if needed
		g.setColor(Color.BLACK);
		if (Param.windSpeed > 0.0 && BoardParam.arrowImageRotated != null) {
			g.drawImage(BoardParam.arrowImageRotated, 0, 0, BoardParam.arrowImageRotated.getWidth(),
					BoardParam.arrowImageRotated.getHeight(), p);
		}
	
		// 7. Draw the map scale
		if (BoardParam.scaleBarImage != null) {
			g.drawImage(BoardParam.scaleBarImage, 0, 0, BoardParam.scaleBarImage.getWidth(), BoardParam.scaleBarImage.getHeight(), p);
		}
	
		// 8. Draw the copyright of the map
		if (BoardParam.map == null) {
			if (UAVParam.numGPSFixed.get() != Param.numUAVs) {
				if (UAVParam.numMAVLinksOnline.get() != Param.numUAVs) {
					g.drawString(Text.WAITING_MAVLINK, 10, SimParam.boardPXHeight-10);
				} else {
					g.drawString(Text.WAITING_GPS, 10, SimParam.boardPXHeight-10);
				}
				
			} else if (Param.simulationIsMissionBased) {
				g.drawString(Text.WAITING_MISSION_UPLOAD, 10, SimParam.boardPXHeight-10);
			}
		} else {
			g.drawString(Text.COPYRIGHT, 10, SimParam.boardPXHeight-10);
		}
	}

	/** Draws the background. */
	private static void drawBackground(Graphics2D g, BoardPanel p) {
		FontMetrics metrics = g.getFontMetrics();
		if (BoardParam.map!=null) {
			Point2D.Double locationPX;
			// Draw images
			for (int i=0; i<BoardParam.map.length; i++) {
				if (BoardParam.map[i] != null) {
					for (int j=0; j<BoardParam.map[i].length; j++) {
						if (BoardParam.map[i][j] != null) {
							if (BoardParam.map[i][j].img!=null) {
								// Draw image
								locationPX = BoardHelper.locateImage(BoardParam.map[i][j].originUTM.Easting,
										BoardParam.map[i][j].originUTM.Northing);
								AffineTransform trans = new AffineTransform();
								trans.translate(locationPX.x,
										locationPX.y);
								trans.rotate(-BoardParam.map[i][j].alfa);
								trans.scale(BoardParam.map[i][j].xScale,
										BoardParam.map[i][j].yScale);
								g.drawImage(BoardParam.map[i][j].img, trans, p);
							}
						}
					}
				}
			}
			// Draw error messages
			for (int i=0; i<BoardParam.map.length; i++) {
				if (BoardParam.map[i] != null) {
					for (int j=0; j<BoardParam.map[i].length; j++) {
						if (BoardParam.map[i][j] != null) {
							if (BoardParam.map[i][j].img!=null && BoardParam.mapDownloadErrorText[i][j] != null) {
								// Draw download error message
								locationPX = BoardHelper.locatePoint(BoardParam.map[i][j].centerX, BoardParam.map[i][j].centerY);
								g.drawString(BoardParam.mapDownloadErrorText[i][j],
										(int)locationPX.x - metrics.stringWidth(BoardParam.mapDownloadErrorText[i][j])/2,
										(int)locationPX.y - metrics.getHeight()/2 + metrics.getAscent());
							}
						}
					}
				}
			}
		}
	}

	/** Locates an image on screen.
	 * <p>originUTMX,originUTMY. Image UTM upper-left corner coordinates. */
	private static Point2D.Double locateImage(double originUTMX, double originUTMY) {
		Point2D.Double res = new Point2D.Double();
		double xUTM = originUTMX - BoardParam.boardUpLeftUTMX;
		double yUTM = originUTMY - BoardParam.boardUpLeftUTMY;
		int xPX = (int) Math.round(xUTM*BoardParam.screenScale);
		int yPX = -(int) Math.round(yUTM*BoardParam.screenScale);	// Negative, as panels draw from up to down
		res.setLocation(xPX, yPX);
		return res;
	}
	
	/** Draws the waypoints of the missions. */
	private static void drawMissions(Graphics2D g) {
		for (int i=0; i<Param.numUAVs; i++) {
			if (!UAVParam.MissionPx[i].isEmpty()) {
				g.setColor(SimParam.COLOR[i%SimParam.COLOR.length]);
				for (int j=0; j<UAVParam.MissionPx[i].size(); j++) {
					g.draw(UAVParam.MissionPx[i].get(j));
				}
			}
		}
	}

	/** Draws the UAV path. */
	private static void drawUAVPath(Graphics2D g) {
		LogPoint currentUTM;
		Point2D.Double currentPXauxiliary;
		LogPoint currentPX;
		LogPoint previousPX;
		for (int i=0; i<Param.numUAVs; i++) {
			//  1. Create new lines if new positions are stored
			while(!SimParam.uavUTMPathReceiving[i].isEmpty()) {
				currentUTM = SimParam.uavUTMPathReceiving[i].poll();
				if (currentUTM != null) {
					// Info storage to redraw in case the screen is rescaled and for logging purposes
					SimParam.uavUTMPath[i].add(currentUTM);
	
					// Store the current position to draw the UAV
					BoardParam.uavCurrentUTMLocation[i] = currentUTM;
					// The UAV is connected. New lines can be drawn
					currentPXauxiliary = BoardHelper.locatePoint(currentUTM.x, currentUTM.y);
					currentPX
						= new LogPoint(currentUTM.time, currentPXauxiliary.x, currentPXauxiliary.y, currentUTM.z, currentUTM.heading,
							currentUTM.speed, currentUTM.inTest);
					BoardParam.uavCurrentPXLocation[i] = currentPX;
					
					if (BoardParam.uavPrevUTMLocation[i] != null) {
						// New line only if the UAV has moved far enough
						previousPX = BoardParam.uavPrevPXLocation[i];
						if (currentPX.distance(previousPX.x, previousPX.y)>=BoardParam.minScreenMovement) {
							Shape l = new Line2D.Double(previousPX.x, previousPX.y,
									currentPX.x,
									currentPX.y);
							BoardParam.uavPXPathLines[i].add(l);
							
							BoardParam.uavPrevUTMLocation[i] = currentUTM;
							BoardParam.uavPrevPXLocation[i] = currentPX;
						}
					}
				}
			}
			
			// 2. Draw all the lines
			g.setColor(SimParam.COLOR[i%SimParam.COLOR.length]);
			for (int j=0; j<BoardParam.uavPXPathLines[i].size(); j++) {
				g.draw(BoardParam.uavPXPathLines[i].get(j));
			}
		}
	}

	/** Draws the UAV image, identifier and altitude value. */
	private static void drawUAVs(Graphics2D g, BoardPanel p) {
		LogPoint currentPXLocation;
		for (int i=0; i<Param.numUAVs; i++) {
			// Only drawn when there is known current position
			currentPXLocation = BoardParam.uavCurrentPXLocation[i];
			if (currentPXLocation != null) {
				// AffineTransform is applied on inverse order
				AffineTransform trans = new AffineTransform();
				trans.translate(currentPXLocation.x, currentPXLocation.y);
				trans.scale(SimParam.uavImageScale, SimParam.uavImageScale);
				trans.rotate( currentPXLocation.heading );
				trans.translate(-SimParam.uavImage.getWidth()/2, -SimParam.uavImage.getHeight()/2);
				g.drawImage(SimParam.uavImage, trans, p);
	
				//  Draw altitude text
				g.setColor(Color.BLACK);
				g.drawString(
						String.format("%.2f",currentPXLocation.z),
						(float)(currentPXLocation.x+10),
						(float)(currentPXLocation.y-10));
	
				//  Draw UAV id
				g.drawString(Text.UAV_ID + " " + Param.id[i],
						(float)(currentPXLocation.x-25),
						(float)(currentPXLocation.y+25));
			}
		}
	}

	/** Locates a UTM point on the screen, using the current screen scale. */
	public static Point2D.Double locatePoint(double inUTMX, double inUTMY) {
		double xUTM = inUTMX - BoardParam.boardUTMx0;
		double yUTM = inUTMY - BoardParam.boardUTMy0;
		int xPX = (int)Math.round(xUTM*BoardParam.screenScale);
		int yPX = SimParam.boardPXHeight - (int)Math.round(yUTM*BoardParam.screenScale);	// The "y" coordinate is drawn on inverse order
		// In case a UAV goes out of the screen
		if (yPX > SimParam.boardPXHeight || yPX < 0) {
			SimTools.println(Text.UAV_OUT_OF_SCREEN_ERROR);
		}
		return new Point2D.Double(xPX, yPX);
	}
	
}
