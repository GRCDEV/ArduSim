package api.pojo.formations;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import api.Tools;

/** The formation numbering starts in 0 in the center of the formation and increases with the distance to the center of the formation.
 * <p>Developed by: Francisco José Fabra Collado, fron GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class MatrixCompactFlightFormation extends FlightFormation {
	
	protected MatrixCompactFlightFormation(int numUAVs, double minDistance) {
		super(numUAVs, minDistance);
	}

	@Override
	protected void initializeFormation() {
		this.centerUAV = 0;
		
		// 1. Get the number of layers around the center UAV to contain all the UAVs
		// 1.1. First, the minimum number of layers
		int n = 1;
		boolean minFound = false;
		int layer = 1;
		while (!minFound) {
			n = n + layer * 8;
			if (n >= this.numUAVs) {
				minFound = true;
			} else {
				layer++;
			}
		}
		
		// 1.2. Finally, add layers until the minimum distance to positions is greater or equal to the previous maximum distance
		double prevMaxDistance = layer * Math.sqrt(2);
		double minDistance;
		boolean found = false;
		while (!found) {
			minDistance = (layer + 1);
			if (minDistance >= prevMaxDistance) {
				found = true;
			} else {
				layer++;
				n = n + layer * 8;
			}
		}
		
		// 2. Create array with all the information needed to sort the positions in the flight formation
		FormationPointHelper[] point = new FormationPointHelper[n];
		point[0] = new FormationPointHelper(0, 0, 0, 0, 0);	// The center UAV
		// Process points on all layers
		int layers = layer;
		layer = 1;
		int p = 1;
		double distance, offsetX, offsetY;
		Point2D.Double upRightCorner, bottomLeftCorner;
		for (layer = 1; layer <= layers; layer++) {
			// The four corners of the layer
			distance = layer * this.minDistance * Math.sqrt(2);
			point[p] = new FormationPointHelper(distance, layer, 1, Tools.round(-layer * this.minDistance, 6), Tools.round(layer * this.minDistance, 6));
			p++;
			upRightCorner = new Point2D.Double(layer * this.minDistance, layer * this.minDistance);
			point[p] = new FormationPointHelper(distance, layer, 3, Tools.round(upRightCorner.x, 6), Tools.round(upRightCorner.y, 6));
			p++;
			bottomLeftCorner = new Point2D.Double(-layer * this.minDistance, -layer * this.minDistance);
			point[p] = new FormationPointHelper(distance, layer, 5, Tools.round(bottomLeftCorner.x, 6), Tools.round(bottomLeftCorner.y, 6));
			p++;
			point[p] = new FormationPointHelper(distance, layer, 6, Tools.round(layer * this.minDistance, 6), Tools.round(-layer * this.minDistance, 6));
			p++;
			int sidePoints = layer * 2 - 1;
			// Get points of each side
			//   left side
			for (int j = 1; j <= sidePoints; j++) {
				offsetY = bottomLeftCorner.y + this.minDistance * j;
				distance = Math.sqrt(Math.pow(bottomLeftCorner.x, 2) + Math.pow(offsetY, 2));
				point[p] = new FormationPointHelper(distance, layer, 1, Tools.round(bottomLeftCorner.x, 6), Tools.round(offsetY, 6));
				p++;
			}
			//   right side
			for (int j = 1; j <= sidePoints; j++) {
				offsetY = upRightCorner.y - this.minDistance * j;
				distance = Math.sqrt(Math.pow(upRightCorner.x, 2) + Math.pow(offsetY, 2));
				point[p] = new FormationPointHelper(distance, layer, 2, Tools.round(upRightCorner.x, 6), Tools.round(offsetY, 6));
				p++;
			}
			//   up side
			for (int j = 1; j <= sidePoints; j++) {
				offsetX = upRightCorner.x - this.minDistance * j;
				distance = Math.sqrt(Math.pow(offsetX, 2) + Math.pow(upRightCorner.y, 2));
				point[p] = new FormationPointHelper(distance, layer, 5, Tools.round(offsetX, 6), Tools.round(upRightCorner.y, 6));
				p++;
			}
			//   bottom side
			for (int j = 1; j <= sidePoints; j++) {
				offsetX = bottomLeftCorner.x + this.minDistance * j;
				distance = Math.sqrt(Math.pow(offsetX, 2) + Math.pow(bottomLeftCorner.y, 2));
				point[p] = new FormationPointHelper(distance, layer, 6, Tools.round(offsetX, 6), Tools.round(bottomLeftCorner.y, 6));
				p++;
			}
		}
		
		// 3. Sort points 1st by distance to the center UAV, 2nd by layer, 3rd by side
		Arrays.sort(point, 0, p);
		
		// 4. Store the result
		p = 0;
		int i = 0;
		int j;
		@SuppressWarnings("unchecked")
		Queue<FormationPointHelper>[] ordered = new LinkedList[7];
		FormationPointHelper extracted;
		for (int k = 0; k < ordered.length; k++) {
			ordered[k] = new LinkedList<FormationPointHelper>();
		}
		// For each distance
		while (i < n && p < this.numUAVs) {
			FormationPointHelper[] set = this.getSet(point, i);
			i = i + set.length;
			j = 0;
			// For each layer
			while (j < set.length && p < this.numUAVs) {
				FormationPointHelper[] subSet = this.getSubSet(set, j);
				j = j + subSet.length;
				for (int k = 0; k < subSet.length; k++) {
					ordered[subSet[k].side].add(subSet[k]);
				}
				// Store the result alternating between sides if possible
				int k = 0;
				int side = 0;
				while (k < subSet.length && p < this.numUAVs) {
					side = (side + 1) % 7;
					extracted = ordered[side].poll();
					if (extracted != null) {
						this.point[p] = new FormationPoint(p, extracted.offsetX, extracted.offsetY);
						p++;
						k++;
					}
				}
			}
		}
	}
	
	// Get set of points with the same distance
	private FormationPointHelper[] getSet(FormationPointHelper[] point, int startIndex) {
		double distance = point[startIndex].distance;
		boolean found = false;
		int endIndex = startIndex + 1;
		while (endIndex < point.length && !found) {
			if (point[endIndex].distance == distance) {
				endIndex++;
			} else {
				found = true;
			}
		}
		return Arrays.copyOfRange(point, startIndex, endIndex);
	}

	// Get subset of points with the same distance and in the same layer
	private FormationPointHelper[] getSubSet(FormationPointHelper[] point, int startIndex) {
		int layer = point[startIndex].layer;
		boolean found = false;
		int endIndex = startIndex + 1;
		while (endIndex < point.length && !found) {
			if (point[endIndex].layer == layer) {
				endIndex++;
			} else {
				found = true;
			}
		}
		return Arrays.copyOfRange(point, startIndex, endIndex);
	}

}
