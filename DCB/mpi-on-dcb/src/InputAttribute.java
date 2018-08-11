public class InputAttribute
{
	public String uid;
	public String Value;
	public String Source;
	public String LVT;
	public String DestinationType;
	
	public InputAttribute (String uid, String Value, String Source, String LVTSource, String DestinationType)
	{
		this.uid = uid;
		this.Value = Value;
		this.Source = Source;
		this.LVT = LVTSource;
		this.DestinationType = DestinationType;
	}
}