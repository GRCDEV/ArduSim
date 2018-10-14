package uavFishing.pojo;

/** 
 * Class whit methods for some vector operations.
 * @author Francisco Barella Badia
 *
 */
public class VectorMath {
	
	static double[] axis1 = {1,0};
	static double[] axis2 = {0,1};
	static double[] axis3 = {-1,0};
	static double[] axis4 = {0,-1};
	
	/**
	 * Calculates module of a given vector 
	 * @param componentX Vector component on axis X
	 * @param componentY Vector component on axis Y
	 * @return The module of the argument
	 */
	public static double getModule(double componentX,double componentY) {
		
		return Math.sqrt((componentX * componentX) + (componentY*componentY));
	}

	public static double getModulo(double vector2D[]) {
		
		return getModule(vector2D[0],vector2D[1]);
	}
	/**
	 * Returns unitary vector
	 * @param componentX Vector component on axis X
	 * @param componentY Vector component on axis Y
	 * @return 
	 */
	public static double[] getUnitaryVector(double componentX,double componentY) {
		
		double modulo = VectorMath.getModule(componentX,componentY);
		double[] unitario = new double[2];
		
		unitario[0] = componentX / modulo;
		unitario[1] = componentY / modulo;
		return unitario;
	}
	
	public static double[] getUnitaryVector(double vector2D[]) {
		
		return getUnitaryVector(vector2D[0],vector2D[1]);
	}
	
	
	public static int getQuadrant(double componentX,double componentY) {
		
		int cuadrante = 0;
		
		if (componentX > 0 && componentY >= 0) cuadrante = 1;
		if (componentX <= 0 && componentY >= 0) cuadrante = 2;
		if (componentX < 0 && componentY <= 0) cuadrante = 3;
		if (componentX >= 0 && componentY < 0) cuadrante = 4;
		
		return cuadrante;
	}
	public static int getQuadrant(double vector2D[]) {
		
		return getQuadrant(vector2D[0],vector2D[1]);
	}
	
	
	public static double getVectorsAngle(double component1X,double component1Y,double component2X,double component2Y){
		
		double cosangulo,angulo;
		
		cosangulo = (component1X*component2X + component1Y*component2Y) / (VectorMath.getModule(component1X,component1Y)*VectorMath.getModule(component2X,component2Y));
		angulo = Math.acos(cosangulo);
		
		
		return angulo;
	}
	
	public static double getVectorsAngle(double vector2D1[],double vector2D2[]){
		
		return getVectorsAngle(vector2D1[0],vector2D1[1],vector2D2[0],vector2D2[1]);
	}
	
