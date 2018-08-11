import java.util.*;

public class OutputRegister // classe que armazena o nome do atributo de saida e uma lista de destinos para onde este deve ser enviado
{
	public String uid;
	public String name;
	public ArrayList DestinationList;
	
	public OutputRegister(String uid) // construtor
	{
		this.uid = uid;
		DestinationList = new ArrayList();
	}
	
	public void AddDestination(Destination Temp) // adiciona um atributo destino a lista
	{ 
		DestinationList.add(Temp);
	}
}