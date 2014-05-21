import es.upm.babel.cclib.Monitor;
import es.upm.babel.cclib.Monitor.Cond;


public class ControlAccesoNavesMonitor implements ControlAccesoNaves {
	private Monitor mutex;
	
	private Cond [] condicionEntrar;
	private Cond [] condicionSalir;
	private Cond [] entradaNave0;
	private int[] controlPesoNave;
	private int[] controlPesoPasillo;
	private int[] pesoEntrada0;
	private boolean[] controlPasillo;
	public ControlAccesoNavesMonitor(){		
		mutex= new Monitor();
		condicionEntrar = new Cond [Robots.N_NAVES-1] ;
		condicionSalir = new Cond [Robots.N_NAVES-1] ;
		entradaNave0 = new Cond[Robots.N_ROBOTS];
		
		for ( int i = 0; i< Robots.N_NAVES-1; i++){
			condicionEntrar[i] = mutex.newCond();
			condicionSalir[i] = mutex.newCond();
		}
		
		for (int i = 0; i< Robots.N_ROBOTS; i++)
			entradaNave0[i] = mutex.newCond();
		
		pesoEntrada0 = new int [Robots.N_ROBOTS];
		controlPesoNave=new int[Robots.N_NAVES];
		controlPesoPasillo=new int[Robots.N_NAVES-1];
		controlPasillo=new boolean[Robots.N_NAVES];
	}
	
	
	public void solicitarEntrar(int n, int p) {
		mutex.enter();
		if(controlPesoNave[n]+p>Robots.MAX_PESO_EN_NAVE){
			if (n >0){
				controlPesoPasillo[n-1]=p;
				condicionEntrar[n-1].await();
			}
			else{
				for (int i = 0; i< Robots.N_ROBOTS; i++)
					if (entradaNave0[i].waiting() == 0){
						pesoEntrada0[i] = p;
						entradaNave0[i].await();
						break;
					}
			}
		}
		
		controlPesoNave[n]=controlPesoNave[n]+p;

		if (n > 0){
			controlPasillo[n]=false;
			controlPesoPasillo[n-1] = 0;
			if (condicionSalir[n-1].waiting() != 0)
				condicionSalir[n-1].signal();
		}
		else{
			//Cosas nazis
			for (int i = 0; i < Robots.N_ROBOTS; i++)
				if (entradaNave0[i].waiting() != 0 && pesoEntrada0[i] + controlPesoNave[n] <= Robots.MAX_PESO_EN_NAVE ){
					entradaNave0[i].signal();
					break;
				}
		}
		mutex.leave();
	}
	
	public void solicitarSalir(int n, int p) {
		mutex.enter();
		if(!(n==Robots.N_NAVES-1 || !controlPasillo[n+1]))
			condicionSalir[n].await();
		
		controlPesoNave[n]=controlPesoNave[n]-p;
		
		if(n!=Robots.N_NAVES-1)
			controlPasillo[n+1]= true;
		
		//Desbloquear otros
		if (n > 0){
			if (condicionEntrar[n-1].waiting() != 0)
				condicionEntrar[n-1].signal();
		}
		else
			for (int i = 0; i < Robots.N_ROBOTS; i++)
				if (entradaNave0[i].waiting() != 0 && pesoEntrada0[i] + controlPesoNave[n] < Robots.MAX_PESO_EN_NAVE ){
					entradaNave0[i].signal();
					break;
				}
		mutex.leave();
	}
	
}
