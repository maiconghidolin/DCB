import java.net.*;
public class ThreadIndex
{
	public String federation_id; 
	public String federate_id; 
	public int thread_index;
	
	////////////////////////////////////////////////////////////////////////////
	// Essa classe armazena as informações do federado
	// e um indice para a thread onde ele esta executando
	////////////////////////////////////////////////////////////////////////////
	
	public ThreadIndex (String federation_id, String federate_id, int thread_index)
	{
		this.federation_id = federation_id;
		this.federate_id = federate_id;
		this.thread_index = thread_index;
	}
	
	public int getThreadIndex()
	{ return thread_index; }
}	