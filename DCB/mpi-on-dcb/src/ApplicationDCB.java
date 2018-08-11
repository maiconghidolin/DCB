
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.w3c.dom.*;


////////////////////////// INICIO CLASSE APPLICATION DCB ////////////////////////////////////
public class ApplicationDCB extends Thread // classe principal --> instancia objetos das outras classes
{							               // todas as classes est�o aninhadas dentro dessa classe principal

    public DCB NewDCB;
    public EDCB NewEDCB;
    public EF NewEF;
    public Gateway NewGateway;
    public String UniqueFederationID; // ID �nico da fedara��o a que o federado pertence
    public String UniqueFederateID;   // ID �nico do federado
    public int LocalPort;             // Porta local na qual o federado recebe mensagens
    public String FederateType;		  // Tipo do federado (sincrono/assincrono)
    public String Config;
    public DCBMThread PointerMT;
    
    
    //public int gatewayVal;  //* gatewayVal agora eh o numero do federado

    /////////////////////////////////////////////////////////////////////////////////////
    // A classe ApplicationDCB tornou-se uma thread ...
    // O construtor da classe recebe tres parametros:
    // 1.Ponteiro p/ a classe DCBMThread
    // 2.Indice da thread (que ser� utilizado para identificar o gateway) ISSO DEVE SER PREVIAMENTE ESPECIFICADO
    // 3.Nome do arquivo onde estao as configuracoes do federado
    /////////////////////////////////////////////////////////////////////////////////////
    public ApplicationDCB(DCBMThread p, int gatewayVal, String cfg) throws IOException // construtor
    {
    	
        PointerMT = p;
        Config = cfg;

        //this.gatewayVal = gatewayVal + 1; // Os indices p/ as threads inicia em zero
        // Somar 1 � necess�rio para que os gateways possam iniciar em 1
        // ex. Gateway1, Gateway2, etc...
        NewDCB = new DCB(this);
        NewEDCB = new EDCB(this);
        NewEF = new EF(this);
        NewGateway = new Gateway(this);
    }

