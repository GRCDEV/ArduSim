package main.api.formations;

import api.API;
import main.api.ValidationTools;
import main.api.formations.helpers.FormationPoint;
import main.api.formations.helpers.FormationPointHelper;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeoutException;

/** The formation numbering starts in 0 in the center of the formation and increases with the distance to the center of the formation.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class FormationMeshCompact extends FlightFormation {

	private long start;
	private final long TIMEOUT = 3000; //after 3000 ms throw timeoutException
	protected FormationMeshCompact(int numUAVs, double minDistance, Formation formation) {
		super(numUAVs, minDistance, formation);
	}

	@Override
	protected void initializeFormation() throws TimeoutException {
		start = System.currentTimeMillis();
		this.centerUAVPosition = 0;
		
		// 1. Get the number of layers around the center UAV to contain all the UAVs
		// 1.1. First, the minimum number of layers
		int n = 1;
		boolean minFound = false;
		int layer = 1;
		while (!minFound) {
			if(timedOut()){
				throw new TimeoutException();
			}
			n = n + layer * 6;
			if (n >= this.numUAVs) {
				minFound = true;
			} else {
				layer++;
			}
		}
		
		// 1.2. Finally, add layers until the minimum distance to positions is greater or equal to the previous maximum distance
		double prevMaxDistance = layer;
		double sin60 = Math.sqrt(3) / 2;
		double minDistance;
		boolean found = false;
		while (!found) {
			if(timedOut()){
				throw new TimeoutException();
			}
			if (layer + 1 % 2 == 0) {
				minDistance = (layer + 1) * sin60;
			} else {
				minDistance = Math.sqrt(0.25 + Math.pow((layer + 1) * sin60, 2));
			}
			if (minDistance >= prevMaxDistance) {
				found = true;
			} else {
				layer++;
				n = n + layer * 6;
			}
		}
		
		// 2. Create array with all the information needed to sort the positions in the flight formation
		FormationPointHelper[] point = new FormationPointHelper[n];
		point[0] = new FormationPointHelper(0, 0, 0, 0, 0);	// The center UAV
		// Process points on all layers
		int layers = layer;
		int p = 1;
		double distance, offsetX, offsetY;
		Point2D.Double leftCorner, rightCorner, upLeftCorner, bottomLeftCorner;
		double incX = 0.5 * this.minDistance;
		double incY = sin60 * this.minDistance;
		ValidationTools validationTools = API.getValidationTools();
		for (layer = 1; layer <= layers; layer++) {
			// The six corners of the layer
			distance = layer * this.minDistance;
			leftCorner = new Point2D.Double(- layer * this.minDistance, 0);
			point[p] = new FormationPointHelper(distance, layer, 1, validationTools.roundDouble(leftCorner.x, 6), validationTools.roundDouble(leftCorner.y, 6));
			p++;
			rightCorner = new Point2D.Double(layer * this.minDistance, 0);
			point[p] = new FormationPointHelper(distance, layer, 2, validationTools.roundDouble(rightCorner.x, 6), validationTools.roundDouble(rightCorner.y, 6));
			p++;
			upLeftCorner = new Point2D.Double(-incX * layer, incY * layer);
			point[p] = new FormationPointHelper(distance, layer, 3, validationTools.roundDouble(upLeftCorner.x, 6), validationTools.roundDouble(upLeftCorner.y, 6));
			p++;
			point[p] = new FormationPointHelper(distance, layer, 4, validationTools.roundDouble(incX * layer, 6), validationTools.roundDouble(incY * layer, 6));
			p++;
			bottomLeftCorner = new Point2D.Double(-incX * layer, -incY * layer);
			point[p] = new FormationPointHelper(distance, layer, 5, validationTools.roundDouble(bottomLeftCorner.x, 6), validationTools.roundDouble(bottomLeftCorner.y, 6));
			p++;
			point[p] = new FormationPointHelper(distance, layer, 6, validationTools.roundDouble(incX * layer, 6), validationTools.roundDouble(-incY * layer, 6));
			p++;
			int sidePoints = layer - 1;
			// Get points of each side
			//   up-left side
			for (int j = 1; j <= sidePoints; j++) {
				offsetX = leftCorner.x + incX * j;
				offsetY = leftCorner.y + incY * j;
				distance = Math.sqrt(Math.pow(offsetX, 2) + Math.pow(offsetY, 2));
				point[p] = new FormationPointHelper(distance, layer, 1, validationTools.roundDouble(offsetX, 6), validationTools.roundDouble(offsetY, 6));
				p++;
			}
			//   up-right side
			for (int j = 1; j <= sidePoints; j++) {
				offsetX = rightCorner.x - incX * j;
				offsetY = rightCorner.y + incY * j;
				distance = Math.sqrt(Math.pow(offsetX, 2) + Math.pow(offsetY, 2));
				point[p] = new FormationPointHelper(distance, layer, 2, validationTools.roundDouble(offsetX, 6), validationTools.roundDouble(offsetY, 6));
				p++;
			}
			//   bottom-left side
			for (int j = 1; j <= sidePoints; j++) {
				offsetX = leftCorner.x + incX * j;
				offsetY = leftCorner.y - incY * j;
				distance = Math.sqrt(Math.pow(offsetX, 2) + Math.pow(offsetY, 2));
				point[p] = new FormationPointHelper(distance, layer, 3, validationTools.roundDouble(offsetX, 6), validationTools.roundDouble(offsetY, 6));
				p++;
			}
			//   bottom-right side
			for (int j = 1; j <= sidePoints; j++) {
				offsetX = rightCorner.x - incX * j;
				offsetY = rightCorner.y - incY * j;
				distance = Math.sqrt(Math.pow(offsetX, 2) + Math.pow(offsetY, 2));
				point[p] = new FormationPointHelper(distance, layer, 4, validationTools.roundDouble(offsetX, 6), validationTools.roundDouble(offsetY, 6));
				p++;
			}
			//   up side
			for (int j = 1; j <= sidePoints; j++) {
				offsetX = upLeftCorner.x + j * this.minDistance;
				offsetY = upLeftCorner.y;
				distance = Math.sqrt(Math.pow(offsetX, 2) + Math.pow(offsetY, 2));
				point[p] = new FormationPointHelper(distance, layer, 5, validationTools.roundDouble(offsetX, 6), validationTools.roundDouble(offsetY, 6));
				p++;
			}
			//   bottom side
			for (int j = 1; j <= sidePoints; j++) {
				offsetX = bottomLeftCorner.x + j * this.minDistance;
				offsetY = bottomLeftCorner.y;
				distance = Math.sqrt(Math.pow(offsetX, 2) + Math.pow(offsetY, 2));
				point[p] = new FormationPointHelper(distance, layer, 6, validationTools.roundDouble(offsetX, 6), validationTools.roundDouble(offsetY, 6));
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
			ordered[k] = new LinkedList<>();
		}
		// For each distance
		while (i < n && p < this.numUAVs) {
			if(timedOut()){
				throw new TimeoutException();
			}
			FormationPointHelper[] set = this.getSet(point, i);
			i = i + set.length;
			j = 0;
			// For each layer
			while (j < set.length && p < this.numUAVs) {
				if(timedOut()){
					throw new TimeoutException();
				}
				FormationPointHelper[] subSet = this.getSubSet(set, j);
				j = j + subSet.length;
				for (FormationPointHelper formationPointHelper : subSet) {
					ordered[formationPointHelper.side].add(formationPointHelper);
				}
				// Store the result alternating between sides if possible
				int k = 0;
				int side = 0;
				while (k < subSet.length && p < this.numUAVs) {
					if(timedOut()){
						throw new TimeoutException();
					}
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

	private boolean timedOut() {
		return (System.currentTimeMillis() - start) > TIMEOUT;
	}

	// Get set of points with the same distance
	private FormationPointHelper[] getSet(FormationPointHelper[] point, int startIndex) throws TimeoutException {
		double distance = point[startIndex].distance;
		boolean found = false;
		int endIndex = startIndex + 1;
		while (endIndex < point.length && !found) {
			if(timedOut()){
				throw new TimeoutException();
			}
			if (point[endIndex].distance == distance) {
				endIndex++;
			} else {
				found = true;
			}
		}
		return Arrays.copyOfRange(point, startIndex, endIndex);
	}
	
	// Get subset of points with the same distance and in the same layer
	private FormationPointHelper[] getSubSet(FormationPointHelper[] point, int startIndex) throws TimeoutException {
		int layer = point[startIndex].layer;
		boolean found = false;
		int endIndex = startIndex + 1;
		while (endIndex < point.length && !found) {
			if(timedOut()){
				throw new TimeoutException();
			}
			if (point[endIndex].layer == layer) {
				endIndex++;
			} else {
				found = true;
			}
		}
		return Arrays.copyOfRange(point, startIndex, endIndex);
	}

}
