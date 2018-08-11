
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EDCB {

    private ApplicationDCB App;
    private int ordertimestamp = 0;
    public ArrayList OutputRegisterList = new ArrayList();    // Lista de objetos contendo as informa√ß√µes sobre os atributos de saida
    public ArrayList OutputAttributeQueue = new ArrayList();                // Fila de sa√≠da p/ atributos a serem enviado
    public Hashtable<String, Message> BufferSentMessages = new Hashtable<String, Message>();
    public boolean waiting = false;
    public boolean init = true;
    
    public int totalAntiMsg;
    public int totalMensagensEnviadas;

    /////////////////////////////////////////////////////////////////////////////////////
    public EDCB(ApplicationDCB A) // construtor
    {
        App = A;
        totalAntiMsg = 0;
        totalMensagensEnviadas = 0;
        System.out.println("EDCB Inicializado...");
    }

    /////////////////////////////////////////////////////////////////////////////////////
    public void Update(String Attribute, String Value, String timestamp) // Recebe as atualizaÔøΩÔøΩo do Gateway
    {
        if (App.FederateType.compareTo("synchronous") == 0) {
            UpdateSync(Attribute, Value, timestamp);
        }
        if (App.FederateType.compareTo("asynchronous") == 0) {
            UpdateAsync(Attribute, Value, timestamp);
        }
        if (App.FederateType.compareTo("notime") == 0) {
            UpdateNoTime(Attribute, Value, timestamp);
        }

    // Incluir aqui uma condiÔøΩÔøΩo informando erro de configuraÔøΩÔøΩo do tipo do
    // federado no config.xml se nÔøΩo ocorrer nenhuma das condiÔøΩÔøΩes acima
    }

    public void UpdateSync(String Attribute, String Value, String timestamp) {
        //	if (Value.compareTo("NullMsg") != 0) {  // Por enquanto, a NullMsg nÔøΩo tem uso prÔøΩtico nesta implementaÔøΩÔøΩo
        if (Integer.parseInt(timestamp) >= Integer.parseInt(App.NewDCB.getGVT())) { //sempre entra nesse if, j· que o GVT n„o È mais atualizado. Ou n„o deveria ser, pelo menos :X
            Update1(Attribute, Value, timestamp);
        } else {
            System.out.println("Viol.de LCC iminente, pois timestamp < GVT! Msg n√£o enviada! " + Attribute + " " + timestamp);
            System.out.println("timestamp" + timestamp + "gvt:" + App.NewDCB.getGVT());
        }
    //	} else Update1(Attribute,App.NewEF.getLVT(),timestamp);
    // envia mensagem nula com um valor qualquer de timestamp, e LVT atual...
    }

    public void UpdateAsync(String Attribute, String Value, String timestamp) { //VERIFICAR AQUI O >= getGVT, pode ser aqui o que nunca gera rollback
        if ((Integer.parseInt(timestamp) >= Integer.parseInt(App.NewDCB.getGVT())) && (Attribute.split("\\.")[0].compareTo("444") != 0)) { //
            System.out.println("Attribute: " + Attribute + "Value: " + Value + " Timestamp: " + timestamp);
            Update1(Attribute, Value, timestamp);
        } else {
            //Disparar Anti-mensagem
            System.out.println("Vai disparar AntiMensagem");
            System.out.println("Attribute: " + Attribute + "Value: " + Value + " Timestamp: " + timestamp);
            AntiMessage("444." + Attribute.split("\\.")[1], Value, timestamp);
            System.out.println("Disparou AntiMensagem");
        }
    }

    public void UpdateNoTime(String Attribute, String Value, String timestamp) {
        if (ordertimestamp <= Integer.parseInt(App.NewDCB.getGVT())) {
            ordertimestamp = Integer.parseInt(App.NewDCB.getGVT()) + 1;
        } else {
            ordertimestamp++;  // utiliza 'ordertimestamp' para ordenar a execuÔøΩÔøΩo, no destino, das msg's enviadas
        }
        Update1(Attribute, Value, "0" + String.valueOf(ordertimestamp));

        App.NewEF.updateLVT(App.NewDCB.getGVT());
    }

    public void Update1(String Attribute, String Value, String timestamp) // Recebe as atualizaÔøΩÔøΩo do Gateway
    {
        OutputAttribute OutMessage;
        OutputAttribute NextMessage;
        Destination NewDestination;

        OutputRegister NewAttribute = getOutputRegister(Attribute); // busca a instancia do objeto OutputRegister que possui as informacoes do atributo + lista de destinos

        if (NewAttribute != null) {
            for (int x = 0; x < NewAttribute.DestinationList.size(); x++) // Monta uma mensagem para cada destino diferente
            {
                NewDestination = (Destination) NewAttribute.DestinationList.get(x);

                OutMessage = new OutputAttribute(NewDestination.federationid,
                    NewDestination.federateid,
                    NewDestination.attributeID,
                    Value,
                    timestamp);

                OutputAttributeQueue.add(OutMessage); // Adiciona a mensagem na fila de saÔøΩda

                if (init && !waiting) // Se for a primeira mensagem a ser enviada
                {					   // e nÔøΩo estiver esperando pelo envio de nenhuma
                    init = false;
                    //	waiting = true;
                    try {
                        Code();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    public void AntiMessage(String Attribute, String Value, String timestamp) {
        OutputRegister NewAttribute = getOutputRegister(Attribute);
        Destination NewDestination;
        boolean attributeFound = false;
        String FederateId = null;
        String FederationId = null;
        if(NewAttribute == null) return;
        //System.out.println("New Atribute: "+NewAttribute+" Size: "+NewAttribute.DestinationList.size());
        for (int x = 0; x < NewAttribute.DestinationList.size(); x++) {
            NewDestination = (Destination) NewAttribute.DestinationList.get(x);
            attributeFound = true;
            FederateId = NewDestination.federateid;
            FederationId = NewDestination.federationid;
            Message m = new Message("AntiMessage",
                App.UniqueFederationID,
                App.UniqueFederateID,
                NewDestination.federationid,
                NewDestination.federateid,
                NewDestination.attributeID,
                Value,
                timestamp,
                "none",
                "none");
            try {
                System.out.println(m);
                totalMensagensEnviadas++;
                App.NewDCB.DCBSend(m);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void AntiMessageRecoveryLine(Message antiMensagem, String source) {
       
        Message m = new Message("AntiMessageRL",
               App.UniqueFederationID,
               App.UniqueFederateID,
               antiMensagem.FederationDestination,
               antiMensagem.FederateDestination,
               "4.0",
               source,
               antiMensagem.LVT,
               "none",
               "none");
        
        try {
            //System.out.println("Mensagem recovery line:");
            //System.out.println(m);
            totalMensagensEnviadas++;
            App.NewDCB.DCBSend(m);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
    }

    public boolean MensagemInicioRecoveryLine(int id) {
       
        // aqui ta fixo pq È oq preciso
        ArrayList mensagensEnviar = new ArrayList();
        for (int x = 5; x <= 9; x++) {
            if(x != id){
                mensagensEnviar.add("1/" + x);
            }   
        }
        
        for (int x = 0; x < mensagensEnviar.size(); x++) {
            String destino = (String) mensagensEnviar.get(x);
            
            String arrayDestino[] = destino.split("/");
            
            Message m = new Message("RL",
               App.UniqueFederationID,
               App.UniqueFederateID,
               arrayDestino[0],
               arrayDestino[1],
               "5.0",
               "",
               "0",
               "none",
               "none");
        
            try {
                //System.out.println("\nMensagem inicio recovery line:");
                //System.out.println(m);
                totalMensagensEnviadas++;
                App.NewDCB.DCBSend(m);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            
        }
        
        return true;
    }
    
    /////////////////////////////////////////////////////////////////////////////////////
    public void SendNextMessage() {
        try {
            new SendNext();

        } catch (IOException e) {
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    public class SendNext extends Thread {

        public SendNext() throws IOException {
            this.start();
        }

        public void run() {
            if (!waiting) {
                try {
                    //	waiting = true;
                    init = false;

                    Code();
                } catch (IOException e) {
                }
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    public void Code() throws IOException // construtor
    {
        String Attribute;
        String Value;
        Message MessageToSend;
        OutputAttribute NewOutput;

        try {
            NewOutput = getFirstRequest();

            if (NewOutput != null) {
            	
            	
            	/*if(NewOutput.Value.compareTo("0") != 0){ //TENTATIVA DE ACOPLAR O CHECKPOINT INDEX APENAS INTERNAMENTE NO DCB.

            		System.out.println("Acoplando Index na mensagem!");
            		System.out.println("Mensagem:"+NewOutput.Value);
            		NewOutput.Value += "Æ"+App.CkpIndex;
            		System.out.println("Mensagem:"+NewOutput.Value);
                		
            	}*/
            	
                MessageToSend = new Message("Update",
                    App.UniqueFederationID,
                    App.UniqueFederateID,
                    NewOutput.federationid,
                    NewOutput.federateid,
                    NewOutput.attributeID,
                    NewOutput.Value,
                    NewOutput.timestamp,
                    "none",
                    "none");
                this.StoreSentMessages(App.NewEF.getLVT(), MessageToSend);
                App.NewDCB.DCBSend(MessageToSend); // Repassa o objeto mensagem para a funÔøΩÔøΩo DCBSend do DCB
            //System.out.println("Mandei mensagem para: "+MessageToSend.FederateDestination
            //+" com atributo "+MessageToSend.AttributeID+ " e timestamp "+MessageToSend.LVT
            //+ " e conteudo "+MessageToSend.Value);
            // Store("OUTPUT",NewOutput.attributeID, NewOutput.Value, App.NewEF.getLVT());

            } else // fila <OutputAttributeQueue> estÔøΩ vazia
            {
                //	waiting = false;
                init = true;
            }

        } catch (IOException e) {
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////
    public void StoreSentMessages(String LVT, Message MessageSent) {
        if ((MessageSent.FederateDestination.compareTo("444") !=0) && (MessageSent.Value.compareTo("") != 0)
            && (MessageSent.AttributeID.compareTo("4.0")!=0 && (MessageSent.AttributeID.compareTo("5.0")!=0))) {
            BufferSentMessages.put(LVT, MessageSent);
        }
        totalMensagensEnviadas++;
    }
    ///////////////////////////////////////////////////////////////////////////////////

    public void Store(String StoreType, String Attribute, String Value, String LVT) // Salva todos os atributos recebidos / enviados em um hist√≥rico
    {
        FileOutputStream FileOutput;
        PrintStream File;
        String OutputString = "LVT=" + LVT + "%TYPE=" + StoreType + "%ATTRIBUTE=" + Attribute + "%VALUE=" + Value;

        try {
            FileOutput = new FileOutputStream("history.dcb", true);
            File = new PrintStream(FileOutput);
            File.println(OutputString);
            File.close();
            FileOutput.close();
        } catch (IOException e) {
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    public OutputRegister getOutputRegister(String uid) // Retorna o objeto OutputRegister que possui o name passado por parametro
    {
        OutputRegister Temp = null;

        for (int x = 0; x < OutputRegisterList.size(); x++) {
            Temp = (OutputRegister) OutputRegisterList.get(x);
            if (uid.compareTo(Temp.uid) == 0) // Compara se as strings sÔøΩo iguais
            {
                break;
            } else {
                Temp = null;
            }
        }
        return Temp;
    }


    /////////////////////////////////////////////////////////////////////////////////////
    public OutputAttribute getFirstRequest() {
        OutputAttribute Temp = null;
        int index = -1;

        if (OutputAttributeQueue.size() > 0) {
            Temp = (OutputAttribute) OutputAttributeQueue.get(0);
            OutputAttributeQueue.remove(0);
        }

        return Temp;
    }

    /////////////////////////////////////////////////////////////////////////////////////
    //Disparador de anti-mensagens
    public void antiMessageTrigger(String checkpoint) {
        Set setDeLVTs = BufferSentMessages.keySet();
        Iterator<String> it = setDeLVTs.iterator();
        if (it.hasNext()) {
        	//System.out.println("eh loop 1");
            String aux = it.next();
            System.out.println("aux: "+aux);
            while (aux != null) {
            	//System.out.println("eh loop infinito aqui2");
                //Verifica quais mensagens que foram enviadas nesse tempo
                //devem ser deletadas (e fazer rollback nos elementos)
                if (Integer.valueOf(aux) > Integer.valueOf(checkpoint)) {
                    Message s = BufferSentMessages.get(aux);
                    //System.out.println("mensagem: " + s.Value + "do elemento 1." + s.FederateDestination + " lvt " + s.LVT);
                    Integer aux1 = Integer.valueOf(s.LVT);
                    //System.out.println("aux1: "+aux1);
                    if (aux1 > 0) {
                        --aux1;
                    }
                    //aux = aux1.toString();
                    //Dispara a anti-mensagem
                    this.AntiMessage("444." + s.FederateDestination, "", aux1.toString());
                    
                    totalAntiMsg++; //incrementa o contador de anti-mensagens para o caso de teste.
                    
                    //aux1++;
                    //aux = aux1.toString();
                    
                    //System.out.println("msg: "+BufferSentMessages.get(aux));
                    
                    BufferSentMessages.remove(aux);
                    
                    /*Message tes = BufferSentMessages.remove(aux);
                    
                    if(tes == null){
                    	System.out.println("nao removeu a msg!"); //estava dando problema aqui, ele nunca removia a mensagem, usei o IF para ter certeza disso.
                    }else{
                    	System.out.println("MDS, removeu a msg!");
                    }*/
                    
                }
                //estava no if acima
                if (it.hasNext()) {
                	
                	try{
                		aux = it.next();
                	}catch(Exception es){
                		return;
                	}
                    
                } else {
                    aux = null;
                }
            }
            System.out.println("Total de anti-mensagens: "+totalAntiMsg);
        }
    }
}