	public static double[] rotateVector(double componentX,double componentY,double angulo, boolean clockwise) {
		
		double z,a,b,c,anguloRad,modulo, y1,y2,x1,x2;
		double [] vector2 = new double[2];
		double [] vector3 = new double[2];
		int cuadrante;
		
		modulo = VectorMath.getModule(componentX,componentY);
		
		anguloRad = Math.toRadians(angulo);
		
		z = Math.cos(anguloRad)*(modulo*modulo);
		
		if(componentX == 0) {
			
			
			y1= y2 = z / componentY;
			
			a = 1;
			b = 0;
			c = (y1*y1)-(modulo*modulo);
			
			
			x1 = (b+Math.sqrt((b*b)-4*a*c))/(2*a);
			x2 = (b-Math.sqrt((b*b)-4*a*c))/(2*a);
			
	
		}
		else {
		
		a = ((componentY*componentX) + (componentX*componentX)) / (componentX*componentX);
		b = (2 * z * componentY) / (componentX*componentX);
		c = ((z*z)-(componentX * componentX) * (modulo*modulo)) / (componentX*componentX);
		
		y1 = (b+Math.sqrt((b*b)-4*a*c))/(2*a);
		y2 = (b-Math.sqrt((b*b)-4*a*c))/(2*a);
		
		x1 = (z-(componentY*y1)) / componentX;
		x2 = (z-(componentY*y2)) / componentX;
		
		
		}
		
		vector2[0] = x1;
		vector2[1] = y1;
		
		vector3[0] = x2;
		vector3[1] = y2;
		
		cuadrante = VectorMath.getQuadrant(componentX, componentY);
		if(clockwise) {
			switch(cuadrante) {
			case 1: 
				if(angulo<=180) {
					if(VectorMath.getVectorsAngle(vector2,VectorMath.axis4)<=VectorMath.getVectorsAngle(vector3,VectorMath.axis4)) return vector2;
					else return vector3;	
				} else {
					if(VectorMath.getVectorsAngle(vector2,VectorMath.axis2)<=VectorMath.getVectorsAngle(vector3,VectorMath.axis2)) return vector2;
					else return vector3;
				}
			case 2:
				if(angulo<=180) {
					if(VectorMath.getVectorsAngle(vector2,VectorMath.axis1)<=VectorMath.getVectorsAngle(vector3,VectorMath.axis1)) return vector2;
					else return vector3;	
				} else {
					if(VectorMath.getVectorsAngle(vector2,VectorMath.axis3)<=VectorMath.getVectorsAngle(vector3,VectorMath.axis3)) return vector2;
					else return vector3;
				}
			case 3:
				if(angulo<=180) {
					if(VectorMath.getVectorsAngle(vector2,VectorMath.axis2)<=VectorMath.getVectorsAngle(vector3,VectorMath.axis2)) return vector2;
					else return vector3;	
				} else {
					if(VectorMath.getVectorsAngle(vector2,VectorMath.axis4)<=VectorMath.getVectorsAngle(vector3,VectorMath.axis4)) return vector2;
					else return vector3;
				}
			default:
				if(angulo<=180) {
					if(VectorMath.getVectorsAngle(vector2,VectorMath.axis3)<=VectorMath.getVectorsAngle(vector3,VectorMath.axis3)) return vector2;
					else return vector3;	
				} else {
					if(VectorMath.getVectorsAngle(vector2,VectorMath.axis1)<=VectorMath.getVectorsAngle(vector3,VectorMath.axis1)) return vector2;
					else return vector3;
				}
			
			}
		} else {
			switch(cuadrante) {
			case 1: 
				if(angulo<=180) {
					if(VectorMath.getVectorsAngle(vector2,VectorMath.axis2)<=VectorMath.getVectorsAngle(vector3,VectorMath.axis2)) return vector2;
					else return vector3;	
				} else {
					if(VectorMath.getVectorsAngle(vector2,VectorMath.axis4)<=VectorMath.getVectorsAngle(vector3,VectorMath.axis4)) return vector2;
					else return vector3;
				}
			case 2:
				if(angulo<=180) {
					if(VectorMath.getVectorsAngle(vector2,VectorMath.axis3)<=VectorMath.getVectorsAngle(vector3,VectorMath.axis3)) return vector2;
					else return vector3;	
				} else {
					if(VectorMath.getVectorsAngle(vector2,VectorMath.axis1)<=VectorMath.getVectorsAngle(vector3,VectorMath.axis1)) return vector2;
					else return vector3;
				}
			case 3:
				if(angulo<=180) {
					if(VectorMath.getVectorsAngle(vector2,VectorMath.axis4)<=VectorMath.getVectorsAngle(vector3,VectorMath.axis4)) return vector2;
					else return vector3;	
				} else {
					if(VectorMath.getVectorsAngle(vector2,VectorMath.axis2)<=VectorMath.getVectorsAngle(vector3,VectorMath.axis2)) return vector2;
					else return vector3;
				}
			default:
				if(angulo<=180) {
					if(VectorMath.getVectorsAngle(vector2,VectorMath.axis1)<=VectorMath.getVectorsAngle(vector3,VectorMath.axis1)) return vector2;
					else return vector3;	
				} else {
					if(VectorMath.getVectorsAngle(vector2,VectorMath.axis3)<=VectorMath.getVectorsAngle(vector3,VectorMath.axis3)) return vector2;
					else return vector3;
				}
			}
		}
	}
	
	public static double[] rotateVector(double[] vector2D,double angle,boolean clockwise) {
		
		return rotateVector(vector2D[0],vector2D[1],angle,clockwise);
	}
	
}

