
import java.io.*;
import java.util.*;
import java.io.File;

public class EF {

    private int actualLVT;
    private ApplicationDCB App;
    public ArrayList InputRegisterList = new ArrayList();    // Lista de objetos contendo as informaï¿½ï¿½es sobre os atributos locais do federado (nome e tipo)
    public ArrayList InputAttributeQueue = new ArrayList();
    public int contElemIAQ = 0;
    public int lessTimestamp = 0;
    public String input;
    public PriorityQueue<Message> BufferReceivedMessages = new PriorityQueue<Message>(1, new Message());
    
    //variáveis criadas por Guilherme Bizzani
    public int menorPredic = Integer.MAX_VALUE;
    public Map<String, Vector<Integer>> hashQt = new HashMap<String, Vector<Integer>>();
    
    public int totalMensagensRecebidas;

    /////////////////////////////////////////////////////////////////////////////////////
    public EF(ApplicationDCB A) throws IOException // construtor
    {
        App = A;
        actualLVT = 0;
        totalMensagensRecebidas = 0;
        System.out.println("EF Inicializado...");
        
    }

	public double quantidadeEventosPrevistos(){
		int logSize;
		int estimatedLVT;
		int dBetweenProcess;
		int dBetweenEvents;
		double meanTimeBetweenEvents;
		double sumEvents = 0;

		//para todo processo no vetor de dependencia
		for ( String processo : hashQt.keySet()){			
			Vector<Integer> log = hashQt.get(processo);

			logSize = log.size();
			dBetweenProcess = actualLVT - log.get(logSize - 1);    
            
			//se existirem mais de uma mensagem no log e  o processo tem um LVT estimado menor que o tempo de criação do checkpoint
			if( ( log.size() > 1) && ( dBetweenProcess > 0) ){
					/*calculo das métricas*/
					dBetweenEvents = log.get(logSize - 1) - log.get(0);
					meanTimeBetweenEvents = dBetweenEvents/(logSize + 1);
					sumEvents += (dBetweenProcess / meanTimeBetweenEvents) + 1;	
			}
		}

		//return (sumEvents>0)? sumEvents : 1;
		return sumEvents;
	}

    ////////////////////////////////////////////////////////////////////////////////////
    //   	SYNCHRONIZATION ( LVT / GVT )
    /////////////////////////////////////////////////////////////////////////////////////
    public String getLVT() // Retorna o LVT (Local Virtual Time)
    {
        return String.valueOf(actualLVT);
    }

    /////////////////////////////////////////////////////////////////////////////////////
    public synchronized String updateLVT(String newLVT) // Recebe o LVT do federado e atualiza LVT local
    {

        int lookahead = Integer.parseInt(newLVT); ///essa variável não deve ser atualizada aqui. Fiz só pra testar

        if (App.FederateType.compareTo("asynchronous") == 0) {
            actualLVT = Integer.parseInt(newLVT);
        	System.out.println("LVT " + newLVT );
        } else {
            if (Integer.parseInt(newLVT) <= lookahead) {
                actualLVT = Integer.parseInt(newLVT);
            } else {
                // aqui é quando o novo LVT seria maior que o lookahead... 
                //se não me engano significa que o processo deve ser bloqueado e esperar...Não lembro
            }
        }
        //Quando um síncrono atualiza o LVT, ele envia um conjunto de mensagens nulas...
        //***enviar mensagens nulas***/ ???

        if (lessTimestamp <= actualLVT &&
            //	actualLVT > oldLVT &&
            contElemIAQ > 0) {
            seekInputAttributeQueue();
        }
        
    
        //"chama" o redirect 3, que "força" o CHAT a criar um checkpoint.
        //LVT chegou a predicao que foram feitas ao receber mensagens dos outros federados.
        if(actualLVT >= menorPredic-100){
        	
        	System.out.println("Criando checkpoint com a menor predicao no lvt:" + actualLVT);
        	App.NewGateway.Redirect(3);
        	
        	menorPredic = Integer.MAX_VALUE;
        	
        }
        
        //codigo para "finalizar" a execução dos federados que sofreram rollbacks.
        
        if(App.FederateType.compareTo("synchronous") != 0){
        	if(actualLVT > GatewayMPI.App.NewDCB.simulationTime ){
	        	System.out.println("Simulação chegou no fim, finalizando processo...."); 	
	        	System.out.println("GVT atual: "+App.NewDCB.getGVT());   
	        	System.out.println("Total de anti-mensagens: "+App.NewEDCB.totalAntiMsg);        	
	        	return String.valueOf(App.NewDCB.simulationTime + 1);
        	}
        }
        //fim
        
        return String.valueOf(actualLVT);
    }

