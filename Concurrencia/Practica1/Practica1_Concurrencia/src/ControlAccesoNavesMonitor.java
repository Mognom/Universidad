import es.upm.babel.cclib.Monitor;
import es.upm.babel.cclib.Monitor.Cond;


public class ControlAccesoNavesMonitor implements ControlAccesoNaves {
	private Monitor mutex;
	
	private Cond [] condicionEntrar;
	private Cond [] condicionSalir;
	private int[] controlPeso;
	private boolean[] controlPasillo;
	public ControlAccesoNavesMonitor(){		
		mutex= new Monitor();
		condicionEntrar = new Cond [Robots.N_NAVES] ;
		condicionSalir = new Cond [Robots.N_NAVES] ;
		
		
		for ( int i = 0; i< Robots.N_NAVES; i++){
			condicionEntrar[i] = mutex.newCond();
			condicionSalir[i] = mutex.newCond();
		}
		
		controlPeso=new int[Robots.N_NAVES];
		controlPasillo=new boolean[Robots.N_NAVES];
	}
	public void solicitarEntrar(int n, int p) {
		mutex.enter();
		while(controlPeso[n]+p>Robots.MAX_PESO_EN_NAVE)
			condicionEntrar[n].await();
		
		controlPeso[n]=controlPeso[n]+p;
		
		controlPasillo[n]=false;
		if (n > 0)
			
			if (condicionSalir[n-1].waiting() != 0)
				condicionSalir[n-1].signal();
		
		mutex.leave();
	}
	public void solicitarSalir(int n, int p) {
		mutex.enter();
		while(!(n==Robots.N_NAVES-1 || !controlPasillo[n+1]))
			condicionSalir[n].await();
		
		controlPeso[n]=controlPeso[n]-p;
		if(n!=Robots.N_NAVES-1)
			controlPasillo[n+1]= true;
		//Desbloquear otros
		if (condicionEntrar[n].waiting() != 0)
			condicionEntrar[n].signal();
		
		mutex.leave();
	}
	
}
