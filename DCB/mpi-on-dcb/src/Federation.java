
import java.util.*;

public class Federation // classe que armazena os ids das federa��es e armazena a lista de federados dessa federa��o
{

    public String id;
    public ArrayList FederateList;

    public Federation(String id) // construtor
    {
        this.id = id;
        FederateList = new ArrayList();
    }

    public void AddFederate(Federate Temp) // Adiciona um federado a lista da federa��o
    {
        FederateList.add(Temp);
    }

    public Federate getFederate(String pid) // Retorna o federado que possui o id passa por parametro
    {
        Federate Temp = null;
        for (int x = 0; x < FederateList.size(); x++) {
            Temp = (Federate) FederateList.get(x);
            if (pid.compareTo(Temp.id) == 0) {
                break;
            }
        }
        return Temp;
    }

    public void seFederateChannel(String pid, int channel) // Retorna o federado que possui o id passa por parametro
    {
        Federate Temp = null;
        for (int x = 0; x < FederateList.size(); x++) {
            Temp = (Federate) FederateList.get(x);
            if (pid.compareTo(Temp.id) == 0) {
                Temp.channel = channel;
                FederateList.set(x, Temp);
                break;
            }

        }
    }
}