
/**
 *
 * @author Flávio Migowski
 * Modificado por: Ricardo Parizotto
 */
public class GatewayMPI {

    public static ApplicationDCB App;
    public static Chat Fed;

    private static int contadorSourceRecoveryLine = 1;

    //Construtor vazio
    public GatewayMPI() {
    }

    public static void SetPointer(ApplicationDCB pointer) {
        App = pointer;

        //System.out.println("Starting MPI window");
        Fed = new Chat(Integer.valueOf(App.UniqueFederateID));
        //Fed.setVisible(true);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Métodos de tratamento do tempo para modo conservador (síncrono)
    ///////////////////////////////////////////////////////////////////////////
    public synchronized static String updateLVT(String federateLVT) // Recebe o LVT do federado e solicita atualização ao EF.
    {
        return (App.NewEF.updateLVT(federateLVT));
    }

    public static String returnGVT() // monitora o avanço do LVT do federado para não ultrapassar o GVT
    {
        return (App.NewDCB.getGVT());
    }

    public static void ProtocolConverter(int AttributeID) {
        InputAttribute A0 = null;
        String checkpoint;

        switch (AttributeID) {
            case 2: {
                A0 = App.NewEF.getAttributeReceived("2.0");
                if (A0 != null) {
                    App.NewEF.AttributeRemove(A0);
                    Fed.setReceivedText(A0.Value);
                    Fed.incrementaContador();
                }
                break;
            }
            case 3: {
            	//System.out.println("entrou no case 3!");
            	Fed.setCheckpoint(App.NewEF.getLVT());
            	break;
            }
            case 4: {
                // aqui sinaliza para simular uma falha a partir de uma anti mensagem
                A0 = App.NewEF.getAttributeReceived("4.0");

                if (A0 != null) {
                    System.out.println("Encontrou a msg 4.0");
                    App.NewEF.AttributeRemove(A0);
                    
                    String LVT = A0.LVT;
                    String Source = A0.Value; // estou passando o federado que iniciou a chamada no value
                    
                    System.out.println("Source: " + Source);
                    
                    Fed.buscarRecoveryLine(LVT, Source);
                    
                }else{
                    System.out.println("Nao encontrou a msg 4.0");
                }
                
                break;
            }   
            case 5: {
                // aqui sinaliza para simular uma falha no tempo atual
                A0 = App.NewEF.getAttributeReceived("5.0");

                // se achou uma mensagem é uma solicitação de outro componente
                // senão é uma chamada interna
                if (A0 != null) {
                    System.out.println("Encontrou a msg 5.0");
                    App.NewEF.AttributeRemove(A0);
                }else{
                    System.out.println("Nao encontrou a msg 5.0");
                    System.out.println("Enviando mensagem de inicio da Recovery Line");
                    App.NewEDCB.MensagemInicioRecoveryLine(Integer.valueOf(App.UniqueFederateID));
                }
                
                String LVT = App.NewEF.getLVT();
                String Source = contadorSourceRecoveryLine + App.UniqueFederationID + App.UniqueFederateID;
                contadorSourceRecoveryLine++;
                Fed.buscarRecoveryLine(LVT, Source);
                
                break;
            
            }
            case 444: {
                A0 = App.NewEF.getAttributeReceived("444.3");
                if (A0 != null) {
                    App.NewEF.AttributeRemove(A0);
                    Fed.rollback = true;
                    checkpoint = Fed.getCheckpoint(A0.LVT);
                    
                    System.out.println("Mensagem 444 LVT " + A0.LVT + " Origem " + A0.Source + " checkpoint " + checkpoint);
                    
                    if ( checkpoint != null) {
                        App.NewEDCB.antiMessageTrigger(checkpoint);
                        Fed.rollback(checkpoint);
                        Fed.setChatLVT(updateLVT(checkpoint));
                        Fed.setReceivedText(A0.Value);
                        Fed.rollback = false;
                    }
                }
                break;
            }

        }
    }

    //////////////////////////////// INICIO TRADUTOR ////////////////////////////////////////
    public static long ToLong(String value) {
        return Long.parseLong(value);
    }

    public static double ToDouble(String value) {
        return Double.parseDouble(value);
    }

    public static float ToFloat(String value) {
        return Float.parseFloat(value);
    }

    public static int ToInt(String value) {
        return Integer.parseInt(value);
    }

    public static char ToChar(String value) {
        return value.charAt(0);
    }

    public static char[] ToCharArray(String value) {
        return value.toCharArray();
    }

    public static boolean ToBoolean(String value) {
        boolean bool = false;
        String val = value.toUpperCase();

        if (val.compareTo("TRUE") == 0) {
            bool = true;
        }

        return bool;
    }

    /////////////////////////////////////////////////////////////////////////////////////
    public static void UpdateAttribute(String Name, String Value, String timestamp) {
        App.NewEDCB.Update(Name, Value, timestamp);
    }

    public static void UpdateAttribute(String Name, int Value, String timestamp) {
        App.NewEDCB.Update(Name, String.valueOf(Value), timestamp);
    }

    public void UpdateAttribute(String Name, boolean Value, String timestamp) {
        App.NewEDCB.Update(Name, String.valueOf(Value), timestamp);
    }

    public void UpdateAttribute(String Name, float Value, String timestamp) {
        App.NewEDCB.Update(Name, String.valueOf(Value), timestamp);
    }

    public void UpdateAttribute(String Name, double Value, String timestamp) {
        App.NewEDCB.Update(Name, String.valueOf(Value), timestamp);
    }

    public void UpdateAttribute(String Name, long Value, String timestamp) {
        App.NewEDCB.Update(Name, String.valueOf(Value), timestamp);
    }

    public void UpdateAttribute(String Name, char Value, String timestamp) {
        App.NewEDCB.Update(Name, String.valueOf(Value), timestamp);
    }

    public void UpdateAttribute(String Name, char[] Value, String timestamp) {
        App.NewEDCB.Update(Name, String.valueOf(Value), timestamp);
    }
}
