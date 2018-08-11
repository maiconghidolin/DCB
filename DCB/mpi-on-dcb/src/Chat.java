
/**
 *
 * @author  FlÃ¡vio Migowski
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.util.HashMap;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Random;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import java.time.Instant;
import java.time.Duration;

public class Chat extends javax.swing.JFrame {

    private int id;
    private String chatLVT = "0";
    private String chatLVTaux;
    private String lastEventTimestamp = "0";
    private int contadorDeMensagens = 0;
    private boolean initAtualizaLVT;
    public static boolean rollback = false;
    private ArrayList<String> checkpointsArray = new ArrayList<String>();
    
    public static int tamanhoArrayCheckpoints = 10;
    public static float percentualChamadaCalculoRecoveryLine = 70;
    public static float percentualChamadaExclusaoRecoveryLine = 90;
    public Hashtable<String, String> checkpointsRecoveryLine = new Hashtable<String, String>();

    private int CkpIndex;
    public int totalTempoRollback;
    public int totalCheckpointsInuteis; 
	public int totalCheckpoints;
    public int totalRollbacks;
    public int mensagensChat5[];
	public int totalTentativaCheckpoints;
	public int eventosDesdeRollback;


	public int totalEventos;
    
    
    /** Inicializa o chat com o id do Federado.
    @param id id do federado*/
    public Chat(int id) {
        this.id = id;
        
        //inicia a variavel CkpIndex com 0
        CkpIndex = 0;
        
        //inicia a variavel de total de tempo de rollback e total de checkpoints inuteis em 0
        totalTempoRollback = 0;
        totalCheckpointsInuteis = 0;
        totalRollbacks = 0;
		totalTentativaCheckpoints = 0;
		totalCheckpoints = 0;
		eventosDesdeRollback = 0;
		totalEventos = 0;
        
        
        //Seta o LookAndFeel para o GTK++
        try {
            UIManager.setLookAndFeel(new com.sun.java.swing.plaf.gtk.GTKLookAndFeel()); //ALTERAÇÃO PARA FUNCIONAR NO WINDOWS.
            //UIManager.setLookAndFeel(new com.sun.java.swing.plaf.windows.WindowsLookAndFeel());
        } catch (Exception ex) {
        }

        this.setTitle("ChatDCB " + new Integer(id).toString());
        switch (id) {
            case 5: {
                chatLVT = GatewayMPI.updateLVT("0");
                AtualizaLVT AtLVT = new AtualizaLVT(this);
                Thread teste = new Thread(AtLVT);
                teste.start();
                break;
            }
        }
        initComponents();

        //Marca o tipo do federado (notime, synchronous ou asynchronous)

        if (GatewayMPI.App.FederateType.compareTo("notime") == 0) {
            untimedButton.setSelected(true);
        } else if (GatewayMPI.App.FederateType.compareTo("synchronous") == 0) {
            synchronousTimeButton.setSelected(true);
        } else if (GatewayMPI.App.FederateType.compareTo("asynchronous") == 0) {
            assynchronousTimeButton.setSelected(true);
            this.setCheckpoint("0");      
        }
        recipientComboBox.removeItemAt(id - 5);  //gambiarra louca. Dps vejo...

		
        //para ativar a criação de checkpoints do Flávio, descomentar o IF a baixo.
        //if(id != 5){
        	CheckpointAnalyzer CpAn = new CheckpointAnalyzer(this);
            Thread cpan = new Thread(CpAn);
            cpan.start();
        //}
	
       	
        //criação e inicio da thread que irá enviar as mensagens automáticas.
        MensagensAutomaticas ChatTest = new MensagensAutomaticas(this);
        Thread chatT = new Thread(ChatTest);
        chatT.start();
        
        
        timeStampText.setText("0");
    }

    /**Edita o texto no quadro de mensagens recebidas.
     * @param value Ã© o valor enviado na mensagem, que Ã© editado e vai para a tela.
     */
    public void setReceivedText(String value) {
        lastEventTimestamp = chatLVT;
        
        
        //Código do método de Índice de Checkpoints
        //atualmente este IF está comparando com o '5', mas pode ser alterado para verificar se o elemento é assincrono
        if(id != 5){
        	
        	String[] splt = value.split("®");
        	
        	if(splt.length == 2){
        		String indexReceived = splt[1];
        		System.out.println("Recebeu o Index: "+indexReceived);
        		System.out.println("Index atual: "+CkpIndex);
        		int indexReceivedINT = Integer.parseInt(indexReceived);
        		if(indexReceivedINT > CkpIndex){
        			System.out.println("Criando checkpoint baseado no Index de mensagem recebida.");
        			Integer aux = Integer.parseInt(chatLVT);
        			aux += 100;
        			this.setCheckpoint(aux.toString());
        			CkpIndex = indexReceivedINT;
        		}
        		
        	}
        }
        //fim
        
        switch (id) {
            case 5: {
                this.receivedText.setText(receivedText.getText() + value + "\n");
                break;
            }
            default: {
                if (!initAtualizaLVT) {
                    initAtualizaLVT = true;
                    chatLVT = GatewayMPI.updateLVT("0");
                    AtualizaLVT AtLVT = new AtualizaLVT(this);
                    Thread teste = new Thread(AtLVT);
                    teste.start();
                    break;
                }

                this.receivedText.setText(receivedText.getText() + value + "\n");
                break;
            }
        }

    }

    /**Retorna o id do federado.
     * @return id
     */
    public int getId() {
        return id;
    }

    public void incrementaContador() {
        contadorDeMensagens++;
        totalEventos++;
    }

    /**Adiciona um checkpoint na lista de checkpoints
     *
     * @param timestamp
     *
     *
     * Modificações por: Ricardo Parizotto
     */

    public void setCheckpoint( String timestamp) {
        Random gerador = new Random();
        double ProbabilidadeRollback = 0;					  
        double quantidadeEventosPrevistos = 0;
		double mediaEventosRollback;

		this.totalTentativaCheckpoints++;
		
		quantidadeEventosPrevistos = GatewayMPI.App.NewEF.quantidadeEventosPrevistos( );
		
        if(totalRollbacks == 0 || totalEventos == 0 ) mediaEventosRollback = 1.;
        else                                          mediaEventosRollback = ((double)totalEventos/(totalRollbacks));

        
		if(quantidadeEventosPrevistos > 0 )           ProbabilidadeRollback = ( quantidadeEventosPrevistos + eventosDesdeRollback)/mediaEventosRollback;
		else                                          ProbabilidadeRollback = 0;
		

        /*
        if(quantidadeEventosPrevistos > 0 )           ProbabilidadeRollback = mediaEventosRollback;
        else                                          ProbabilidadeRollback = 0;
        */

        /*teste*/
		//alpha é distribuído em um intervalo [0.0, 1.0]
        double alpha = gerador.nextDouble();   		


		//Probabilidade deste checkpoint ser útil caso for salvo
        //Cria checkpoint

        if(alpha < (1 - (ProbabilidadeRollback))){      

            System.out.println("checkpoint criado: " + timestamp);            //#DEBUG
            checkpointsArray.add(timestamp);
            CkpIndex++;
			totalCheckpoints++;

            VerificaSituacaoListaCheckpoints();

        }
    }


    /**Pega o checkpoint necessÃ¡rio para poder entregar a mensagem no tempo passado
     *
     * @param timestamp
     * @return checkpoint
     */
    public String getCheckpoint(String timestamp) {
    	System.out.println("Causando rollback por mensagem recebida em: "+timestamp);
        Iterator<String> it = checkpointsArray.iterator();
        ArrayList<String> listaCheckpointsRemocao = new ArrayList<String>();
        String auxiliar, checkpoint = null;
        while (it.hasNext()) {
            auxiliar = it.next();
            if (Integer.valueOf(timestamp) < Integer.valueOf(auxiliar)) { //modificado
                listaCheckpointsRemocao.add(auxiliar);
                totalCheckpointsInuteis ++;
                if(CkpIndex < 1) CkpIndex--;
                //System.out.println("Adicionou na lista de exclusão o checkpoint:"+auxiliar);
            } else {
            	//System.out.println("Atualizando checkpoint para retornar:"+auxiliar);
                checkpoint = auxiliar;
                
            }
        }
        if (checkpoint != null) {
            for (String aux : listaCheckpointsRemocao) {
                checkpointsArray.remove(aux);
            }
        }
        System.out.println("Total de Checkpoints Inuteis: "+totalCheckpointsInuteis);
        return checkpoint;
    }

    /**Retorna o lvt do federado.
     * @return chatLVT
     */
    public String getLVT() {
        return chatLVT;
    }

    /**Retorna o texto na Ã­ntegra das mensagens recebidas
     * @return receivedText.getText()
     */
    public String getReceivedText() {
        return this.receivedText.getText();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        receivedLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        receivedText = new javax.swing.JTextArea();
        messageLabel = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        message = new javax.swing.JTextArea();
        recipientLabel = new javax.swing.JLabel();
        timeStampLabel = new javax.swing.JLabel();
        timeStampText = new javax.swing.JTextField();
        gvtLabel = new javax.swing.JLabel();
        lvtLabel = new javax.swing.JLabel();
        untimedButton = new javax.swing.JRadioButton();
        synchronousTimeButton = new javax.swing.JRadioButton();
        assynchronousTimeButton = new javax.swing.JRadioButton();
        sendButton = new javax.swing.JButton();
        closeButton = new javax.swing.JButton();
        recipientComboBox = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        receivedLabel.setFont(new java.awt.Font("DejaVu Sans", 0, 12));
        receivedLabel.setText("Received");

        receivedText.setColumns(20);
        receivedText.setEditable(false);
        receivedText.setRows(5);
        jScrollPane1.setViewportView(receivedText);

        messageLabel.setFont(new java.awt.Font("DejaVu Sans", 0, 12));
        messageLabel.setText("Message");

        message.setColumns(20);
        message.setRows(5);
        jScrollPane2.setViewportView(message);

        recipientLabel.setFont(new java.awt.Font("DejaVu Sans", 0, 12));
        recipientLabel.setText("Recipient");

        timeStampLabel.setFont(new java.awt.Font("DejaVu Sans", 0, 12));
        timeStampLabel.setText("TimeStamp");

        gvtLabel.setText("GVT:");

        lvtLabel.setText("LVT:");

        untimedButton.setText("Untimed");
        untimedButton.setEnabled(false);

        synchronousTimeButton.setText("Synchronous");
        synchronousTimeButton.setEnabled(false);

        assynchronousTimeButton.setText("Asynchronous");
        assynchronousTimeButton.setEnabled(false);

        sendButton.setText("Send");
        sendButton.setPreferredSize(new java.awt.Dimension(60, 30));
        sendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendButtonActionPerformed(evt);
            }
        });

        closeButton.setText("Close");
        closeButton.setPreferredSize(new java.awt.Dimension(60, 30));
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        recipientComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ChatDCB 5 - Synchronous", "ChatDCB 6 - Asynchronous", "ChatDCB 7 - Asynchronous", "ChatDCB 8 - Asynchronous", "ChatDCB 9 - No Time" }));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(timeStampLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(messageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(receivedLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(recipientLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(recipientComboBox, 0, 262, Short.MAX_VALUE)
                    .addComponent(jScrollPane2)
                    .addComponent(jScrollPane1)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(sendButton, javax.swing.GroupLayout.DEFAULT_SIZE, 118, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(closeButton, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE))
                    .addComponent(timeStampText, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 262, Short.MAX_VALUE))
                .addGap(75, 75, 75)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(synchronousTimeButton)
                    .addComponent(gvtLabel)
                    .addComponent(untimedButton)
                    .addComponent(lvtLabel)
                    .addComponent(assynchronousTimeButton))
                .addGap(42, 42, 42))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 129, Short.MAX_VALUE)
                        .addGap(30, 30, 30))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(receivedLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
                    .addComponent(messageLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(recipientLabel)
                    .addComponent(recipientComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(timeStampLabel)
                    .addComponent(timeStampText))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(closeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sendButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(50, 50, 50))
            .addGroup(layout.createSequentialGroup()
                .addGap(86, 86, 86)
                .addComponent(gvtLabel)
                .addGap(18, 18, 18)
                .addComponent(lvtLabel)
                .addGap(18, 18, 18)
                .addComponent(untimedButton)
                .addGap(12, 12, 12)
                .addComponent(synchronousTimeButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(assynchronousTimeButton)
                .addContainerGap(215, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
	    int opcao = JOptionPane.showConfirmDialog(null, "Deseja realmente sair?", "Fechar ChatDCB", JOptionPane.YES_NO_OPTION);
	    if (opcao == 0) {
	        System.exit(0);
	    }
	}//GEN-LAST:event_closeButtonActionPerformed
	
	private void sendButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendButtonActionPerformed
	    try {
	        Integer.valueOf(recipientComboBox.getSelectedIndex() + 4);
	        try {
	            Integer.valueOf(timeStampText.getText());

		        //Aqui não é sempre 1. Rever. Algo não vai funcionar rs - Ricardo
                GatewayMPI.UpdateAttribute("1." + ((String) recipientComboBox.getSelectedItem()).split(" ")[1], message.getText()+"®"+CkpIndex, timeStampText.getText());

	            message.setText("");
	            lastEventTimestamp = chatLVT;
	            this.incrementaContador();
	        } catch (Exception e) {
	            JOptionPane.showMessageDialog(null, "O valor do Timestamp deve ser um numero inteiro.");
	            e.printStackTrace();
	        }
	    } catch (Exception e) {
	        JOptionPane.showMessageDialog(null, "O valor do Recipient deve ser um numero inteiro.");
	    }
	}//GEN-LAST:event_sendButtonActionPerformed
	
	public void setChatLVT(String chatLVT){
	    this.chatLVT = chatLVT;
	    this.chatLVTaux = chatLVT;
	    
	}

    public void rollback(String Momento) {
    	
    	int tempoRollback = 0;

		eventosDesdeRollback = 0;	
    	tempoRollback = Integer.parseInt(chatLVT) - Integer.parseInt(Momento); 	
    	
    	totalTempoRollback += tempoRollback;
    	
    	totalRollbacks++;
    	
    	System.out.println("Rollback "+totalRollbacks+", LVT Atual: "+chatLVT+", LVT do rollback: "+Momento+" Somatorio de rollbacks: "+totalTempoRollback);
    	
        PriorityQueue<Message> Lista = null;
        PriorityQueue<Message> ListaParaRemocao = new PriorityQueue<Message>(1, new Message());

        Lista = GatewayMPI.App.NewEF.BufferReceivedMessages;

        String todasMensagens = "";
        Iterator<Message> it = Lista.iterator();
        if (it.hasNext()) {
            Message s = it.next();
            while (s != null) {
                //System.out.println("s.LVT: "+s.LVT +" Momento: "+ Momento);
                //Verifica quais mensagens devem ser deletadas
            	//System.out.println("LVT da mensagem '"+s.Value+"' sendo testada"+s.LVT);
                if (Integer.valueOf(s.LVT) > Integer.valueOf(Momento)){  // && Integer.valueOf(s.LVT) != Integer.valueOf(Momento)) {
                	
                    todasMensagens += s.Value+"\n"; // comeco a acrescentar o \n para se assemelhar com a mensagem que está na tela.
                    //System.out.println("mensagem a ser apagada devido ao rollback: " + s.Value);
                    ListaParaRemocao.add(s);
                	
                }
                if (it.hasNext()) {
                    s = it.next();
                } else {
                    s = null;
                }
            }
        }

        //RemoÃ§Ã£o das mensagens perdidas
        if (ListaParaRemocao.size() > 0) {
            for (Message m : ListaParaRemocao) {
                GatewayMPI.App.NewEF.BufferReceivedMessages.remove(m);
            }
        }
        

        System.out.println("LVT no Rollback: " + GatewayMPI.updateLVT(Momento));
 
        this.chatLVT = Momento;
        this.chatLVTaux = Momento;
        if (todasMensagens.compareTo("") != 0) {
        	/*
        	 * alterações: Já que as mensagens a serem removidas estão vindo com \n para ficarem igual as que estão na tela,
        	 * a String todasMensagens também contém as mernsagens "de trás pra frente" em ordem de envio.
        	 * pego a última do split, que na verdade é a primeira que deve ser removida da tela
        	 * e reescrevo o "receivedText" da posição 0 até o inicio da mensagem a ser apagada.
        	 */
            //System.out.println("Mensagens para serem retiradas: " + todasMensagens);
            String teste[] = todasMensagens.split("\n");
            String teste2;
            if(teste.length == 1){
            	teste2 = teste[0];
            }else{
            	teste2 = teste[0]; //primeira posicao das mensagens a serem removidas, para apagar a partir deste.
            	//tem que ser [0] para o teste automático, e [1] para testar manualmente
            }
            int posicao = this.getReceivedText().lastIndexOf(teste2);
            if (posicao != -1) {
                this.receivedText.setText(this.getReceivedText().substring(0, posicao));
            } else {
                //this.receivedText.setText(""); //não zera o "setReceivedText pois provavelmente não havia mensagem para ser apagada.
            }
            //System.out.println("Terminou o rollback, chatlvt:"+chatLVT);
        }
    }

    public void VerificaSituacaoListaCheckpoints(){
        
        float percentualChamadaRL = percentualChamadaCalculoRecoveryLine / 100;
        float percentualExclusaoRL = percentualChamadaExclusaoRecoveryLine / 100;
        
        float numeroCheckpointsChamaRecoveryLine = tamanhoArrayCheckpoints * percentualChamadaRL;
        
        // está em estado critico, chama o calculo da recovery
        if(checkpointsArray.size() == (int)numeroCheckpointsChamaRecoveryLine){
            System.out.println("Chamou calculo recovery line");
            GatewayMPI.ProtocolConverter(5);
        }
        
        float numeroCheckpointsExcluiRecoveryLine = tamanhoArrayCheckpoints * percentualExclusaoRL;
        
        // quase enchendo, exclui os checks
        if(checkpointsArray.size() >= (int)numeroCheckpointsExcluiRecoveryLine){
            System.out.println("Excluindo checkpoints fora da recovery line");
            ExcluiCheckpointsForaRecoveryLine();
        }        
    }
    
    public void ExcluiCheckpointsForaRecoveryLine(){
        
        ArrayList<String> listaCheckpointsRemocao = new ArrayList<String>();
        
        for (String aux : checkpointsArray) {
            if(!checkpointsRecoveryLine.contains(aux)){
                listaCheckpointsRemocao.add(aux);
            }
        }
        
        for (String aux : listaCheckpointsRemocao) {
            checkpointsArray.remove(aux);
        }
        
        System.out.println("Checkpoints excluidos de acordo com a recovery line");
        System.out.println("\nInicio do checkpointsArray");
        for (String aux : checkpointsArray) {
            System.out.println(aux);
        }
        System.out.println("Fim do checkpointsArray\n");        
    }

    public void buscarRecoveryLine(String timestampFalha, String source){
    
        System.out.println("Buscando recovery line");
        
        String timestampCheckpoint = getCheckpoint(timestampFalha);
        if ( timestampCheckpoint != null) {
                        
            // busca todas as mensagens alem do checkpoint para mandar a antiMessage
            // pega a menor para cada federado para enviar só uma mensagem, pois já basta
            
            Hashtable<String, Message> mensagensEnviarPorFederado = new Hashtable<String, Message>();
            
            Set setSentMessages = GatewayMPI.App.NewEDCB.BufferSentMessages.keySet();
            Iterator<String> iteratorMensagensEnviadas = setSentMessages.iterator();
            while (iteratorMensagensEnviadas.hasNext()) {
                String timestampMensagem = iteratorMensagensEnviadas.next();
                
                if (Integer.valueOf(timestampMensagem) > Integer.valueOf(timestampCheckpoint)) {
                    Message mensagemOrfa = GatewayMPI.App.NewEDCB.BufferSentMessages.get(timestampMensagem);
                    
                    if (mensagemOrfa != null){
                        
                        Integer aux1 = Integer.valueOf(mensagemOrfa.LVT);
                        if(aux1 > 0){
                            --aux1;
                            mensagemOrfa.LVT = aux1.toString();
                        }
                        
                        String key = mensagemOrfa.FederationDestination + mensagemOrfa.FederateDestination;
                        if(mensagensEnviarPorFederado.containsKey(key)){
                            Message mensagemNoHash = mensagensEnviarPorFederado.get(key);
                            if(Integer.valueOf(mensagemOrfa.LVT) < Integer.valueOf(mensagemNoHash.LVT)){
                                mensagensEnviarPorFederado.put(key, mensagemOrfa);
                            }
                        }else{
                            mensagensEnviarPorFederado.put(key, mensagemOrfa);
                        }       
                    }   
                }   
            }
            
            Iterator<String> iteratorMensagensEnviar = mensagensEnviarPorFederado.keySet().iterator();
            while (iteratorMensagensEnviar.hasNext()) {
                
                Message mensagemEnviar = mensagensEnviarPorFederado.get(iteratorMensagensEnviar.next());
                
                // se a mensagem é maior que o checkpoint armazenado, não envia a msg denovo
                if(checkpointsRecoveryLine.containsKey(source)){
                    String checkpointRecoveryLine = checkpointsRecoveryLine.get(source);
                    if(Integer.valueOf(mensagemEnviar.LVT) < Integer.valueOf(checkpointRecoveryLine)){
                        System.out.println("Enviando anti mensagem de recovery line para " + mensagemEnviar.FederationDestination + " " + mensagemEnviar.FederateDestination + " com source " + source + " ");
                        GatewayMPI.App.NewEDCB.AntiMessageRecoveryLine(mensagemEnviar, source);
                    }
                }else{
                    GatewayMPI.App.NewEDCB.AntiMessageRecoveryLine(mensagemEnviar, source);
                    System.out.println("Enviando anti mensagem de recovery line para " + mensagemEnviar.FederationDestination + " " + mensagemEnviar.FederateDestination + " com source " + source + " ");
                }
                
            }
            
            // adiciona checkpoint na recovery line por componente, pois cada componente pode gerar uma RL diferente
            if(checkpointsRecoveryLine.containsKey(source)){
                String checkpointRecoveryLine = checkpointsRecoveryLine.get(source);
                
                if(Integer.valueOf(timestampCheckpoint) < Integer.valueOf(checkpointRecoveryLine)){
                    checkpointsRecoveryLine.put(source, timestampCheckpoint);
                    System.out.println("Substituido " + source + " no checkpointsRecoveryLine");
                }
            }else{
                checkpointsRecoveryLine.put(source, timestampCheckpoint);
                System.out.println("Adicionado " + source + " no checkpointsRecoveryLine");
            }
            
            // deleta checkpoint da ultima recovery line
            
            int contadorSourceAnterior = Character.getNumericValue(source.charAt(0)) - 1; 
            String sourceAnterior = String.valueOf(contadorSourceAnterior) + source.charAt(1) + source.charAt(2);
            if(checkpointsRecoveryLine.containsKey(sourceAnterior)){
                checkpointsRecoveryLine.remove(sourceAnterior);
                System.out.println("Removido " + sourceAnterior + " do checkpointsRecoveryLine");
            }           
            
            System.out.println("\nInicio do checkpointsRecoveryLine");
            for ( String checkSource : checkpointsRecoveryLine.keySet()){           
                String checkLVT = checkpointsRecoveryLine.get(checkSource);
                System.out.println(checkSource + ":" + checkLVT);
            }
            System.out.println("Fim do checkpointsRecoveryLine\n");
            
        }    
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton assynchronousTimeButton;
    private javax.swing.JButton closeButton;
    private javax.swing.JLabel gvtLabel;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lvtLabel;
    private javax.swing.JTextArea message;
    private javax.swing.JLabel messageLabel;
    private javax.swing.JLabel receivedLabel;
    private javax.swing.JTextArea receivedText;
    private javax.swing.JComboBox recipientComboBox;
    private javax.swing.JLabel recipientLabel;
    private javax.swing.JButton sendButton;
    private javax.swing.JRadioButton synchronousTimeButton;
    private javax.swing.JLabel timeStampLabel;
    private javax.swing.JTextField timeStampText;
    private javax.swing.JRadioButton untimedButton;
    // End of variables declaration//GEN-END:variables

    /**Classe privada AtualizaLVT.
     * Ã‰ uma Thread para atualizar o GVT (advindo do fedgvt) na tela e o LVT do federado
     */
    private class AtualizaLVT implements Runnable {

        private Chat outerClass;
        private Integer aux;
        private Integer auxGVT;
        private Random gerador = new Random();

        public AtualizaLVT(Chat pointer) {
            outerClass = pointer;
        }

        @Override
        public void run() {
            chatLVTaux = chatLVT;
            int num=0;

			Instant start = Instant.now();

            if (outerClass.getId() == 5) {
                //SincronizaÃ§Ã£o
                GatewayMPI.UpdateAttribute("1.6", "", "0");
                GatewayMPI.UpdateAttribute("1.7", "", "0");
                GatewayMPI.UpdateAttribute("1.8", "", "0");
                GatewayMPI.UpdateAttribute("1.9", "", "0");
            }
            while (rollback == false) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {} 				//wtf
			
			    totalEventos++;
				eventosDesdeRollback++;

                auxGVT = Integer.valueOf(GatewayMPI.returnGVT());
                outerClass.gvtLabel.setText("GVT: " + auxGVT);

                outerClass.lvtLabel.setText("LVT: " + chatLVT);
                aux = new Integer(chatLVT);
                //lookahead de 100

                switch(id){//cada chat enviará uma série de mensagens
                    case 5:
                        num = 75;
                        //////////////////////////////////////////////////////////////chat 5         
                        break;
                    case 6:
                        num = 100;
                        //////////////////////////////////////////////////////////////chat 6
                        break;
                    case 7: 
                        num = 150;
                        //////////////////////////////////////////////////////////////chat 7
                        break;
                    case 8:
                        num = 200;
                        break; 
                    case 9:
                        num = 250;
                        ////////////////////////////////////////////////////////////// chat 9
                        break;   
                }

                aux = aux + num;
                chatLVT = GatewayMPI.updateLVT(aux.toString());
                
                if(Integer.valueOf(chatLVT) > GatewayMPI.App.NewDCB.simulationTime ){
				    Instant end = Instant.now();

					try {
						File file = new File("output"+id+".txt");
						if(file.createNewFile()){
			            	FileOutputStream is = new FileOutputStream(file);
							OutputStreamWriter osw = new OutputStreamWriter(is);    
							Writer w = new BufferedWriter(osw);
							w.write("Total de rollbacks realizados: "+totalRollbacks+"\n");
							w.write("Total de tempo em rollback: "+totalTempoRollback+"\n");
							w.write("Total de checkpoints inúteis: "+totalCheckpointsInuteis+"\n");
        	    			w.write("Total de chamadas de checkpoint: "+totalTentativaCheckpoints+"\n");
        	    			w.write("Checkpoint Index: "+CkpIndex+"\n");
                            w.write("Checkpoint criados: " + totalCheckpoints+"\n");
                            w.write("Checkpoints mantidos: " + checkpointsArray.size() + "\n");
                            w.write("Checkpoints recovery line: " + checkpointsRecoveryLine.size() + "\n");
                            w.write("Mensagens recebidas: " + GatewayMPI.App.NewEF.totalMensagensRecebidas + "\n");
                            w.write("Mensagens enviadas: " + GatewayMPI.App.NewEDCB.totalMensagensEnviadas + "\n");
							w.write("Duracao" + Duration.between(start, end));
							w.close();
						}else
							System.out.println("Error while creating File, file already exists in specified path");
					}
					catch(IOException io) {
						io.printStackTrace();
					}

				    System.out.println("Duracao" + Duration.between(start, end));
                	System.out.println("Total de rollbacks realizados: "+totalRollbacks);
        	    	System.out.println("Total de tempo em rollback: "+totalTempoRollback);
        	    	System.out.println("Total de checkpoints inúteis: "+totalCheckpointsInuteis);
        	    	System.out.println("Total de tentativas de checkpoint: "+totalTentativaCheckpoints);
                    System.out.println("Checkpoint Index: " + CkpIndex);
                    System.out.println("Checkpoint criados: " + totalCheckpoints);
                    System.out.println("Checkpoints mantidos: " + checkpointsArray.size());
                    System.out.println("Checkpoints recovery line: " + checkpointsRecoveryLine.size());
                    System.out.println("Mensagens recebidas: " + GatewayMPI.App.NewEF.totalMensagensRecebidas);
                    System.out.println("Mensagens enviadas: " + GatewayMPI.App.NewEDCB.totalMensagensEnviadas);
                    break;
        	    }
            }
    		System.out.println("Finalizando Chat!");
        }
    }

    private class CheckpointAnalyzer implements Runnable {

        private Chat outerClass;
        private Integer tempo,  provavelCheckpoint, ultimoCheckpoint;

        public CheckpointAnalyzer(Chat pointer) {
            outerClass = pointer;
            ultimoCheckpoint = 0;
        }

        public void run() {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
                //Se a quantidade de mensagens trocadas for considerÃ¡vel, maior que 10
                if (outerClass.contadorDeMensagens > 10) {
                	System.out.println("Criando Checkpoint por ter trocado mais que 10 mensagens");
                    outerClass.setCheckpoint(outerClass.chatLVT);
                    outerClass.contadorDeMensagens = 0;
                }
                
                if(Integer.valueOf(outerClass.chatLVT) - ultimoCheckpoint >= 200){
                	if(!(outerClass.checkpointsArray.contains(outerClass.chatLVT))){
                		System.out.println("Criando Checkpoint!");
                		outerClass.setCheckpoint(outerClass.chatLVT);
                		ultimoCheckpoint = Integer.parseInt(outerClass.chatLVT);
                	}
                }
            }
        }
    }
    
    private class MensagensAutomaticas implements Runnable{

        private Chat outerClass;      
        private int cont = 0;
		private int LVT;
        private Random gerador = new Random();
        
        public MensagensAutomaticas(Chat pointer){ // construtor
            outerClass = pointer;
        }

        public void run(){

        	System.out.println("Startou a thread!");
        		
            while( (LVT = Integer.valueOf(outerClass.chatLVT)) < GatewayMPI.App.NewDCB.simulationTime ){

                int num;
			
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                	System.out.println("Exception no sleep!");
                	System.out.println(ex);
                }
			
                switch(id){//cada chat enviará uma série de mensagens
                    case 5:
                    	//////////////////////////////////////////////////////////////chat 5         
                
                        num = gerador.nextInt(100);           
                        if(LVT != 0 && num < 50){
                            GatewayMPI.UpdateAttribute("1.6", "Msg chat 6"+"®"+outerClass.CkpIndex, Integer.toString(LVT));
                            incrementaContador();
                        }

                        num = gerador.nextInt(100);           
                                    	if(LVT != 0 && num < 50){
                            GatewayMPI.UpdateAttribute("1.7", "Msg chat 6"+"®"+outerClass.CkpIndex, Integer.toString(LVT));
							incrementaContador();
						}

                        //num = gerador.nextInt(100);           
                    	//if(LVT != 0 && num < 50){
                        //    GatewayMPI.UpdateAttribute("1.8", "Msg chat 6"+"®"+outerClass.CkpIndex, Integer.toString(LVT));
						//	incrementaContador();
						//}

                        break;
                    case 6:
                    	//////////////////////////////////////////////////////////////chat 6
                        
                        num = gerador.nextInt(100);           
                    	if(LVT != 0 && num < 50){
                            GatewayMPI.UpdateAttribute("1.7", "Msg chat 6"+"®"+outerClass.CkpIndex, Integer.toString(LVT));
							incrementaContador();
						}

                        num = gerador.nextInt(100);           
                    	if(LVT != 0 && num < 50){
                            GatewayMPI.UpdateAttribute("1.8", "Msg chat 6"+"®"+outerClass.CkpIndex, Integer.toString(LVT));
							incrementaContador();
						}

                        //num = gerador.nextInt(100);           
                    	//if(LVT != 0 && num < 50){
                        //    GatewayMPI.UpdateAttribute("1.9", "Msg chat 6"+"®"+outerClass.CkpIndex, Integer.toString(LVT));
						//	incrementaContador();
						//}

                        break;
                    case 7:
                    	//////////////////////////////////////////////////////////////chat 7

                        num = gerador.nextInt(100);
                    	if(LVT != 0 && num < 50){
                            GatewayMPI.UpdateAttribute("1.8", "Msg chat 7 " + " " + outerClass.CkpIndex, Integer.toString(LVT));
							incrementaContador();						
						}

                        break;

                    case 8:
						
                        num = gerador.nextInt(100);
                    	if(LVT != 0 && num < 80){
                            GatewayMPI.UpdateAttribute("1.9", "Msg chat 8 " + " " + outerClass.CkpIndex, Integer.toString(LVT));
							incrementaContador();		
						}

                        break;
                    case 9:
                    	////////////////////////////////////////////////////////////// chat 9

                        break;
                }
            }
        }
    }   
}

