
import java.util.*;
import java.io.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.w3c.dom.*;

////////////////////////// INICIO CLASSE APPLICATION DCB ////////////////////////////////////
public class DCBMThread {

    public ArrayList ThreadList = new ArrayList();
    public static ApplicationDCB[] DCBThread = new ApplicationDCB[10];        // Array de objetos utilizado para armazenar as threads
    public static int currentThread = 0;
    public String federation_id;
    public String federate_id;              					  // Armazena o n�mero de threads criadas

    /////////////////////////////////////////////////////////////////////////////////////
    public static void main(String args[]) throws IOException // Main
    {
    	
        System.out.println("Inicializado DCB Multithread...");

        new DCBMThread(args);
    }

    /////////////////////////////////////////////////////////////////////////////////////
    // Esse metodo recebe como argumento uma lista de arquivos os quais contem informacoes
    // sobre os federados que ser�o executados em diferentes threads.
    // O ponteiro para cada uma das threads + id_federacao + id_ferderado sao armazenados
    // em uma lista <ThreadList> de objetos <ThreadIndex>
    /////////////////////////////////////////////////////////////////////////////////////
    public DCBMThread(String args[]) throws IOException // construtor
    {
        NodeList FederateInfoNode = null;
        Element Info = null;

        for (int x = 0; x < args.length; x++) {
            DCBThread[currentThread] = new ApplicationDCB(this, currentThread, args[x]); // Cria uma nova thread para tratar a conex�o

            try {
                DocumentBuilderFactory xmldocfac = DocumentBuilderFactory.newInstance();
                DocumentBuilder xmldoc = xmldocfac.newDocumentBuilder();
                Document xml = xmldoc.parse(args[x]);
                Element root = xml.getDocumentElement();

                FederateInfoNode = root.getElementsByTagName("INFO");
                Info = (Element) FederateInfoNode.item(0);

                federation_id = Info.getAttribute("federationid");
                federate_id = Info.getAttribute("federateid");
            } catch (Exception e) {
                System.out.println("Error processing XML file...");
                e.printStackTrace();
            }

            System.out.println("Buscando informacoes do federado " + federation_id + "." + federate_id + " (" + args[x] + ")");

            ThreadIndex T = new ThreadIndex(federation_id, federate_id, currentThread);

            ThreadList.add(T);

            System.out.println("Criando e registrando a thread para o federado " + federation_id + "." + federate_id);

            DCBThread[currentThread].start();

            currentThread++;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////
    //  Metodo utilizado para enviar mensagens localmente entre federados
    //  Esse metodo procura a thread onde o federado est� executando e
    //  envia a mensagem atraves de uma chamada ao metodo LocalReceive
    //////////////////////////////////////////////////////////////////////////////////
    public synchronized void SendMessage(String federation_id, String federate_id, Message Msg) throws IOException {
        DCBThread[getThread(federation_id, federate_id)].LocalReceive(Msg);
    }

    ///////////////////////////////////////////////////////////////////////////////////
    //  Busca o indice para a thread onde o federado <federation_id,federate_id>
    //  est� sendo executado
    ///////////////////////////////////////////////////////////////////////////////////
    public int getThread(String federation_id, String federate_id) // Retorna o objeto Federation que possui o id passado por parametro
    {
        ThreadIndex Temp = null;

        for (int x = 0; x < ThreadList.size(); x++) {
            Temp = (ThreadIndex) ThreadList.get(x);
            if (federation_id.compareTo(Temp.federation_id) == 0 && federate_id.compareTo(Temp.federate_id) == 0) {
                break;
            }
        }
        return Temp.getThreadIndex();
    }
}