    /////////////////////////////////////////////////////////////////////////////////////
    // O metodo run inicializa o DCB e o Gateway
    /////////////////////////////////////////////////////////////////////////////////////
    public void run() {
        Config(Config);

        try {
            NewDCB.Start();
            NewGateway.Start(Integer.parseInt(UniqueFederateID)); //gatewayVal);
        } catch (IOException e) {
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    // Cria uma thread Send que faz o envio da mensagem para o respectivo federado
    /////////////////////////////////////////////////////////////////////////////////////
    public void LocalSend(String federation_id, String federate_id, Message MessageToSend) throws IOException {
        new Send(federation_id, federate_id, MessageToSend);
    }
    /////////////////////////////////////////////////////////////////////////////////////
    // Cria uma thread Receive que faz o tratamento dos dados recebidos localmente
    /////////////////////////////////////////////////////////////////////////////////////

    public void LocalReceive(Message msg) throws IOException {
        new Receive(msg);
    }

    /////////////////////////////////////////////////////////////////////////////////////
    // Repassa mensagem para a classe DCBMThread que se encarregar� de envia-la para
    // a thread onde o federado destino esta executando
    /////////////////////////////////////////////////////////////////////////////////////
    public class Send extends Thread {

        public Message MessageToSend;
        public String federation_id;
        public String federate_id;

        public Send(String federation_id, String federate_id, Message MessageToSend) throws IOException // construtor
        {
            super("this");
            this.federation_id = federation_id;
            this.federate_id = federate_id;
            this.MessageToSend = MessageToSend;
            start();
        }

        public void run() {
            try {
                PointerMT.SendMessage(federation_id, federate_id, MessageToSend);
            } catch (IOException e) {
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    // Envia a mensagem recebida para o metodo Decode do EF
    /////////////////////////////////////////////////////////////////////////////////////
    public class Receive extends Thread {

        public Message Msg;

        public Receive(Message Msg) throws IOException // construtor
        {
            super("this");
            this.Msg = Msg;
            start();
        }

        public void run() {
            try {
                NewEF.Decode(Msg);
            } catch (IOException e) {
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    // Carrega as configura��es utilizadas pelo DCB e EDCB a partir de um arquivo XML
    // e armazena essas configuracoes em listas de objetos
    // ***  A unica difereca desse metodo em relacao a vers�o que n�o utiliza thread �
    // a passagem do nome do arquivo de configuracao como parametro (antes era estatico >> config.xml)
    /////////////////////////////////////////////////////////////////////////////////////
    public void Config(String file) {
        NodeList FederateInfoNode = null;
        Element Info = null;

        NodeList FederationNode = null;
        Element FederationElement = null;
        NodeList FederateNode = null;
        Element FederateElement = null;

        NodeList DestinationNode = null;
        Element OutputRegisterElement = null;
        NodeList OutputDestinationNode = null;
        Element OutputDestinationElement = null;

        NodeList AttributeNode = null;
        Element AttributeElement = null;

        try {
            ///// INICIO Inicializa��o XML

            DocumentBuilderFactory xmldocfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder xmldoc = xmldocfac.newDocumentBuilder();
            Document xml = xmldoc.parse(file);
            Element root = xml.getDocumentElement();

            ///// FIM Inicializa��o XML


            ///// INICIO leitura tags <INFO>

            FederateInfoNode = root.getElementsByTagName("INFO");
            Info = (Element) FederateInfoNode.item(0);

            UniqueFederationID = Info.getAttribute("federationid");
            UniqueFederateID = Info.getAttribute("federateid");
            LocalPort = Integer.parseInt(Info.getAttribute("localport"));
            FederateType = Info.getAttribute("type");

            ///// INICIO leitura tags <ATTRIBUTE>

            AttributeNode = Info.getElementsByTagName("ATTRIBUTE");

            for (int i = 0; i < AttributeNode.getLength(); i++) {
                AttributeElement = (Element) AttributeNode.item(i);
                NewEF.InputRegisterList.add(new InputRegister(AttributeElement.getAttribute("id"),
                    AttributeElement.getAttribute("name"),
                    AttributeElement.getAttribute("type")));

                DestinationNode = AttributeElement.getElementsByTagName("DESTINATION");

                if (DestinationNode.getLength() > 0) {
                    OutputRegister OutputRegisterTemp = new OutputRegister(AttributeElement.getAttribute("id"));

                    for (int f = 0; f < DestinationNode.getLength(); f++) {
                        OutputDestinationElement = (Element) DestinationNode.item(f);

                        OutputRegisterTemp.AddDestination(new Destination(OutputDestinationElement.getAttribute("federationid"),
                            OutputDestinationElement.getAttribute("federateid"),
                            OutputDestinationElement.getAttribute("attribute")));
                    }

                    NewEDCB.OutputRegisterList.add(OutputRegisterTemp);
                }
            }

            ///// FIM leitura tags <ATTRIBUTE>

            ///// FIM leitura tags <INFO>

            ///// INICIO leitura tags <FEDERATION><FEDERATE>

            FederationNode = root.getElementsByTagName("FEDERATION");
            Federation FederationTemp = null;

            for (int f = 0; f < FederationNode.getLength(); f++) {
                FederationElement = (Element) FederationNode.item(f);
                FederationTemp = new Federation(FederationElement.getAttribute("id"));
                FederateNode = FederationElement.getElementsByTagName("FEDERATE");

                for (int i = 0; i < FederateNode.getLength(); i++) {
                    FederateElement = (Element) FederateNode.item(i);

                    FederationTemp.AddFederate(new Federate(FederateElement.getAttribute("id"),
                        FederateElement.getAttribute("ip"),
                        FederateElement.getAttribute("port")));
                }

                NewDCB.FederationList.add(FederationTemp);
            }

        ///// FIM leitura tags <FEDERATION><FEDERATE>

        } catch (Exception e) {
            System.out.println("Error processing XML file...");
            e.printStackTrace();
        }
    }
}