    /////////////////////////////////////////////////////////////////////////////////////
    //     END OF SYNCHRONIZATION
    /////////////////////////////////////////////////////////////////////////////////////
    public void Decode(Message Msg) throws IOException // Decodifica a mensagem
    {
  	
        this.StoreReceivedMessages(Msg);

        InputAttribute AttributeTemp = null;
        String Source = Msg.FederationSource + Msg.FederateSource;
        AttributeTemp = new InputAttribute(Msg.AttributeID,
            Msg.Value,
            Source,
            Msg.LVT, // Fiz alteraï¿½ï¿½o aqui (o LVT ï¿½ o timestamp da msg)
            getAttributeType(Msg.AttributeID));
        
        if (Msg.AttributeID.compareTo("444.3") == 0) { //aqui onde recebe uma mensagem que causa rollback
            InputAttributeQueue.add(0, AttributeTemp);
            //System.out.println("adicionou na primeira posicao1");
            contElemIAQ++;
            seekInputAttributeQueue();
            int i = 0;
            
            //this.StoreReceivedMessages(Msg);

        } else if (Msg.AttributeID.compareTo("444.1") != 0) { //aqui é onde recebe a mensagem de outro chat
        	
        	if (Msg.AttributeID.compareTo("4.0") != 0 && Msg.AttributeID.compareTo("5.0") != 0) {
        	
        	
        	    //código para modificar o UID da mensagem, pois seu timestamp é menor que o LVT atual.
        	    int lvtMsg = Integer.parseInt(Msg.LVT);
        	    //int lvtChat = Integer.parseInt(actualLVT);
        	    if(lvtMsg < actualLVT && Msg.Value.compareTo("") != 0){
        		       		
            		AttributeTemp.uid = "444.3";      		
                    InputAttributeQueue.add(0, AttributeTemp);
                    contElemIAQ++;
                    seekInputAttributeQueue();
                    
                    //updateLVT(Msg.LVT);
                    return;
        	    }	
        	    //código para criação e manutenção do HashMap, que mantém o timestamp das ultimas 5 mensagens recebidas de cada outro chat
                /*LOG DE MENSAGENS*/

                if(App.FederateType.compareTo("synchronous") != 0){
            	
                	if(Msg.Value.compareTo("") != 0){ //verifica se a mensagem não está vazia
                		//atualiza ou cria o hashMap do Source atual
    	            	if(hashQt.containsKey(Source)){ //caso já exista o source no hashQt
    		            	Vector<Integer> qt = new Vector<Integer>();
    		            	qt = hashQt.get(Source);
    		            	int tam = qt.size();


    		            	if(tam < 5){ //caso não tenha 5 mensagens recebidas ainda, simplesmente adiciona a nova.
    		            		qt.add(Integer.parseInt(Msg.LVT));

    		            	}else{//já possui 5 mensagens, então tem que excluir a mais antiga.
    							qt.removeElementAt(0);
    							qt.add(Integer.parseInt(Msg.LVT));
    		        	    }
    		                hashQt.put(Source, qt);
    		            }else{//criando um source novo.
    		            	Vector<Integer> aux = new Vector<Integer>();
    		            	aux.add(Integer.parseInt(Msg.LVT));
    		            	hashQt.put(Source, aux);
    		            }
    	            	int dif, aux;
    	            	if(hashQt.get(Source).size() > 1){
    	            		dif = hashQt.get(Source).get(hashQt.get(Source).size()-1) - hashQt.get(Source).get(0);
    	            		aux = dif / (hashQt.get(Source).size());
    	            	}else{
    	            		aux = Integer.parseInt(Msg.LVT);
    	            	}
    	                aux += Integer.parseInt(Msg.LVT);
    	                System.out.println("Mensagem recebida do federado " + Source + " previsão: " + aux + " e tamanho do vector = " + hashQt.get(Source).size());
    	                if(aux < menorPredic){
    	                	menorPredic = aux;
    	                }
    	            }
                }
        	   
                System.out.println("MenorPredic " + menorPredic);
            }
        	//codigo original
        	
            InputAttributeQueue.add(0, AttributeTemp);
            contElemIAQ++; // controla o numero de elementos na lista de entrada
            seekInputAttributeQueue();

        } else {
            // Se AttributeID == "444.1", atualiza GVT do federado local - proveniente de broadcast de atualizacao
            // e o valor de Msg.Value carrega o novo GVT

            App.NewDCB.updateGVT(Msg.Value);
            if (App.FederateType.compareTo("notime") == 0) {
                if (contElemIAQ > 0) {
                    seekInputAttributeQueue(); // Verifica se hï¿½ msg's a serem enviadas antes de atualizar o GVT
                }
                updateLVT(Msg.Value);
            }
        // Avanï¿½a o LVT de federados 'notime'. Como antes desta operaï¿½ï¿½o ï¿½
        // disparado um seek na fila de entrada, nï¿½o existe o risco do LVT
        // ultrapassar o timestamp de mensagens ainda nï¿½o retiradas da lista
        // de entrada e enviadas ao federado.

        }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    public void StoreReceivedMessages(Message ReceivedMessage) {
    	//System.out.println("FederateSource da mensagem atual:"+ReceivedMessage.FederateSource);
        
    	if ((ReceivedMessage.FederateSource.compareTo("444") != 0) && (ReceivedMessage.Value.compareTo("") != 0)) {
            BufferReceivedMessages.add(ReceivedMessage);
        }
        totalMensagensRecebidas++;
    }
    /////////////////////////////////////////////////////////////////////////////////////

    public synchronized void seekInputAttributeQueue() {
        InputAttribute Temp = null;
        InputAttribute TempNext = null;

        // Encontra Msg's com menor LVT na lista e encaminha para execuï¿½ï¿½o pelo federado.
        // executa a busca na lista mais de uma vez, pois a lista pode conter
        // mais de uma mensagem com timestamp <= GVT
        while (InputAttributeQueue.size() > 0)
        {
            Temp = (InputAttribute) InputAttributeQueue.get(0);
            for (int x = 1; x < InputAttributeQueue.size(); x++) {
                TempNext = (InputAttribute) InputAttributeQueue.get(x);
                if (Integer.parseInt(TempNext.LVT) < Integer.parseInt(Temp.LVT)) {
                    Temp = (InputAttribute) InputAttributeQueue.get(x);
                }
            }
            lessTimestamp = Integer.parseInt(Temp.LVT);

            // se Temp.LVT comeï¿½ar com 0 entï¿½o ï¿½ originï¿½rio de notime
            // incluir tratamento
            //System.out.print(Temp.LVT +" ");
            if (Temp.LVT.indexOf('0') == 0) {
                System.out.println("NOTIME!");
                App.NewGateway.Redirect(getProtocolConverterID(Temp.uid));
                if (App.FederateType.compareTo("notime") == 0) {
                    updateLVT(App.NewDCB.getGVT());
                }
            } 
            else if (lessTimestamp <= actualLVT && Temp.LVT.indexOf('0') != 0) {

                // Marcador de tempo de inicio do tempo gasto para atualizaï¿½ï¿½o do display
                //	Calendar hoje = Calendar.getInstance();
            	
            	if(Temp.uid.compareTo("444.3") == 0){	//esta situação ocorre quando o DCB "tenta" reproduzir uma mensagem que gerou rollback e ficou salva com
            											//LVT-1, por exemplo: Mensagem gerou rollback no 600, fica salva como: 599.
            											//O dcb chamava os métodos de rollbacks, que por algum motivo não chegavam a completar o rollback
            											//então está sendo ignorado este rollback e removendo a mensagem da fila de atributos.
            		if(actualLVT - Integer.parseInt(Temp.LVT) < 10 ){
                		System.out.println("Nao vai chamar o protocol converter.");
                		System.out.println("LVT Atual: "+actualLVT+"LVT temp: "+Temp.LVT);
                		InputAttributeQueue.remove(Temp);
                		
                		break;
                	}
            	}
                App.NewGateway.Redirect(getProtocolConverterID(Temp.uid));
                if (App.FederateType.compareTo("notime") == 0) {
                    updateLVT(String.valueOf(lessTimestamp));
                }
            } else {
            	System.out.println("entrou no ELSE!");
            	//System.out.println("LVT do Temp: "+Temp.LVT+" Valor do Temp: "+Temp.Value+" e UID: "+Temp.uid);
                //App.NewGateway.Redirect(getProtocolConverterID(Temp.uid));
                break;
            }
        }
    // Ao enviar a mens ao federado, o timestamp (Msg.LVT) da mens pode passar
    // a ser o novo LVT, se assim o federado desejar. O federado tambï¿½m pode
    // simplesmente armazenar o msg numa lista, e executar o evento mais tarde.
    // Se o federado precisar alterar novamente o LVT
    // de acordo com seu tempo interno (se possuir), ele poderï¿½ faze-lo atravï¿½s do mï¿½todo
    // Gateway.updateLVT(...), respeitando o GVT.
    }

    /////////////////////////////////////////////////////////////////////////////////////
    public String getAttributeType(String uid) throws IOException {
        InputRegister InputRegisterTemp = null;

        for (int x = 0; x < InputRegisterList.size(); x++) {
            InputRegisterTemp = (InputRegister) InputRegisterList.get(x);
            if (uid.compareTo(String.valueOf(InputRegisterTemp.uid)) == 0) {
                break;
            } else {
                InputRegisterTemp = null;
            }
        }
        return InputRegisterTemp.type;
    }


    /////////////////////////////////////////////////////////////////////////////////////
    public int getProtocolConverterID(String uid) {
        String pc_id = "";
        char tmp = ' ';

        for (int i = 0; i < uid.length(); i++) {
            tmp = uid.charAt(i);
            if (tmp != '.') {
                pc_id = pc_id + tmp;
            } else {
                break;
            }
        }
        return Integer.parseInt(pc_id);
    }

    /////////////////////////////////////////////////////////////////////////////////////
    public InputAttribute getAttributeReceived(String uid) {
        InputAttribute Temp = null;

        for (int x = 0; x < InputAttributeQueue.size(); x++) {
            Temp = (InputAttribute) InputAttributeQueue.get(x);
            if (uid.compareTo(Temp.uid) == 0 && Integer.parseInt(Temp.LVT) == lessTimestamp) {
                // procura por msg's na lista de espera que tenham timestamp == ao LVT atual
                // se houver, elas sï¿½o enviadas ao federado
                break;
            } else {
                Temp = null;
            }
        }

        return Temp;
    }

    /////////////////////////////////////////////////////////////////////////////////////
    public InputAttribute getAttributeReceived(String uid, String Source, String LVT) {
        InputAttribute Temp = null;

        for (int x = 0; x < InputAttributeQueue.size(); x++) {
            Temp = (InputAttribute) InputAttributeQueue.get(x);
            if (uid.compareTo(Temp.uid) == 0 && Source.compareTo(Temp.Source) == 0 && LVT.compareTo(Temp.LVT) == 0) {
                break;
            } else {
                Temp = null;
            }
        }
        return Temp;
    }

    /////////////////////////////////////////////////////////////////////////////////////
    public void AttributeRemove(InputAttribute AttribRemove) {
        for (int x = 0; x < InputAttributeQueue.size(); x++) {
            if (AttribRemove == (InputAttributeQueue.get(x))) {
                InputAttributeQueue.remove(x);
                contElemIAQ--;  // controla o numero de msg's na lista de entrada
                break;
            }
        }
    }
}
