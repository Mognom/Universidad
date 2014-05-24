import es.upm.babel.cclib.Monitor;
import es.upm.babel.cclib.Monitor.Cond;

public class ControlAccesoNavesMonitor implements ControlAccesoNaves {
	private Monitor mutex; //Mutex
	private Cond[] condicionEntrar;//Conditions para la entrada de cada nave
	private Cond[] condicionSalir;//Conditions para la salida de cada nave
	private Cond[] entradaNave0; //Conditions para el caso especial
	//Arrays de pesos para comprobar si se cumple la CPRE y poder desbloquear
	private int[] controlPesoNave;
	private int[] controlPesoPasillo;
	private int[] pesoEntrada0;
	//Pasillo ocupado o libre
	private boolean[] controlPasillo;

	public ControlAccesoNavesMonitor() {
		//Monitor que garantiza el mutex
		mutex = new Monitor();
		//Arrays de conditions para el bloqueo y desbloqueo de los robots que no cumplan la CPRE
		condicionEntrar = new Cond[Robots.N_NAVES - 1]; // N_NAVES -1 ya que la nave 0 es el caso especial
		condicionSalir = new Cond[Robots.N_NAVES - 1];
		entradaNave0 = new Cond[Robots.N_ROBOTS];	//El maximo numero de conditions que necesito usar es N_ROBOTS

		//Inicializamos las conditions de los arrays
		for (int i = 0; i < Robots.N_NAVES - 1; i++) {
			condicionEntrar[i] = mutex.newCond();
			condicionSalir[i] = mutex.newCond();
		}
		//Inicializamos las conditions de los arrays
		for (int i = 0; i < Robots.N_ROBOTS; i++)
			entradaNave0[i] = mutex.newCond();

		//Arrays que controlan el peso en cada zona para poder desbloquear a los robots que pasen a cumplir la CPRE
		pesoEntrada0 = new int[Robots.N_ROBOTS];//Peso de los que esperan entrar en la nave 0
		controlPesoNave = new int[Robots.N_NAVES];//Peso actual en cada nave
		controlPesoPasillo = new int[Robots.N_NAVES - 1]; //Peso actual en cada pasillo
		controlPasillo = new boolean[Robots.N_NAVES]; //Controla si un pasillo esta ocupado o no
	}

	public void solicitarEntrar(int n, int p) {
		//Garantiza MUTEX
		mutex.enter();
		//if !CPRE
		if (controlPesoNave[n] + p > Robots.MAX_PESO_EN_NAVE) {
			//Si no es la nave 0
			if (n > 0) {
				//Asigna el peso del pasillo ocupado para que los demas procesos sepan cuando desbloquearle
				controlPesoPasillo[n - 1] = p;
				//Se bloquea
				condicionEntrar[n - 1].await();
			}
			//Si esta en la nave 0
			else {
				//Se bloquea guardando informacion para su posterior desbloqueo
				for (int i = 0; i < Robots.N_ROBOTS; i++)
					//busca la primera posicion del array libre
					if (entradaNave0[i].waiting() == 0) {
						//le asigna su peso para poder ser desbloqueado y se bloquea
						pesoEntrada0[i] = p;
						entradaNave0[i].await();
						//Sale del bucle
						break;
					}
			}
		}
		
		//POST
		controlPesoNave[n] = controlPesoNave[n] + p;
		
		//Intetar desbloquear otros procesos
		//Si entra en una nave que no es la 0
		if (n > 0) {
			//El pasillo pasa a libre y a pesar 0
			controlPasillo[n] = false;
			controlPesoPasillo[n - 1] = 0;
			//Busca si hay algun proceso esperando para ocupar ese pasillo y le desbloquea
			if (condicionSalir[n - 1].waiting() != 0)
				condicionSalir[n - 1].signal();
		} 
		else {
			// Si ha entrado en la nave 0
			for (int i = 0; i < Robots.N_ROBOTS; i++)
				//Busca el primer robot que este esperando para entrar en la nave 0 que cumpla la CPRE y le desbloquea
				if (entradaNave0[i].waiting() != 0 && pesoEntrada0[i] + controlPesoNave[n] <= Robots.MAX_PESO_EN_NAVE) {
					entradaNave0[i].signal();
					//Sale del bucle ya que ha encontrado lo que buscaba
					break;
				}
		}
		//Termina el mutex
		mutex.leave();
	}

	public void solicitarSalir(int n, int p) {
		//Garantiza MUTEX
		mutex.enter();
		//if !CPRE
		if (!(n == Robots.N_NAVES - 1 || !controlPasillo[n + 1]))
			//si el pasillo estaba lleno espera
			condicionSalir[n].await();

		//POST
		controlPesoNave[n] = controlPesoNave[n] - p;
		
		//Deja el pasillo como lleno para que no se cumpla la CPRE del resto que intenten entrar
		if (n != Robots.N_NAVES - 1)
			controlPasillo[n + 1] = true;

		//Intetar desbloquear otros procesos
		//Si no es la nave 0
		if (n > 0) {
			//Libera al proceso que este esperando por ocupar ese pasillo (si lo hay)
			if (condicionEntrar[n - 1].waiting() != 0)
				condicionEntrar[n - 1].signal();
		}
	
		else{
			// Si ha entrado en la nave 0
			for (int i = 0; i < Robots.N_ROBOTS; i++)
				//Busca el primer robot que este esperando para entrar en la nave 0 que cumpla la CPRE y le desbloquea
				if (entradaNave0[i].waiting() != 0 && pesoEntrada0[i] + controlPesoNave[n] < Robots.MAX_PESO_EN_NAVE) {
					entradaNave0[i].signal();
					break;
				}
		}
		//Termina el mutex
		mutex.leave();
	}
}
