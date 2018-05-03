package followme.logic;

import java.util.concurrent.atomic.AtomicReference;

public class FollowMeParam {
	
	public static final long MASTER_ID_SIM = 0;
	public static final String[] MASTER_MAC = {"b8:27:eb:74:0c:d1", "00:c0:ca:90:32:05"};
	public static final long[] MASTER_ID_REAL = {202481593486545L, 202481593486545L};
	
	public static int posMaster = -1;
	public static boolean realUAVisMaster = false;
	
	public static AtomicReference<RecursoCompartido> recurso = new AtomicReference<RecursoCompartido>(null);
	
	public static FollowMeState[] uavs;
	
	public enum FollowMeState {
		START((short) 1, FollowMeText.START), 
		CONFIGURATION((short) 2, FollowMeText.CONFIGURATION),
		WAIT((short) 3, FollowMeText.WAIT), 
		FOLLOWER((short) 4, FollowMeText.FOLLOWER), 
		LANDING((short) 5, FollowMeText.LANDING), 
		FINISH((short) 6, FollowMeText.FINISH),;

		private final short id;
		private final String name;
		
		private FollowMeState(short id, String name) {
			this.id = id;
			this.name=name;
		}
		
		public short getId() {
			return this.id;
		}
		
		public String getName() {
			return this.name;
		}

		public static FollowMeState getStateById(short id) {
			FollowMeState res = null;
			for (FollowMeState p:FollowMeState.values()) {
				if(p.getId()==id) {
					res=p;
					break;
				}
			}
			return res;
		}
		
	}

}
