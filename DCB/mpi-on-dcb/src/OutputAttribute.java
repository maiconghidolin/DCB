public class OutputAttribute
{
	public String federationid;
	public String federateid;
	public String attributeID;
	public String Value;
	public String timestamp;

	public OutputAttribute (String federationid, String federateid, String attributeID, String Value, String timestamp)
	{
		this.federationid = federationid;
		this.federateid = federateid;
		this.attributeID = attributeID;
		this.Value = Value;
		this.timestamp = timestamp;
	}
}