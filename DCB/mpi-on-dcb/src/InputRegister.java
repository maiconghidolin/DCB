public class InputRegister // classe que armazena o nome do atributo e o seu tipo (string, integer, float, etc)
{
	public String uid;
	public String name;
	public String type;
	
	public InputRegister(String uid, String name, String type) // construtor
	{
		this.uid = uid;
		this.name = name;
		this.type = type;
	}
}