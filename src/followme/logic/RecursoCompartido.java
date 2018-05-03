package followme.logic;

public class RecursoCompartido {

	public Nodo primero = null;
	private Nodo ultimo = null;
	public boolean vacio = true;
	public long instanteInicial = 0;
	public int length = 0;

	public synchronized Nodo pop() {
		while (vacio == true) {
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		Nodo aux = primero;
		primero = primero.next;
		if (primero == null) {
			vacio = true;
		}
		length-=1;
		return aux;

	}

	public synchronized Nodo pop(int type) {
		while (vacio == true) {
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		Nodo aux = null;
		do {
			aux = primero;
			primero = primero.next;
			if (primero == null) {
				vacio = true;
			}
		} while (aux.type != type);
		length-=1;
		return aux;

	}

	public synchronized void put(Nodo value) {

		if (vacio) {
			vacio = false;
			instanteInicial = value.time;
			value.time = 0;
			primero = value;
			ultimo = value;
		} else {
			value.time -= instanteInicial;
			ultimo.next = value;
			ultimo = value;
		}
		length+=1;
		notifyAll();
	}

	public void print() {
		Nodo aux = primero;
		while (aux != null) {
			aux.print();
			aux = aux.next;
		}

	}

	public void bubbleSort() {
		System.out.println("Ordenando si es posible");
		if (length > 1) {
	        boolean cambio;
	        do {
	            Nodo actual = primero;
	            Nodo anterior = null;
	            Nodo siguiente = primero.next;
	            cambio = false;
	            while ( siguiente != null ) {
	                if (actual.getTime() > siguiente.getTime()) {
	                    cambio = true;
	                    System.out.println("Se ha cambiado 1 nodo!");
	                    if ( anterior != null ) {
	                    	Nodo sig = siguiente.next;
	                        anterior.next = siguiente;
	                        siguiente.next = actual;
	                        actual.next = sig;
	                    } else {
	                    	Nodo sig = siguiente.next;
	                        primero = siguiente;
	                        siguiente.next = actual;
	                        actual.next = sig;
	                    }
	                    anterior = siguiente;
	                    siguiente = actual.next;
	                } else { 
	                    anterior = actual;
	                    actual = siguiente;
	                    siguiente = siguiente.next;
	                }
	            } 
	        } while( cambio );
	    }
		
	}
}
